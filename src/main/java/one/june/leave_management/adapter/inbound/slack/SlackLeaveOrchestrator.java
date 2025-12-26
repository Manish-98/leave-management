package one.june.leave_management.adapter.inbound.slack;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewClosedRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewSubmissionRequest;
import one.june.leave_management.adapter.inbound.slack.mapper.SlackLeaveRequestMapper;
import one.june.leave_management.adapter.inbound.slack.util.SlackMessageTemplate;
import one.june.leave_management.adapter.inbound.slack.util.SlackMetadataUtil;
import one.june.leave_management.adapter.inbound.slack.util.SlackRequestParser;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.adapter.outbound.slack.builder.SlackBlockBuilder;
import one.june.leave_management.adapter.outbound.slack.builder.SlackModalBuilder;
import one.june.leave_management.adapter.outbound.slack.client.SlackApiClient;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageResponse;
import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.common.mapper.LeaveMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrator for Slack-specific leave request workflows
 * <p>
 * This service coordinates the flow of Slack leave interactions:
 * - Receives leave requests from Slack controllers
 * - Processes them asynchronously in a new transaction
 * - Posts success/failure messages back to the Slack thread
 * <p>
 * This is part of the adapter layer and coordinates between:
 * - Application services (LeaveService)
 * - Slack messaging (SlackMessageTemplate, SlackApiClient)
 * <p>
 * The @Async annotation ensures the method runs in a separate thread,
 * allowing controllers to return immediately to Slack.
 * The @Transactional annotation with REQUIRES_NEW creates a new
 * transaction, independent of the caller's transaction context.
 */
@Slf4j
@Service
public class SlackLeaveOrchestrator {

    private final LeaveService leaveService;
    private final LeaveMapper leaveMapper;
    private final SlackApiClient slackApiClient;

    public SlackLeaveOrchestrator(
            LeaveService leaveService,
            LeaveMapper leaveMapper,
            SlackApiClient slackApiClient
    ) {
        this.leaveService = leaveService;
        this.leaveMapper = leaveMapper;
        this.slackApiClient = slackApiClient;
    }

    /**
     * Asynchronously processes a leave request from Slack and posts the result to the thread
     * <p>
     * This method:
     * 1. Converts the request to a command
     * 2. Calls the application service to ingest the leave
     * 3. Posts a success message to the Slack thread if successful
     * 4. Posts a failure message to the Slack thread if an error occurs
     * <p>
     * Runs in a separate thread (@Async) with a new transaction (@Transactional REQUIRES_NEW).
     *
     * @param leaveRequest The leave request from the modal
     * @param channelId    The channel ID where to post updates
     * @param threadTs     The thread timestamp for posting threaded replies
     * @param userId       The Slack user ID for tagging the user
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processLeaveRequestAsync(
            LeaveIngestionRequest leaveRequest,
            String channelId,
            String threadTs,
            String userId
    ) {
        log.info("Processing leave request for user: {}, channel: {}, thread_ts: {}",
                userId, channelId, threadTs);

        try {
            // Convert to command
            LeaveIngestionCommand command = leaveMapper.toCommand(
                    leaveRequest,
                    leaveRequest.getSourceType(),
                    leaveRequest.getSourceId()
            );

            // Ingest the leave
            LeaveDto result = leaveService.ingest(command);
            log.info("Successfully created leave with ID: {}", result.getId());

            // Build success message
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated(
                    channelId, threadTs, userId, result
            );

            // Send success message to thread
            slackApiClient.postThreadReply(channelId, threadTs, message);
            log.info("Successfully posted success message to thread");

        } catch (Exception e) {
            log.error("Failed to process leave request for user: {}", userId, e);

            // Build failure message
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestFailed(
                    channelId, threadTs, userId, e.getMessage()
            );

            // Send failure message to thread
            try {
                slackApiClient.postThreadReply(channelId, threadTs, message);
                log.info("Successfully posted failure message to thread");
            } catch (Exception messagingException) {
                log.error("Failed to post failure message to thread", messagingException);
            }
        }
    }

    /**
     * Creates and posts the initial thread anchor message
     * <p>
     * This message serves as the anchor for all subsequent updates about the leave request.
     *
     * @param channelId The channel ID where the message will be posted
     * @param userTag   The Slack user tag (e.g., "&lt;@U12345&gt;")
     * @return The response containing the message timestamp (thread_ts), or null if posting fails
     */
    public SlackMessageResponse postThreadAnchorMessage(String channelId, String userTag) {
        try {
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated(
                    channelId, userTag
            );

            return slackApiClient.postMessage(channelId, message);

        } catch (Exception e) {
            log.error("Failed to post thread anchor message", e);
            return null;
        }
    }

