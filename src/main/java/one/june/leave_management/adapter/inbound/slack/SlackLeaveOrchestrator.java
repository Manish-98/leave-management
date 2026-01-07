package one.june.leave_management.adapter.inbound.slack;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackAction;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionRequest;
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
import one.june.leave_management.adapter.outbound.slack.dto.blocks.SlackSectionBlock;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackStaticSelectElement;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.application.leave.service.OptionalHolidayService;
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
    private final OptionalHolidayService optionalHolidayService;
    private final SlackLeaveRequestMapper slackLeaveRequestMapper;

    public SlackLeaveOrchestrator(
            LeaveService leaveService,
            LeaveMapper leaveMapper,
            SlackApiClient slackApiClient,
            OptionalHolidayService optionalHolidayService,
            SlackLeaveRequestMapper slackLeaveRequestMapper
    ) {
        this.leaveService = leaveService;
        this.leaveMapper = leaveMapper;
        this.slackApiClient = slackApiClient;
        this.optionalHolidayService = optionalHolidayService;
        this.slackLeaveRequestMapper = slackLeaveRequestMapper;
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
        LeaveIngestionRequest leaveRequest = slackLeaveRequestMapper.toLeaveIngestionRequest(submissionRequest);
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
     * Handles block_actions events from Slack interactions endpoint
     * <p>
     * This method is called when a user interacts with a block element
     * that has dispatch_action=true (e.g., changing leave type selection).
     * <p>
     * Flow:
     * 1. Parses the block action request from form-encoded payload
     * 2. Extracts the selected leave type from the action
     * 3. Extracts view_id, view.hash, external_id, and private_metadata from the request
     * 4. Reconstructs modal based on selected leave type
     * 5. Updates the modal using Slack views.update API with hash validation
     * <p>
     * This method is called by the controller after signature verification.
     * <p>
     * All exceptions are handled by the global exception handler.
     *
     * @param requestBody The raw form-encoded request body from Slack
     */
    public void handleBlockAction(String requestBody) {
        log.info("Handling block_action event");
        log.info(requestBody);

        // Parse the block action request from form-encoded payload
        SlackBlockActionRequest blockActionRequest = SlackRequestParser.parsePayload(
                requestBody,
                SlackBlockActionRequest.class
        );

        log.info("Block action from user: {}, view ID: {}",
                blockActionRequest.getUser().getId(),
                blockActionRequest.getContainer().getViewId());

        // Extract the action that triggered this event
        // The first action should be the leave type selection
        if (blockActionRequest.getActions() == null || blockActionRequest.getActions().isEmpty()) {
            log.error("No actions found in block action request");
            throw new RuntimeException("No actions found in block action request");
        }

        SlackAction action = blockActionRequest.getActions().get(0);

        // Verify this is the leave type category action
        if (!"leave_type_category_action".equals(action.getActionId())) {
            log.warn("Received block action for unexpected action_id: {}. Expected: leave_type_category_action",
                    action.getActionId());
            // For now, we only handle leave type changes
            return;
        }

        // Extract selected leave type from the selected_option
        String selectedLeaveType = action.getSelectedOption().getValue();
        log.info("Selected leave type: {}", selectedLeaveType);

        // Extract view_id and view.hash for updating
        String viewId = blockActionRequest.getContainer().getViewId();
        String viewHash = blockActionRequest.getView().getHash();
        log.info("View ID: {}, Hash: {}", viewId, viewHash);

        // Extract metadata from the view
        String metadataJson = blockActionRequest.getView().getPrivateMetadata();
        log.info("Metadata from view: {}", metadataJson);

        // Parse metadata to reconstruct context
        String userId = SlackMetadataUtil.extractUserId(metadataJson);
        String channelId = SlackMetadataUtil.extractChannelId(metadataJson);
        String channelName = SlackMetadataUtil.extractChannelName(metadataJson);
        String threadTs = SlackMetadataUtil.extractThreadTs(metadataJson);

        log.info("Extracted thread context - userId: {}, channelId: {}, threadTs: {}",
                userId, channelId, threadTs);

        // Reconstruct a minimal SlackCommandRequest for modal building
        SlackCommandRequest slackRequest = new SlackCommandRequest();
        slackRequest.setUserId(userId);
        slackRequest.setChannelId(channelId);
        slackRequest.setChannelName(channelName);
        // trigger_id is not needed for modal updates

        // Build the appropriate modal based on selected leave type
        SlackModalView updatedModal = switch (selectedLeaveType) {
            case "ANNUAL_LEAVE" -> {
                log.info("Building Annual Leave modal");
                yield buildAnnualLeaveModal(slackRequest, threadTs);
            }
            case "OPTIONAL_HOLIDAY" -> {
                log.info("Building Optional Holiday modal");
                yield buildOptionalHolidayModal(slackRequest, threadTs);
            }
            default -> {
                log.error("Unknown leave type selected: {}", selectedLeaveType);
                throw new RuntimeException("Unknown leave type: " + selectedLeaveType);
            }
        };

        // Update the modal with hash validation
        log.info("Updating modal with view_id: {}, hash: {}", viewId, viewHash);
        slackApiClient.updateModal(viewId, updatedModal, viewHash);
        log.info("Successfully updated modal based on leave type selection");
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
     * It runs asynchronously to avoid blocking the HTTP response../
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

            log.debug("Modal view structure: {}", modalView);

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
     * Builds the leave application modal
     * <p>
     * This modal shows all fields for both leave types.
     * Users select the leave type and fill in the appropriate fields.
     * Backend processes based on the selected leave type.
     * <p>
     * Note: Dynamic modal updates are not supported by Slack's API for modals,
     * so we show all fields and handle the logic during submission processing.
     *
     * @param slackRequest The Slack command request containing user context
     * @param threadTs     The thread timestamp for posting updates later
     * @return A configured SlackModalView for leave application
     */
    private SlackModalView buildLeaveApplicationModal(SlackCommandRequest slackRequest, String threadTs) {
        // Build modal with leave type selection (using static_select with dispatch_action)
        // AND annual leave form (shown by default)

        // Leave Type options
        List<SlackOption> leaveTypeOptions = List.of(
                SlackOption.of("Annual Leave", "ANNUAL_LEAVE"),
                SlackOption.of("Optional Holiday", "OPTIONAL_HOLIDAY")
        );

        // Leave Duration options
        List<SlackOption> leaveDurationOptions = List.of(
                SlackOption.of("Full Day", "FULL_DAY"),
                SlackOption.of("First Half", "FIRST_HALF"),
                SlackOption.of("Second Half", "SECOND_HALF")
        );

        // Create static select element for leave type selection
        // Note: dispatch_action removed as it's not supported for static_select in section block accessories
        SlackStaticSelectElement leaveTypeSelect = SlackStaticSelectElement.builder()
                .actionId("leave_type_category_action")
                .options(leaveTypeOptions)
                .initialOption(leaveTypeOptions.get(0))
                .placeholder(one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText.plainText("Select leave type"))
                .build();

        // Create section block with static select as accessory
        SlackSectionBlock leaveTypeSection = SlackBlockBuilder.sectionWithStaticSelect(
                "leave_type_category_block",
                "*Leave Type*",
                leaveTypeSelect
        );

        List<Object> blocks = new java.util.ArrayList<>();
        blocks.add(leaveTypeSection);

        // Add Annual Leave fields (shown by default since Annual Leave is preselected)
        blocks.add(SlackBlockBuilder.radioButtonsInput(
                "leave_duration_block",
                "leave_duration_action",
                "Duration",
                leaveDurationOptions,
                "FULL_DAY"
        ));

        blocks.add(SlackBlockBuilder.dateInput(
                "start_date_block",
                "start_date_action",
                "Start Date",
                "Select a date",
                false // required
        ));

        blocks.add(SlackBlockBuilder.dateInput(
                "end_date_block",
                "end_date_action",
                "End Date",
                "Select a date",
                false // required
        ));

        blocks.add(SlackBlockBuilder.plainTextInput(
                "reason_block",
                "reason_action",
                "Reason",
                "Optional: Provide a reason for your leave",
                true, // multiline
                true // optional
        ));

        // Create JSON metadata with thread context
        String metadataJson = SlackMetadataUtil.createMetadata(
                slackRequest.getUserId(),
                slackRequest.getChannelId(),
                slackRequest.getChannelName(),
                threadTs
        );

        // Modal shows leave type selector (with dispatch_action) AND annual leave form by default
        return SlackModalBuilder.create("Apply for Leave", "leave_application_submit")
                .withBlocks(blocks)
                .withPrivateMetadata(metadataJson)
                .build();
    }

    /**
     * Builds an Annual Leave modal with full date pickers and duration options
     * <p>
     * This modal is shown when user selects ANNUAL_LEAVE as the leave type.
     * Contains: duration, start date, end date, and reason fields.
     *
     * @param slackRequest The Slack command request containing user context
     * @param threadTs     The thread timestamp for posting updates later
     * @return A configured SlackModalView for annual leave application
     */
    // Package-private for testing
    SlackModalView buildAnnualLeaveModal(SlackCommandRequest slackRequest, String threadTs) {
        // Leave Type options (preselect ANNUAL_LEAVE)
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

        // Create static select element for leave type selection
        // Note: dispatch_action removed as it's not supported for static_select in section block accessories
        SlackStaticSelectElement leaveTypeSelect = SlackStaticSelectElement.builder()
                .actionId("leave_type_category_action")
                .options(leaveTypeOptions)
                .initialOption(leaveTypeOptions.get(0)) // Annual Leave
                .placeholder(one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText.plainText("Select leave type"))
                .build();

        // Create section block with static select as accessory
        SlackSectionBlock leaveTypeSection = SlackBlockBuilder.sectionWithStaticSelect(
                "leave_type_category_block",
                "*Leave Type*",
                leaveTypeSelect
        );

        List<Object> blocks = new java.util.ArrayList<>();
        blocks.add(leaveTypeSection);

        // Add Annual Leave fields
        blocks.add(SlackBlockBuilder.radioButtonsInput(
                "leave_duration_block",
                "leave_duration_action",
                "Duration",
                leaveDurationOptions,
                "FULL_DAY"
        ));

        blocks.add(SlackBlockBuilder.dateInput(
                "start_date_block",
                "start_date_action",
                "Start Date",
                "Select a date",
                false // required
        ));

        blocks.add(SlackBlockBuilder.dateInput(
                "end_date_block",
                "end_date_action",
                "End Date",
                "Select a date",
                false // required
        ));

        blocks.add(SlackBlockBuilder.plainTextInput(
                "reason_block",
                "reason_action",
                "Reason",
                "Optional: Provide a reason for your leave",
                true, // multiline
                true // optional
        ));

        // Create JSON metadata with thread context
        String metadataJson = SlackMetadataUtil.createMetadata(
                slackRequest.getUserId(),
                slackRequest.getChannelId(),
                slackRequest.getChannelName(),
                threadTs
        );

        return SlackModalBuilder.create("Apply for Annual Leave", "leave_application_submit")
                .withBlocks(blocks)
                .withPrivateMetadata(metadataJson)
                .build();
    }

    /**
     * Builds an Optional Holiday modal with holiday dropdown
     * <p>
     * This modal is shown when user selects OPTIONAL_HOLIDAY as the leave type.
     * Contains: holiday dropdown (dates from database)
     * No duration, date pickers, or reason fields (defaults to FULL_DAY, single day)
     *
     * @param slackRequest The Slack command request containing user context
     * @param threadTs     The thread timestamp for posting updates later
     * @return A configured SlackModalView for optional holiday application
     */
    // Package-private for testing
    SlackModalView buildOptionalHolidayModal(SlackCommandRequest slackRequest, String threadTs) {
        // Get holidays from database and convert to Slack options
        List<SlackOption> holidayOptions = optionalHolidayService.getAllHolidaysAsSlackOptions();

        // Leave Type options (preselect OPTIONAL_HOLIDAY)
        List<SlackOption> leaveTypeOptions = List.of(
                SlackOption.of("Annual Leave", "ANNUAL_LEAVE"),
                SlackOption.of("Optional Holiday", "OPTIONAL_HOLIDAY")
        );

        // Create static select element for leave type selection
        // Note: dispatch_action removed as it's not supported for static_select in section block accessories
        SlackStaticSelectElement leaveTypeSelect = SlackStaticSelectElement.builder()
                .actionId("leave_type_category_action")
                .options(leaveTypeOptions)
                .initialOption(leaveTypeOptions.get(1)) // Optional Holiday (index 1)
                .placeholder(one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText.plainText("Select leave type"))
                .build();

        // Create section block with static select as accessory
        SlackSectionBlock leaveTypeSection = SlackBlockBuilder.sectionWithStaticSelect(
                "leave_type_category_block",
                "*Leave Type*",
                leaveTypeSelect
        );

        List<Object> blocks = new java.util.ArrayList<>();
        blocks.add(leaveTypeSection);

        // Add Holiday dropdown (populated from database)
        // Shows format: "YYYY-MM-DD - Holiday Name"
        blocks.add(SlackBlockBuilder.staticSelectInput(
                "holiday_select_block",
                "holiday_select_action",
                "Select Holiday",
                holidayOptions,
                "Choose a holiday from the list",
                null // no initial selection
        ));

        // Create JSON metadata with thread context
        String metadataJson = SlackMetadataUtil.createMetadata(
                slackRequest.getUserId(),
                slackRequest.getChannelId(),
                slackRequest.getChannelName(),
                threadTs
        );

        return SlackModalBuilder.create("Optional Holiday", "leave_application_submit")
                .withBlocks(blocks)
                .withPrivateMetadata(metadataJson)
                .build();
    }
}
