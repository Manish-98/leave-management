package one.june.leave_management.adapter.inbound.slack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackAction;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionValue;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewClosedRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewState;
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
import one.june.leave_management.application.genai.dto.ParseResult;
import one.june.leave_management.application.genai.dto.ParsedLeaveRequest;
import one.june.leave_management.application.genai.service.LeaveParsingService;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.application.leave.service.OptionalHolidayService;
import one.june.leave_management.common.async.AsyncUtility;
import one.june.leave_management.common.exception.EmployeeNotFoundException;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final LeaveParsingService leaveParsingService;
    private final AsyncUtility asyncUtility;
    private final EmployeeRepository employeeRepository;

    public SlackLeaveOrchestrator(
            LeaveService leaveService,
            LeaveMapper leaveMapper,
            SlackApiClient slackApiClient,
            OptionalHolidayService optionalHolidayService,
            SlackLeaveRequestMapper slackLeaveRequestMapper,
            LeaveParsingService leaveParsingService,
            AsyncUtility asyncUtility,
            EmployeeRepository employeeRepository
    ) {
        this.leaveService = leaveService;
        this.leaveMapper = leaveMapper;
        this.slackApiClient = slackApiClient;
        this.optionalHolidayService = optionalHolidayService;
        this.slackLeaveRequestMapper = slackLeaveRequestMapper;
        this.leaveParsingService = leaveParsingService;
        this.asyncUtility = asyncUtility;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Processes a leave request from Slack and posts the result to the thread
     * <p>
     * This method:
     * 1. Converts the request to a command
     * 2. Calls the application service to ingest the leave
     * 3. Posts a success message to the Slack thread if successful
     * 4. Posts a failure message to the Slack thread if an error occurs
     * <p>
     * Runs with a new transaction (@Transactional REQUIRES_NEW).
     * Should be called asynchronously via AsyncUtility.
     *
     * @param leaveRequest The leave request from the modal
     * @param channelId    The channel ID where to post updates
     * @param threadTs     The thread timestamp for posting threaded replies
     * @param userId       The Slack user ID for tagging the user
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processLeaveRequest(
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
     * @param channelId   The channel ID where the message will be posted
     * @param threadTs    The thread timestamp to reply to
     * @param userTag     The Slack user tag (e.g., "&lt;@U12345&gt;")
     * @param status      The status message to display
     * @param originalText The original leave request text (optional, can be null)
     * @return The response containing the message timestamp (thread_ts), or null if posting fails
     */
    public SlackMessageResponse postThreadAnchorMessage(String channelId, String threadTs, String userTag, String status, String originalText) {
        try {
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated(
                    channelId, threadTs, userTag, status, originalText
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
     * Updates an existing message (typically to remove buttons after they've been clicked)
     * <p>
     * This is a best-effort operation - failures are logged but don't throw exceptions.
     *
     * @param channelId   The channel ID where the message exists
     * @param messageTs   The timestamp of the message to update
     * @param newMessage  The new message content
     */
    private void updateConfirmationMessage(String channelId, String messageTs, SlackMessageRequest newMessage) {
        try {
            slackApiClient.updateMessage(channelId, messageTs, newMessage);
            log.info("Successfully updated confirmation message");
        } catch (Exception e) {
            log.error("Failed to update confirmation message", e);
            // Don't throw - the button action should still proceed even if update fails
        }
    }

    /**
     * Handles view_submission events from Slack interactions endpoint
     * <p>
     * This method:
     * 1. Parses the form-encoded payload to extract the view submission request
     * 2. Routes based on callback_id (leave_application_submit or delete_leave_submit)
     * 3. For leave applications: extracts thread context, maps to ingestion request, triggers async processing
     * 4. For delete leave: validates selection, performs soft delete, posts success/error message
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

        log.info("View submission from user: {}, view ID: {}, callback_id: {}",
                submissionRequest.getUser().getId(),
                submissionRequest.getView().getId(),
                submissionRequest.getView().getCallbackId());

        // Route based on callback_id
        String callbackId = submissionRequest.getView().getCallbackId();
        if ("delete_leave_submit".equals(callbackId)) {
            // Handle delete leave submission
            log.info("Routing to delete leave handler");
            handleDeleteLeaveSubmission(submissionRequest);
            return;
        }

        // Default: Handle leave application submission (existing flow)
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
        asyncUtility.executeAsync(() -> processLeaveRequest(
                leaveRequest,
                channelId,
                threadTs,
                userId
        ));

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
     * This method is called when a user interacts with a block element.
     * It handles two types of containers:
     * <p>
     * 1. View-based actions (from modal interactions):
     * - Triggered when user changes leave type selection
     * - Flow: Parse → Extract leave type → Rebuild modal → Update modal
     * <p>
     * 2. Message-based actions (from button clicks):
     * - Triggered when user clicks Confirm/Edit buttons in confirmation message
     * - Flow: Parse → Route based on action ID → Handle Confirm or Edit
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

        log.info("Block action from user: {}, container type: {}",
                blockActionRequest.getUser().getId(),
                blockActionRequest.getContainer().getType());

        // Extract the action that triggered this event
        if (blockActionRequest.getActions() == null || blockActionRequest.getActions().isEmpty()) {
            log.error("No actions found in block action request");
            throw new RuntimeException("No actions found in block action request");
        }

        SlackAction action = blockActionRequest.getActions().get(0);
        String actionId = action.getActionId();
        log.info("Action ID: {}", actionId);

        // Route based on container type
        String containerType = blockActionRequest.getContainer().getType();

        if ("message".equals(containerType)) {
            // Handle message-based button actions (Confirm/Edit buttons)
            log.info("Handling message-based button action");
            handleMessageButtonAction(blockActionRequest, action);
        } else if ("view".equals(containerType)) {
            // Handle view-based actions (modal interactions)
            log.info("Handling view-based action");
            handleViewBlockAction(blockActionRequest, action);
        } else {
            log.warn("Unknown container type: {}", containerType);
            throw new RuntimeException("Unknown container type: " + containerType);
        }
    }

    /**
     * Handles message-based button actions from confirmation messages.
     * <p>
     * This routes to:
     * - confirm_leave_action → Create leave directly
     * - edit_leave_action → Open modal with pre-filled data
     * - select_leaves_button → Open delete leave modal with fresh trigger_id
     *
     * @param blockActionRequest The block action request
     * @param action             The action that was triggered
     */
    private void handleMessageButtonAction(SlackBlockActionRequest blockActionRequest, SlackAction action) {
        String actionId = action.getActionId();

        if ("confirm_leave_action".equals(actionId)) {
            log.info("Handling confirm leave action");
            handleConfirmLeaveAction(blockActionRequest, action);
        } else if ("edit_leave_action".equals(actionId)) {
            log.info("Handling edit leave action");
            handleEditLeaveAction(blockActionRequest, action);
        } else if ("open_modal_action".equals(actionId)) {
            log.info("Handling open modal action");
            handleOpenModalAction(blockActionRequest, action);
        } else if ("select_leaves_button".equals(actionId)) {
            log.info("Handling select leaves button action");
            handleSelectLeavesButton(blockActionRequest, action);
        } else {
            log.warn("Unknown message button action ID: {}", actionId);
            throw new RuntimeException("Unknown message button action ID: " + actionId);
        }
    }

    /**
     * Handles view-based block actions from modal interactions.
     * <p>
     * This is triggered when user changes leave type selection in the modal.
     * <p>
     * Flow:
     * 1. Extracts the selected leave type from the action
     * 2. Extracts view_id, view.hash, and private_metadata from the request
     * 3. Reconstructs modal based on selected leave type
     * 4. Updates the modal using Slack views.update API with hash validation
     *
     * @param blockActionRequest The block action request
     * @param action             The action that was triggered
     */
    private void handleViewBlockAction(SlackBlockActionRequest blockActionRequest, SlackAction action) {
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

        // Check if command has accompanying text for AI parsing
        String commandText = commandRequest.getText();
        if (commandText != null && !commandText.isBlank()) {
            log.info("Command has text for AI parsing: {}", commandText);

            // Handle command with text - route to AI parsing flow
            handleSlashCommandWithText(commandRequest, commandText);
        } else {
            // Handle command without text - use traditional modal flow
            log.info("No text provided, using traditional modal flow");
            handleSlashCommandWithoutText(commandRequest);
        }

        log.info("Successfully initiated slash command workflow");
    }

    /**
     * Handles slash command with accompanying text for AI-powered parsing.
     * <p>
     * This method:
     * 1. Posts a thread anchor message to get a timestamp (includes original text)
     * 2. Uses that timestamp for all subsequent threaded replies
     * 3. Parses the text using LeaveParsingService
     * 4. Shows a confirmation dialog with parsed data
     *
     * @param commandRequest The parsed slash command request
     * @param text           The text after the slash command
     */
    private void handleSlashCommandWithText(SlackCommandRequest commandRequest, String text) {
        String userTag = "<@" + commandRequest.getUserId() + ">";

        // Post thread anchor message to get a timestamp for threading
        log.info("Posting thread anchor message for AI-powered leave request");
        SlackMessageResponse anchorResponse = postThreadAnchorMessage(
                commandRequest.getChannelId(),
                null,  // No thread_ts yet - this becomes the anchor
                userTag,
                "Parsing your leave request with AI...",
                text  // Include original text in the anchor message
        );

        // Get the timestamp of the anchor message
        String threadTs = (anchorResponse != null) ? anchorResponse.getTs() : null;

        if (threadTs == null) {
            log.error("Failed to get thread anchor timestamp, messages may not be threaded");
        }

        // Trigger AI parsing and confirmation dialog asynchronously with thread context
        log.info("Triggering AI parsing with thread_ts: {}", threadTs);
        asyncUtility.executeAsync(() -> parseAndConfirmLeave(
                commandRequest, text, threadTs
        ));
    }

    /**
     * Handles slash command without text - uses traditional modal flow.
     * <p>
     * This method:
     * 1. Posts a thread anchor message to get a timestamp
     * 2. Uses that timestamp for all subsequent threaded replies
     * 3. Opens the modal for user to fill in leave details
     *
     * @param commandRequest The parsed slash command request
     */
    private void handleSlashCommandWithoutText(SlackCommandRequest commandRequest) {
        // Create user tag for mentioning the user
        String userTag = "<@" + commandRequest.getUserId() + ">";

        // Post thread anchor message to get a timestamp for threading
        log.info("Posting thread anchor message for modal-based leave request");
        SlackMessageResponse anchorResponse = postThreadAnchorMessage(
                commandRequest.getChannelId(),
                null,  // No thread_ts yet - this becomes the anchor
                userTag,
                "Opening leave request modal...",
                null  // No original text for modal flow
        );

        // Get the timestamp of the anchor message
        String threadTs = (anchorResponse != null) ? anchorResponse.getTs() : null;

        if (threadTs == null) {
            log.error("Failed to get thread anchor timestamp, messages may not be threaded");
        }

        // Open modal IMMEDIATELY and SYNCHRONOUSLY (trigger_id expires after 3 seconds!)
        log.info("Opening modal immediately with trigger_id: {}, thread_ts: {}",
                commandRequest.getTriggerId(), threadTs);
        openLeaveApplicationModal(commandRequest, threadTs);
    }

    /**
     * Opens a leave application modal
     * <p>
     * This method builds a modal with form fields for leave application
     * and opens it in Slack using the trigger_id from the slash command.
     * <p>
     * Should be called asynchronously via AsyncUtility to avoid blocking the HTTP response.
     * The async nature is important because:
     * - Slack requires a response within 3 seconds
     * - The modal opening is independent of the ACK response
     * - Errors in modal opening shouldn't affect the command ACK
     *
     * @param slackRequest The parsed Slack command request containing trigger_id
     * @param threadTs     The thread timestamp for posting updates later
     */
    public void openLeaveApplicationModal(SlackCommandRequest slackRequest, String threadTs) {
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

    /**
     * Parses leave text and shows confirmation dialog.
     * <p>
     * This method:
     * 1. Calls LeaveParsingService to parse the natural language text
     * 2. Shows a confirmation message with Confirm/Edit buttons
     * 3. On Confirm: Creates leave directly
     * 4. On Edit or failure: Opens the modal with pre-filled data
     * <p>
     * Should be called asynchronously via AsyncUtility.
     *
     * @param commandRequest The parsed Slack command request
     * @param text           The natural language text to parse
     * @param threadTs       The thread timestamp for posting updates
     */
    public void parseAndConfirmLeave(SlackCommandRequest commandRequest, String text, String threadTs) {
        try {
            log.info("Parsing leave text: {}", text);

            // Parse the text using AI (synchronous call) - pass Slack user ID
            ParseResult parseResult = leaveParsingService.parseLeaveRequest(text, commandRequest.getUserId());

            if (parseResult.isSuccess() && parseResult.getParsedRequest() != null) {
                log.info("Successfully parsed leave request with confidence: {}",
                        parseResult.getConfidenceScore());

                // Show confirmation dialog with parsed data
                showConfirmationDialog(commandRequest, parseResult, threadTs);
            } else {
                log.warn("Failed to parse leave request: {}",
                        parseResult.getErrorMessage());

                // Show error and offer to open modal
                showParsingErrorAndOfferModal(commandRequest, parseResult.getErrorMessage(), threadTs);
            }

        } catch (Exception e) {
            log.error("Unexpected error in parseAndConfirmLeaveAsync", e);

            // Show error and offer to open modal
            showParsingErrorAndOfferModal(commandRequest, e.getMessage(), threadTs);
        }
    }

    /**
     * Parses and confirms a leave request using AI (for slash command with text)
     * <p>
     * Uses response_url for all messages to keep them threaded with the original command.
     * <p>
     * Flow:
     * 1. Parses text using AI
     * 2. Shows confirmation dialog with Confirm/Edit buttons
     * 3. On Confirm: Creates leave directly
     * 4. On Edit or failure: Opens the modal with pre-filled data
     * <p>
     * Should be called asynchronously via AsyncUtility.
     *
     * @param commandRequest The parsed Slack command request
     * @param text           The natural language text to parse
     * @param responseUrl    The response_url for posting threaded messages
     */
    public void parseAndConfirmLeaveWithResponseUrl(SlackCommandRequest commandRequest, String text, String responseUrl) {
        try {
            log.info("Parsing leave text: {}", text);

            // Parse the text using AI (synchronous call) - pass Slack user ID
            ParseResult parseResult = leaveParsingService.parseLeaveRequest(text, commandRequest.getUserId());

            if (parseResult.isSuccess() && parseResult.getParsedRequest() != null) {
                log.info("Successfully parsed leave request with confidence: {}",
                        parseResult.getConfidenceScore());

                // Show confirmation dialog with parsed data using response_url
                showConfirmationDialogViaResponseUrl(commandRequest, parseResult, responseUrl);
            } else {
                log.warn("Failed to parse leave request: {}",
                        parseResult.getErrorMessage());

                // Show error and offer to open modal using response_url
                showParsingErrorAndOfferModalViaResponseUrl(commandRequest, parseResult.getErrorMessage(), responseUrl);
            }

        } catch (Exception e) {
            log.error("Unexpected error in parseAndConfirmLeaveWithResponseUrl", e);

            // Show error and offer to open modal using response_url
            showParsingErrorAndOfferModalViaResponseUrl(commandRequest, e.getMessage(), responseUrl);
        }
    }

    /**
     * Shows a confirmation dialog with the parsed leave data.
     * <p>
     * Posts a message with:
     * - Summary of parsed leave details
     * - Confirm button to create leave directly
     * - Edit button to open modal with pre-filled data
     *
     * @param commandRequest The parsed Slack command request
     * @param parseResult    The parsing result containing leave details
     * @param threadTs       The thread timestamp for posting updates
     */
    private void showConfirmationDialog(
            SlackCommandRequest commandRequest,
            ParseResult parseResult,
            String threadTs
    ) {
        try {
            ParsedLeaveRequest request = parseResult.getParsedRequest();

            // Build confirmation message with leave details
            String confirmationText = buildConfirmationMessage(request);

            // Build message with Confirm/Edit buttons
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    commandRequest.getChannelId(),
                    threadTs,
                    commandRequest.getUserId(),
                    confirmationText,
                    request // Store parsed request in metadata for button handlers
            );

            // Post confirmation message
            slackApiClient.postThreadReply(commandRequest.getChannelId(), threadTs, message);
            log.info("Successfully posted confirmation dialog");

        } catch (Exception e) {
            log.error("Failed to show confirmation dialog", e);

            // Fallback: Open modal with error message
            asyncUtility.executeAsync(() -> openLeaveApplicationModal(commandRequest, threadTs));
        }
    }

    /**
     * Shows parsing error and offers to open modal.
     *
     * @param commandRequest The parsed Slack command request
     * @param errorMessage  The error message to display
     * @param threadTs      The thread timestamp for posting updates
     */
    private void showParsingErrorAndOfferModal(
            SlackCommandRequest commandRequest,
            String errorMessage,
            String threadTs
    ) {
        try {
            // Post error message with button to open modal
            SlackMessageRequest message = SlackMessageTemplate.leaveParsingError(
                    commandRequest.getChannelId(),
                    threadTs,
                    commandRequest.getUserId(),
                    errorMessage
            );

            slackApiClient.postThreadReply(commandRequest.getChannelId(), threadTs, message);
            log.info("Successfully posted parsing error message");

        } catch (Exception e) {
            log.error("Failed to show parsing error", e);

            // Fallback: Just open the modal
            asyncUtility.executeAsync(() -> openLeaveApplicationModal(commandRequest, threadTs));
        }
    }

    /**
     * Shows a confirmation dialog with the parsed leave data using response_url.
     * <p>
     * Posts a message with:
     * - Summary of parsed leave details
     * - Confirm button to create leave directly
     * - Edit button to open modal with pre-filled data
     *
     * @param commandRequest The parsed Slack command request
     * @param parseResult    The parsing result containing leave details
     * @param responseUrl    The response_url for posting threaded messages
     */
    private void showConfirmationDialogViaResponseUrl(
            SlackCommandRequest commandRequest,
            ParseResult parseResult,
            String responseUrl
    ) {
        try {
            ParsedLeaveRequest request = parseResult.getParsedRequest();

            // Build confirmation message with leave details
            String confirmationText = buildConfirmationMessage(request);

            // Build message with Confirm/Edit buttons (no channel/threadTs needed for response_url)
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    null,  // channel not needed for response_url
                    null,  // threadTs not needed for response_url
                    commandRequest.getUserId(),
                    confirmationText,
                    request // Store parsed request in metadata for button handlers
            );

            // Post confirmation message via response_url
            slackApiClient.sendViaResponseUrl(responseUrl, message);
            log.info("Successfully posted confirmation dialog via response_url");

        } catch (Exception e) {
            log.error("Failed to show confirmation dialog via response_url", e);

            // Fallback: Open modal with error message
            asyncUtility.executeAsync(() -> openLeaveApplicationModal(commandRequest, null));
        }
    }

    /**
     * Shows parsing error and offers to open modal using response_url.
     *
     * @param commandRequest The parsed Slack command request
     * @param errorMessage  The error message to display
     * @param responseUrl   The response_url for posting threaded messages
     */
    private void showParsingErrorAndOfferModalViaResponseUrl(
            SlackCommandRequest commandRequest,
            String errorMessage,
            String responseUrl
    ) {
        try {
            // Post error message with button to open modal (no channel/threadTs needed)
            SlackMessageRequest message = SlackMessageTemplate.leaveParsingError(
                    null,  // channel not needed for response_url
                    null,  // threadTs not needed for response_url
                    commandRequest.getUserId(),
                    errorMessage
            );

            slackApiClient.sendViaResponseUrl(responseUrl, message);
            log.info("Successfully posted parsing error message via response_url");

        } catch (Exception e) {
            log.error("Failed to show parsing error via response_url", e);

            // Fallback: Just open the modal (if trigger_id is still valid)
            try {
                openLeaveApplicationModal(commandRequest, null);
            } catch (Exception ex) {
                log.error("Failed to open modal as fallback", ex);
            }
        }
    }

    /**
     * Builds a human-readable confirmation message from parsed leave request.
     *
     * @param request The parsed leave request
     * @return Formatted confirmation message
     */
    private String buildConfirmationMessage(ParsedLeaveRequest request) {
        StringBuilder message = new StringBuilder();
        message.append("*Leave Request Details*\n\n");

        // Dates
        if (request.getStartDate().equals(request.getEndDate())) {
            message.append(String.format("*Date:* %s\n",
                    request.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))));
        } else {
            message.append(String.format("*Duration:* %s to %s\n",
                    request.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    request.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))));
        }

        // Duration Type
        message.append(String.format("*Duration Type:* %s\n",
                java.text.MessageFormat.format("{0}", request.getDurationType())
                        .replace("_", " ")));

        // Leave Type
        message.append(String.format("*Leave Type:* %s\n",
                java.text.MessageFormat.format("{0}", request.getLeaveType())
                        .replace("_", " ")));

        // Reason (if provided)
        if (request.getReason() != null && !request.getReason().isBlank()) {
            message.append(String.format("*Reason:* %s\n", request.getReason()));
        }

        return message.toString();
    }

    /**
     * Handles the Confirm button action from the confirmation message.
     * <p>
     * This method:
     * 1. Updates the confirmation message to show processing status
     * 2. Extracts the parsed leave request JSON from the button value
     * 3. Deserializes it to ParsedLeaveRequest
     * 4. Maps to LeaveIngestionRequest
     * 5. Creates the leave via processLeaveRequest
     *
     * @param blockActionRequest The block action request
     * @param action             The action that was triggered
     */
    private void handleConfirmLeaveAction(SlackBlockActionRequest blockActionRequest, SlackAction action) {
        try {
            // Extract channel and message info from container
            String channelId = blockActionRequest.getContainer().getChannelId();
            String messageTs = blockActionRequest.getContainer().getMessageTs();

            // First: Update the confirmation message to remove buttons and show processing status
            updateConfirmationMessage(channelId, messageTs, SlackMessageTemplate.buttonClickedProcessing());

            // Extract the JSON value from the button
            String requestJson = action.getValue();
            log.info("Parsed request JSON from confirm button: {}", requestJson);

            // Deserialize JSON to ParsedLeaveRequest
            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
            ParsedLeaveRequest parsedRequest = objectMapper.readValue(requestJson, ParsedLeaveRequest.class);
            log.info("Deserialized parsed request: {}", parsedRequest);

            // Map ParsedLeaveRequest to LeaveIngestionRequest using the mapper
            LeaveIngestionRequest leaveRequest = slackLeaveRequestMapper.toLeaveIngestionRequest(parsedRequest);
            log.info("Mapped to leave ingestion request: {}", leaveRequest);

            String userId = blockActionRequest.getUser().getId();

            log.info("Processing leave for userId: {}, channelId: {}, messageTs: {}",
                    userId, channelId, messageTs);

            // Process the leave request asynchronously
            asyncUtility.executeAsync(() -> processLeaveRequest(
                    leaveRequest,
                    channelId,
                    messageTs,
                    userId
            ));

            log.info("Triggered async leave processing from confirm button");

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize parsed leave request from confirm button", e);
            throw new RuntimeException("Failed to parse leave request data", e);
        }
    }

    /**
     * Handles the Edit button action from the confirmation message.
     * <p>
     * This method:
     * 1. Updates the confirmation message to show opening modal status
     * 2. Extracts the parsed leave request JSON from the button value
     * 3. Deserializes it to ParsedLeaveRequest
     * 4. Opens the modal with pre-filled data from the parsed request
     *
     * @param blockActionRequest The block action request
     * @param action             The action that was triggered
     */
    private void handleEditLeaveAction(SlackBlockActionRequest blockActionRequest, SlackAction action) {
        try {
            // Extract channel and message info from container
            String channelId = blockActionRequest.getContainer().getChannelId();
            String messageTs = blockActionRequest.getContainer().getMessageTs();

            // First: Update the confirmation message to remove buttons and show opening modal status
            updateConfirmationMessage(channelId, messageTs, SlackMessageTemplate.buttonClickedOpeningModal());

            // Extract the JSON value from the button
            String requestJson = action.getValue();
            log.info("Parsed request JSON from edit button: {}", requestJson);

            // Deserialize JSON to ParsedLeaveRequest
            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
            ParsedLeaveRequest parsedRequest = objectMapper.readValue(requestJson, ParsedLeaveRequest.class);
            log.info("Deserialized parsed request: {}", parsedRequest);

            // Build context for opening modal
            String userId = blockActionRequest.getUser().getId();

            // Create a minimal SlackCommandRequest for modal building
            SlackCommandRequest slackRequest = new SlackCommandRequest();
            slackRequest.setUserId(userId);
            slackRequest.setChannelId(channelId);
            slackRequest.setChannelName(""); // Channel name not available in message container

            // Build and open modal with pre-filled data
            SlackModalView modalView = buildModalWithParsedData(slackRequest, messageTs, parsedRequest);

            // Open modal using trigger_id from the block action request
            slackApiClient.openModal(blockActionRequest.getTriggerId(), modalView);

            log.info("Successfully opened modal with pre-filled data from edit button");

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize parsed leave request from edit button", e);
            throw new RuntimeException("Failed to parse leave request data", e);
        }
    }

    /**
     * Handles the Open Modal button action from parsing error messages.
     * <p>
     * This method opens a blank modal when user clicks the button after parsing fails.
     *
     * @param blockActionRequest The block action request
     * @param action             The action that was triggered
     */
    private void handleOpenModalAction(SlackBlockActionRequest blockActionRequest, SlackAction action) {
        String userId = blockActionRequest.getUser().getId();
        String channelId = blockActionRequest.getContainer().getChannelId();
        String messageTs = blockActionRequest.getContainer().getMessageTs();

        // Create a minimal SlackCommandRequest
        SlackCommandRequest slackRequest = new SlackCommandRequest();
        slackRequest.setUserId(userId);
        slackRequest.setChannelId(channelId);
        slackRequest.setChannelName(""); // Channel name not available

        // Open blank modal
        asyncUtility.executeAsync(() -> openLeaveApplicationModal(slackRequest, messageTs));
        log.info("Opened blank modal from open modal button action");
    }

    /**
     * Builds a modal with pre-filled data from the parsed leave request.
     * <p>
     * This creates a modal with all fields pre-populated from the AI-parsed data.
     *
     * @param slackRequest  The Slack request context
     * @param threadTs      The thread timestamp
     * @param parsedRequest The parsed leave request
     * @return Configured modal view with pre-filled data
     */
    private SlackModalView buildModalWithParsedData(
            SlackCommandRequest slackRequest,
            String threadTs,
            ParsedLeaveRequest parsedRequest
    ) {
        // Determine which modal to build based on leave type
        if (parsedRequest.getLeaveType() == one.june.leave_management.domain.leave.model.LeaveType.OPTIONAL_HOLIDAY) {
            return buildOptionalHolidayModalWithData(slackRequest, threadTs, parsedRequest);
        } else {
            return buildAnnualLeaveModalWithData(slackRequest, threadTs, parsedRequest);
        }
    }

    /**
     * Builds an Annual Leave modal with pre-filled data.
     *
     * @param slackRequest  The Slack request context
     * @param threadTs      The thread timestamp
     * @param parsedRequest The parsed leave request
     * @return Configured modal view
     */
    private SlackModalView buildAnnualLeaveModalWithData(
            SlackCommandRequest slackRequest,
            String threadTs,
            ParsedLeaveRequest parsedRequest
    ) {
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

        // Create static select element for leave type (preselect Annual Leave)
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

        // Add duration radio buttons (preselect parsed duration)
        String initialDuration = parsedRequest.getDurationType() != null
                ? parsedRequest.getDurationType().name()
                : "FULL_DAY";
        blocks.add(SlackBlockBuilder.radioButtonsInput(
                "leave_duration_block",
                "leave_duration_action",
                "Duration",
                leaveDurationOptions,
                initialDuration
        ));

        // Add start date (pre-filled)
        blocks.add(SlackBlockBuilder.dateInput(
                "start_date_block",
                "start_date_action",
                "Start Date",
                "Select a date",
                false // required
        ));

        // Add end date (pre-filled)
        blocks.add(SlackBlockBuilder.dateInput(
                "end_date_block",
                "end_date_action",
                "End Date",
                "Select a date",
                false // required
        ));

        // Add reason (pre-filled if available)
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
     * Builds an Optional Holiday modal with pre-filled data.
     *
     * @param slackRequest  The Slack request context
     * @param threadTs      The thread timestamp
     * @param parsedRequest The parsed leave request
     * @return Configured modal view
     */
    private SlackModalView buildOptionalHolidayModalWithData(
            SlackCommandRequest slackRequest,
            String threadTs,
            ParsedLeaveRequest parsedRequest
    ) {
        // Get holidays from database
        List<SlackOption> holidayOptions = optionalHolidayService.getAllHolidaysAsSlackOptions();

        // Leave Type options (preselect Optional Holiday)
        List<SlackOption> leaveTypeOptions = List.of(
                SlackOption.of("Annual Leave", "ANNUAL_LEAVE"),
                SlackOption.of("Optional Holiday", "OPTIONAL_HOLIDAY")
        );

        // Create static select element for leave type (preselect Optional Holiday)
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

        // Add Holiday dropdown
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

    /**
     * Handles /delete-leave slash command from Slack
     * <p>
     * This method:
     * 1. Returns immediately to avoid Slack timeout
     * 2. Triggers async processing to fetch and display leaves
     * <p>
     * Uses response_url pattern: return fast, then post updates asynchronously.
     * This avoids both the 3-second HTTP timeout and trigger_id expiration issues.
     *
     * @param commandRequest The parsed slash command request
     */
    public void handleDeleteLeaveSlashCommand(SlackCommandRequest commandRequest) {
        log.info("Handling /delete-leave command from user: {} in channel: {}",
                commandRequest.getUserId(),
                commandRequest.getChannelName());

        // Trigger async processing to fetch leaves and post message with buttons
        asyncUtility.executeAsync(() -> processDeleteLeaveWithButtons(commandRequest));

        log.info("Successfully initiated delete leave command workflow");
    }

    /**
     * Processes delete leave request asynchronously with button-based UI.
     * <p>
     * This method:
     * 1. Posts an anchor message
     * 2. Posts a message with a "Select Leaves" button
     * 3. User clicks button → gets fresh trigger_id → opens modal
     * <p>
     * Runs asynchronously to avoid blocking the HTTP response.
     * Uses button pattern to get fresh trigger_id (avoids expiration).
     *
     * @param commandRequest The parsed slash command request
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDeleteLeaveWithButtons(SlackCommandRequest commandRequest) {
        log.info("Processing delete leave request with button for user: {}",
                commandRequest.getUserId());

        String userTag = "<@" + commandRequest.getUserId() + ">";
        String channelId = commandRequest.getChannelId();

        try {
            // Post anchor message
            log.info("Posting thread anchor message");
            SlackMessageResponse anchorResponse = postThreadAnchorMessage(
                    channelId,
                    null,
                    userTag,
                    "Loading leave deletion options...",
                    null
            );

            String threadTs = (anchorResponse != null) ? anchorResponse.getTs() : null;

            // Post message with "Select Leaves" button
            log.info("Posting message with select leaves button");
            postSelectLeavesButtonMessage(channelId, threadTs, userTag);

        } catch (Exception e) {
            log.error("Failed to process delete leave request for user: {}",
                    commandRequest.getUserId(), e);
            postDeleteLeaveErrorToThread(channelId, userTag, e.getMessage());
        }
    }

    /**
     * Handles the "Select Leaves to Delete" button click.
     * <p>
     * This method:
     * 1. Updates the button message to remove the button (shows "Opening modal...")
     * 2. Gets a FRESH trigger_id from the button click (valid for 3 seconds)
     * 3. Fetches leaves from database
     * 4. Opens modal immediately
     * <p>
     * Called from handleBlockAction when user clicks the button.
     *
     * @param blockActionRequest The block action request
     * @param action             The action that was triggered
     */
    private void handleSelectLeavesButton(SlackBlockActionRequest blockActionRequest, SlackAction action) {
        String channelId = blockActionRequest.getContainer().getChannelId();
        String messageTs = blockActionRequest.getContainer().getMessageTs();
        String threadTs = blockActionRequest.getContainer().getThreadTs();
        String userId = blockActionRequest.getUser().getId();
        String triggerId = blockActionRequest.getTriggerId();

        log.info("Handling select leaves button click for user: {}, trigger_id: {}", userId, triggerId);

        // First: Update the button message to remove the button and show processing status
        updateSelectLeavesButtonMessage(channelId, messageTs);

        try {
            // Fetch leaves from database synchronously (must be fast!)
            String employeeId = getEmployeeIdFromSlackUserId(userId);
            List<LeaveDto> activeLeaves = leaveService.findActiveLeavesByUserId(employeeId);

            if (activeLeaves.isEmpty()) {
                // No leaves available
                log.info("No active leaves found for user: {}", userId);
                String userTag = "<@" + userId + ">";
                postNoLeavesAvailableToThread(channelId, threadTs, userTag);
            } else {
                // Open modal with leave selection IMMEDIATELY
                log.info("Found {} active leaves, opening modal now", activeLeaves.size());
                openDeleteLeaveModalWithTrigger(triggerId, userId, channelId, threadTs, activeLeaves);
            }

        } catch (Exception e) {
            log.error("Failed to open delete leave modal for user: {}", userId, e);
            String userTag = "<@" + userId + ">";
            postDeleteLeaveErrorToThread(channelId, userTag, e.getMessage());
        }
    }

    /**
     * Gets the employee ID (UUID) from a Slack user ID.
     * This is a helper method that looks up the employee by Slack ID.
     *
     * @param slackUserId The Slack user ID
     * @return The employee UUID as a string
     * @throws IllegalArgumentException if employee not found
     */
    private String getEmployeeIdFromSlackUserId(String slackUserId) {
        // Look up employee by Slack ID
        one.june.leave_management.domain.employee.model.Employee employee =
                employeeRepository.findBySlackId(slackUserId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Employee not found for Slack user ID: " + slackUserId));
        return employee.getId().toString();
    }

    /**
     * Posts a message with a "Select Leaves" button.
     *
     * @param channelId The channel ID
     * @param threadTs  The thread timestamp
     * @param userTag   The user tag
     */
    private void postSelectLeavesButtonMessage(String channelId, String threadTs, String userTag) {
        try {
            SlackMessageRequest message = SlackMessageTemplate.selectLeavesButton(
                    channelId, threadTs, userTag
            );
            slackApiClient.postThreadReply(channelId, threadTs, message);
            log.info("Posted select leaves button message");
        } catch (Exception e) {
            log.error("Failed to post select leaves button message", e);
        }
    }

    /**
     * Updates the "Select Leaves" button message to remove the button.
     * <p>
     * This is called when the user clicks the button to prevent them from clicking it again.
     *
     * @param channelId  The channel ID
     * @param messageTs  The message timestamp to update
     */
    private void updateSelectLeavesButtonMessage(String channelId, String messageTs) {
        try {
            SlackMessageRequest newMessage = SlackMessageTemplate.selectLeavesButtonClicked();
            slackApiClient.updateMessage(channelId, messageTs, newMessage);
            log.info("Updated select leaves button message (removed button)");
        } catch (Exception e) {
            log.error("Failed to update select leaves button message", e);
            // Don't throw - the button action should still proceed even if update fails
        }
    }

    /**
     * Opens a delete leave modal using the provided trigger_id.
     *
     * @param triggerId   The fresh trigger_id from button click
     * @param userId      The Slack user ID
     * @param channelId   The channel ID
     * @param threadTs    The thread timestamp
     * @param activeLeaves List of active leaves
     */
    private void openDeleteLeaveModalWithTrigger(String triggerId, String userId, String channelId, String threadTs, List<LeaveDto> activeLeaves) {
        try {
            log.info("Building delete leave modal with {} leaves", activeLeaves.size());

            // Build modal with thread context
            SlackModalView modalView = buildDeleteLeaveModalWithThread(userId, channelId, threadTs, activeLeaves);

            log.debug("Delete leave modal structure: {}", modalView);

            slackApiClient.openModal(triggerId, modalView);

            log.info("Delete leave modal opened successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to open delete leave modal for user: {}", userId, e);
            String userTag = "<@" + userId + ">";
            postDeleteLeaveErrorToThread(channelId, userTag, "Failed to open modal: " + e.getMessage());
        }
    }

    /**
     * Builds a delete leave modal with thread context.
     *
     * @param userId       The Slack user ID
     * @param channelId    The channel ID
     * @param threadTs     The thread timestamp
     * @param activeLeaves List of active leaves
     * @return Configured modal view
     */
    private SlackModalView buildDeleteLeaveModalWithThread(String userId, String channelId, String threadTs, List<LeaveDto> activeLeaves) {
        // Convert leaves to Slack options
        List<SlackOption> leaveOptions = activeLeaves.stream()
                .map(leave -> {
                    String label = formatLeaveForSelection(leave);
                    String value = leave.getId().toString();
                    return SlackOption.of(label, value);
                })
                .collect(Collectors.toList());

        List<Object> blocks = new java.util.ArrayList<>();

        // Add leave selection dropdown
        blocks.add(SlackBlockBuilder.staticSelectInput(
                "leave_select_block",
                "leave_select_action",
                "Select Leave to Delete",
                leaveOptions,
                "Choose a leave request to delete",
                null // no initial selection
        ));

        // Create JSON metadata with thread context
        String metadataJson = SlackMetadataUtil.createMetadata(
                userId,
                channelId,
                "", // channel name not available
                threadTs  // Include thread timestamp for posting result in thread
        );

        return SlackModalBuilder.create("Delete Leave Request", "delete_leave_submit")
                .withBlocks(blocks)
                .withPrivateMetadata(metadataJson)
                .build();
    }

    /**
     * Posts "no leaves available" message to thread.
     *
     * @param channelId The channel ID
     * @param threadTs  The thread timestamp
     * @param userTag   The user tag
     */
    private void postNoLeavesAvailableToThread(String channelId, String threadTs, String userTag) {
        try {
            SlackMessageRequest message = SlackMessageTemplate.noLeavesAvailable(
                    channelId, threadTs, userTag
            );
            slackApiClient.postThreadReply(channelId, threadTs, message);
            log.info("Posted no leaves available message to thread");
        } catch (Exception e) {
            log.error("Failed to post no leaves available message", e);
        }
    }

    /**
     * Posts error message to thread.
     *
     * @param channelId    The channel ID
     * @param userTag      The user tag
     * @param errorMessage The error message
     */
    private void postDeleteLeaveErrorToThread(String channelId, String userTag, String errorMessage) {
        try {
            SlackMessageRequest message = SlackMessageTemplate.deleteLeaveError(
                    channelId, null, userTag, errorMessage
            );
            slackApiClient.postMessage(channelId, message);
            log.info("Posted error message to channel");
        } catch (Exception e) {
            log.error("Failed to post error message", e);
        }
    }

    /**
     * Formats a leave DTO into a human-readable string for the selection dropdown.
     *
     * @param leave The leave DTO
     * @return Formatted string (e.g., "2024-01-15 to 2024-01-20 - ANNUAL_LEAVE - APPROVED")
     */
    private String formatLeaveForSelection(LeaveDto leave) {
        // Format dates as "Jan 15" or "Jan 15-20"
        String dateStr;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        if (leave.getStartDate().equals(leave.getEndDate())) {
            // Single day: "Jan 15"
            dateStr = leave.getStartDate().format(formatter);
        } else {
            // Date range: "Jan 15-20"
            dateStr = String.format("%s-%s",
                    leave.getStartDate().format(formatter),
                    leave.getEndDate().format(formatter));
        }

        // Calculate duration in days
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                leave.getStartDate(),
                leave.getEndDate()
        ) + 1; // +1 because both start and end dates are inclusive

        // Convert enum to friendly text
        String leaveType = formatLeaveTypeFriendly(leave.getType().toString());

        // Format: "Jan 15-20 (6 days): Annual Leave"
        return String.format("%s (%d day%s): %s",
                dateStr,
                days,
                days == 1 ? "" : "s",
                leaveType
        );
    }

    /**
     * Converts leave type enum to friendly text.
     *
     * @param leaveType The leave type enum name (e.g., "ANNUAL_LEAVE")
     * @return Friendly text (e.g., "Annual Leave")
     */
    private String formatLeaveTypeFriendly(String leaveType) {
        // Convert ANNUAL_LEAVE -> Annual Leave
        // Or just capitalize first letter and lowercase rest
        if (leaveType == null || leaveType.isEmpty()) {
            return "Leave";
        }

        // Handle snake_case: ANNUAL_LEAVE -> Annual Leave
        String[] parts = leaveType.split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.charAt(0))
                  .append(part.substring(1).toLowerCase());
        }

        return result.toString();
    }

    /**
     * Handles the delete leave modal submission.
     * <p>
     * This method:
     * 1. Extracts the selected leave ID from modal state
     * 2. Validates that leave exists and belongs to user
     * 3. Performs soft delete (updates status to DEACTIVATED)
     * 4. Posts success/failure message to thread
     * <p>
     * Called from handleViewSubmission when callback_id is "delete_leave_submit".
     *
     * @param submissionRequest The view submission request
     */
    public void handleDeleteLeaveSubmission(SlackViewSubmissionRequest submissionRequest) {
        log.info("Handling delete leave submission from user: {}",
                submissionRequest.getUser().getId());

        // Extract metadata
        String metadataJson = submissionRequest.getView().getPrivateMetadata();
        String slackUserId = SlackMetadataUtil.extractUserId(metadataJson);
        String channelId = SlackMetadataUtil.extractChannelId(metadataJson);
        String threadTs = SlackMetadataUtil.extractThreadTs(metadataJson);

        log.info("Extracted thread context - userId: {}, channelId: {}, threadTs: {}",
                slackUserId, channelId, threadTs);

        try {
            // Get employee ID from Slack user ID
            String employeeId = getEmployeeIdFromSlackUserId(slackUserId);

            // Extract selected leave ID from modal state
            String selectedLeaveId = extractSelectedLeaveId(submissionRequest);

            if (selectedLeaveId == null || selectedLeaveId.isBlank()) {
                throw new IllegalArgumentException("No leave selected");
            }

            UUID leaveId = UUID.fromString(selectedLeaveId);

            // Perform soft delete (validates ownership)
            LeaveDto deletedLeave = leaveService.softDeleteLeave(leaveId, employeeId);

            log.info("Successfully soft deleted leave {} for user {}", leaveId, slackUserId);

            // Post success message to thread
            postDeleteLeaveSuccessMessage(channelId, threadTs, slackUserId, deletedLeave);

        } catch (Exception e) {
            log.error("Failed to delete leave for user: {}", slackUserId, e);
            // Post error message to thread
            postDeleteLeaveFailureMessage(channelId, threadTs, slackUserId, e.getMessage());
        }
    }

    /**
     * Extracts the selected leave ID from the view submission state.
     *
     * @param submissionRequest The view submission request
     * @return The selected leave ID, or null if not found
     */
    private String extractSelectedLeaveId(SlackViewSubmissionRequest submissionRequest) {
        try {
            SlackViewState state = submissionRequest.getView().getState();
            if (state != null && state.getValues() != null) {
                Map<String, Map<String, SlackBlockActionValue>> values = state.getValues();
                if (values.containsKey("leave_select_block")) {
                    Map<String, SlackBlockActionValue> blockValues = values.get("leave_select_block");
                    if (blockValues.containsKey("leave_select_action")) {
                        SlackBlockActionValue actionValue = blockValues.get("leave_select_action");
                        if (actionValue != null && actionValue.getSelectedOption() != null) {
                            return actionValue.getSelectedOption().getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract selected leave ID from submission", e);
        }
        return null;
    }

    /**
     * Posts a success message after leave deletion.
     *
     * @param channelId    The channel to post to
     * @param threadTs     The thread timestamp for posting threaded replies
     * @param userId       The Slack user ID
     * @param deletedLeave The deleted leave details
     */
    private void postDeleteLeaveSuccessMessage(String channelId, String threadTs, String userId, LeaveDto deletedLeave) {
        try {
            String userTag = "<@" + userId + ">";
            SlackMessageRequest message = SlackMessageTemplate.leaveDeleted(
                    channelId,
                    threadTs,
                    userTag,
                    deletedLeave
            );
            slackApiClient.postThreadReply(channelId, threadTs, message);
            log.info("Posted delete success message to thread");
        } catch (Exception e) {
            log.error("Failed to post delete success message", e);
        }
    }

    /**
     * Posts a failure message after leave deletion fails.
     *
     * @param channelId    The channel to post to
     * @param threadTs     The thread timestamp for posting threaded replies
     * @param userId       The Slack user ID
     * @param errorMessage The error message
     */
    private void postDeleteLeaveFailureMessage(String channelId, String threadTs, String userId, String errorMessage) {
        try {
            String userTag = "<@" + userId + ">";
            SlackMessageRequest message = SlackMessageTemplate.leaveDeleteFailed(
                    channelId,
                    threadTs,
                    userTag,
                    errorMessage
            );
            slackApiClient.postThreadReply(channelId, threadTs, message);
            log.info("Posted delete failure message to thread");
        } catch (Exception e) {
            log.error("Failed to post delete failure message", e);
        }
    }
}