    /**
     * Posts a cancellation message when a user closes the modal without submitting
     * <p>
     * This informs the thread that the leave request was cancelled.
     *
     * @param channelId The channel ID where the thread exists
     * @param threadTs  The thread timestamp of the parent message
     * @param userId    The Slack user ID for tagging
     */
    public void postCancellationMessage(String channelId, String threadTs, String userId) {
        try {
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestCancelled(
                    channelId, threadTs, userId
            );

            slackApiClient.postThreadReply(channelId, threadTs, message);
            log.info("Successfully posted cancellation message to thread");

        } catch (Exception e) {
            log.error("Failed to post cancellation message to thread", e);
        }
    }

    /**
     * Handles view_submission events from Slack interactions endpoint
     * <p>
     * This method:
     * 1. Parses the form-encoded payload to extract the view submission request
     * 2. Extracts thread context from private_metadata
     * 3. Maps the Slack request to a LeaveIngestionRequest
     * 4. Triggers async leave processing with thread context
     * <p>
     * This method is called by the controller after signature verification.
     * It coordinates the parsing, mapping, and workflow initiation.
     * <p>
     * All exceptions are handled by the global exception handler.
     *
     * @param requestBody The raw form-encoded request body from Slack
     */
    public void handleViewSubmission(String requestBody) {
        log.info("Handling view_submission event");

        // Parse the view submission request from form-encoded payload
        SlackViewSubmissionRequest submissionRequest = SlackRequestParser.parsePayload(
                requestBody,
                SlackViewSubmissionRequest.class
        );

        log.info("View submission from user: {}, view ID: {}",
                submissionRequest.getUser().getId(),
                submissionRequest.getView().getId());

        // Extract thread info from private_metadata
        String metadataJson = submissionRequest.getView().getPrivateMetadata();
        String userId = SlackMetadataUtil.extractUserId(metadataJson);
        String channelId = SlackMetadataUtil.extractChannelId(metadataJson);
        String threadTs = SlackMetadataUtil.extractThreadTs(metadataJson);

        log.info("Extracted thread context - userId: {}, channelId: {}, threadTs: {}",
                userId, channelId, threadTs);

        // Map to LeaveIngestionRequest
        LeaveIngestionRequest leaveRequest = SlackLeaveRequestMapper.toLeaveIngestionRequest(submissionRequest);
        log.info("Mapped to leave request: {}", leaveRequest);

        // Trigger async leave processing with thread context
        processLeaveRequestAsync(
                leaveRequest,
                channelId,
                threadTs,
                userId
        );

        log.info("Triggered async leave processing");
    }

    /**
     * Handles view_closed events from Slack interactions endpoint
     * <p>
     * This method:
     * 1. Parses the form-encoded payload to extract the view closed request
     * 2. Extracts thread context from private_metadata
     * 3. Posts a cancellation message to the thread
     * <p>
     * This method is called by the controller after signature verification
     * when a user closes the modal without submitting.
     * <p>
     * All exceptions are handled by the global exception handler.
     *
     * @param requestBody The raw form-encoded request body from Slack
     */
    public void handleViewClosed(String requestBody) {
        log.info("Handling view_closed event");

        // Parse the view closed request from form-encoded payload
        SlackViewClosedRequest closedRequest = SlackRequestParser.parsePayload(
                requestBody,
                SlackViewClosedRequest.class
        );

        log.info("View closed by user: {}, view ID: {}",
                closedRequest.getUser().getId(),
                closedRequest.getView().getId());

        // Extract thread info from private_metadata
        String metadataJson = closedRequest.getView().getPrivateMetadata();
        String userId = SlackMetadataUtil.extractUserId(metadataJson);
        String channelId = SlackMetadataUtil.extractChannelId(metadataJson);
        String threadTs = SlackMetadataUtil.extractThreadTs(metadataJson);

        log.info("Extracted thread context - userId: {}, channelId: {}, threadTs: {}",
                userId, channelId, threadTs);

        // Post cancellation message to thread (best-effort)
        postCancellationMessage(channelId, threadTs, userId);
    }

    /**
     * Handles slash command events from Slack commands endpoint
     * <p>
     * This method:
     * 1. Creates a user tag from the user ID
     * 2. Posts a thread anchor message to the channel
     * 3. Extracts the thread timestamp from the response
     * 4. Triggers modal opening asynchronously with thread context
     * <p>
     * This method is called by the controller after signature verification
     * when a user invokes the /leave slash command.
     * <p>
     * All exceptions are handled by the global exception handler.
     *
     * @param commandRequest The parsed slash command request
     */
    public void handleSlashCommand(SlackCommandRequest commandRequest) {
        log.info("Handling slash command: {} from user: {} in channel: {}",
                commandRequest.getCommand(),
                commandRequest.getUserId(),
                commandRequest.getChannelName());

        // Create user tag for mentioning the user
        String userTag = "<@" + commandRequest.getUserId() + ">";

        // Post thread anchor message to channel
        log.info("Posting thread anchor message to channel: {}", commandRequest.getChannelId());
        SlackMessageResponse messageResponse = postThreadAnchorMessage(
                commandRequest.getChannelId(),
                userTag
        );

        // Extract thread timestamp from response
        String threadTs = messageResponse != null ? messageResponse.getTs() : null;
        log.info("Posted thread anchor message with timestamp: {}", threadTs);

        // Trigger modal opening asynchronously with thread context
        log.info("Triggering modal opening asynchronously for trigger_id: {}", commandRequest.getTriggerId());
        openLeaveApplicationModalAsync(commandRequest, threadTs);

        log.info("Successfully initiated slash command workflow");
    }

    /**
     * Opens a leave application modal asynchronously
     * <p>
     * This method builds a modal with form fields for leave application
     * and opens it in Slack using the trigger_id from the slash command.
     * It runs asynchronously to avoid blocking the HTTP response.
     * <p>
     * The async nature is important because:
     * - Slack requires a response within 3 seconds
     * - The modal opening is independent of the ACK response
     * - Errors in modal opening shouldn't affect the command ACK
     *
     * @param slackRequest The parsed Slack command request containing trigger_id
     * @param threadTs     The thread timestamp for posting updates later
     */
    @Async
    public void openLeaveApplicationModalAsync(SlackCommandRequest slackRequest, String threadTs) {
        try {
            log.info("Building and opening leave application modal for user: {}, thread_ts: {}",
                    slackRequest.getUserId(), threadTs);

            SlackModalView modalView = buildLeaveApplicationModal(slackRequest, threadTs);

            slackApiClient.openModal(slackRequest.getTriggerId(), modalView);

            log.info("Leave application modal opened successfully for user: {}", slackRequest.getUserId());

        } catch (Exception e) {
            log.error("Failed to open leave application modal for user: {}",
                    slackRequest.getUserId(), e);
            // In a production system, you might want to:
            // - Send an error message to the user via the response_url
            // - Log to an error tracking system
            // - Send a notification to administrators
        }
    }

    /**
     * Builds a modal view for leave application
     * <p>
     * The modal contains form fields for:
     * - Leave type (Annual Leave / Optional Holiday)
     * - Duration (Full Day / First Half / Second Half)
     * - Start date
     * - End date
     * - Reason (optional)
     * <p>
     * This structure follows Slack's Block Kit format for modals.
     * Reference: <a href="https://api.slack.com/block-kit/building">...</a>
     *
     * @param slackRequest The Slack command request containing user context
     * @param threadTs     The thread timestamp for posting updates later
     * @return A configured SlackModalView for leave application
     */
    private SlackModalView buildLeaveApplicationModal(SlackCommandRequest slackRequest, String threadTs) {
        // Leave Type options (ANNUAL_LEAVE, OPTIONAL_HOLIDAY)
        List<SlackOption> leaveTypeOptions = List.of(
                SlackOption.of("Annual Leave", "ANNUAL_LEAVE"),
                SlackOption.of("Optional Holiday", "OPTIONAL_HOLIDAY")
        );

        // Leave Duration options (FULL_DAY, FIRST_HALF, SECOND_HALF)
        List<SlackOption> leaveDurationOptions = List.of(
                SlackOption.of("Full Day", "FULL_DAY"),
                SlackOption.of("First Half", "FIRST_HALF"),
                SlackOption.of("Second Half", "SECOND_HALF")
        );

        List<Object> blocks = List.of(
                // Leave Type selection (ANNUAL_LEAVE vs OPTIONAL_HOLIDAY)
                SlackBlockBuilder.radioButtonsInput(
                        "leave_type_category_block",
                        "leave_type_category_action",
                        "Leave Type",
                        leaveTypeOptions,
                        "ANNUAL_LEAVE"
                ),

                // Leave Duration selection (FULL_DAY vs HALF_DAY)
                SlackBlockBuilder.radioButtonsInput(
                        "leave_duration_block",
                        "leave_duration_action",
                        "Duration",
                        leaveDurationOptions,
                        "FULL_DAY"
                ),

                // Start date
                SlackBlockBuilder.dateInput(
                        "start_date_block",
                        "start_date_action",
                        "Start Date",
                        "Select a date",
                        false // required
                ),

                // End date
                SlackBlockBuilder.dateInput(
                        "end_date_block",
                        "end_date_action",
                        "End Date",
                        "Select a date",
                        false // required
                ),

                // Reason
                SlackBlockBuilder.plainTextInput(
                        "reason_block",
                        "reason_action",
                        "Reason",
                        "Optional: Provide a reason for your leave",
                        true, // multiline
                        true // optional
                )
        );

        // Create JSON metadata with thread context
        String metadataJson = SlackMetadataUtil.createMetadata(
                slackRequest.getUserId(),
                slackRequest.getChannelId(),
                slackRequest.getChannelName(),
                threadTs
        );

        return SlackModalBuilder.create("Apply for Leave", "leave_application_submit")
                .withBlocks(blocks)
                .withPrivateMetadata(metadataJson)
                .build();
    }
}
