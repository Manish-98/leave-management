package one.june.leave_management.adapter.inbound.slack.util;

import one.june.leave_management.adapter.outbound.slack.builder.SlackMessageBuilder;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.application.genai.dto.ParsedLeaveRequest;
import one.june.leave_management.application.leave.dto.LeaveDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Template utility for creating Slack messages for leave-related operations
 * <p>
 * This class provides pre-built templates for common Slack message types.
 * All methods return SlackMessageRequest objects that can be passed to
 * SlackApiClient methods for sending.
 * <p>
 * This is a pure builder utility - it only creates message objects,
 * it does NOT send them. The caller is responsible for invoking
 * the SlackApiClient to send the messages.
 */
public class SlackMessageTemplate {

    /**
     * Creates a message for when a leave request is initiated
     * <p>
     * This message serves as the thread anchor for all subsequent updates.
     *
     * @param channelId   The channel ID where the message will be posted
     * @param threadTs    The thread timestamp to reply to (slash command message timestamp)
     * @param userTag     The Slack user tag (e.g., "&lt;@U12345&gt;")
     * @param status      The status message to display
     * @param originalText The original leave request text (optional, can be null)
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveRequestInitiated(String channelId, String threadTs, String userTag, String status, String originalText) {
        String message;
        if (originalText != null && !originalText.isBlank()) {
            message = "📝 Leave request for " + userTag + " - \"" + originalText + "\" - " + status;
        } else {
            message = "📝 Leave request for " + userTag + " - " + status;
        }

        return SlackMessageBuilder
                .create(message)
                .toChannel(channelId)
                .inThread(threadTs)
                .build();
    }

    /**
     * Creates a success message when a leave is created
     * <p>
     * This message is posted as a threaded reply to the original anchor message.
     *
     * @param channelId The channel ID where the thread exists
     * @param threadTs  The thread timestamp of the parent message
     * @param userId    The Slack user ID for tagging
     * @param leaveDto  The created leave details
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveCreated(String channelId, String threadTs, String userId, LeaveDto leaveDto) {
        String userTag = "<@" + userId + ">";
        String dates = formatLeaveDates(leaveDto);
        String duration = leaveDto.getDurationType() != null
                ? leaveDto.getDurationType().name()
                : "FULL_DAY";

        Map<String, String> fields = new HashMap<>();
        fields.put("User", userTag);
        fields.put("Leave ID", leaveDto.getId().toString());
        fields.put("Type", leaveDto.getType().toString());
        fields.put("Dates", dates);
        fields.put("Duration", formatDuration(duration));
        fields.put("Status", leaveDto.getStatus().toString());

        return SlackMessageBuilder
                .create("✅ Leave created successfully for " + userTag)
                .withHeader("✅ Leave Created Successfully", true)
                .withFields(fields)
                .toChannel(channelId)
                .inThread(threadTs)
                .build();
    }

    /**
     * Creates a failure message when leave creation fails
     * <p>
     * This message is posted as a threaded reply to the original anchor message.
     *
     * @param channelId     The channel ID where the thread exists
     * @param threadTs      The thread timestamp of the parent message
     * @param userId        The Slack user ID for tagging
     * @param errorMessage  The error message describing what went wrong
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveRequestFailed(String channelId, String threadTs, String userId, String errorMessage) {
        String userTag = "<@" + userId + ">";
        String errorText = errorMessage != null ? errorMessage : "Unknown error";

        return SlackMessageBuilder
                .create("❌ Leave request failed for " + userTag)
                .withHeader("❌ Leave Request Failed", true)
                .withSection("User", userTag)
                .withSection("Error", errorText)
                .withDivider()
                .withSection("Please try again or contact HR for assistance.")
                .toChannel(channelId)
                .inThread(threadTs)
                .build();
    }

    /**
     * Creates a cancellation message when a modal is closed without submitting
     * <p>
     * This message is posted as a threaded reply to the original anchor message.
     *
     * @param channelId The channel ID where the thread exists
     * @param threadTs  The thread timestamp of the parent message
     * @param userId    The Slack user ID for tagging
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveRequestCancelled(String channelId, String threadTs, String userId) {
        String userTag = "<@" + userId + ">";

        return SlackMessageBuilder
                .create("❌ Leave request cancelled for " + userTag)
                .withHeader("❌ Leave Request Cancelled", true)
                .withSection("User", userTag)
                .withSection("Status", "The leave request modal was cancelled without submitting.")
                .toChannel(channelId)
                .inThread(threadTs)
                .build();
    }

    /**
     * Creates a processing status message to replace confirmation dialog after Confirm button click
     * <p>
     * This message is used to update the confirmation message in-place, removing buttons
     * and showing that the request is being processed.
     *
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest buttonClickedProcessing() {
        return SlackMessageBuilder
                .create("⏳ Processing your leave request...")
                .build();
    }

    /**
     * Creates an opening modal status message to replace confirmation dialog after Edit button click
     * <p>
     * This message is used to update the confirmation message in-place, removing buttons
     * and showing that the modal is being opened.
     *
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest buttonClickedOpeningModal() {
        return SlackMessageBuilder
                .create("📝 Opening modal for editing...")
                .build();
    }

    /**
     * Formats leave dates for display
     * <p>
     * Handles single day and date range scenarios.
     *
     * @param leaveDto The leave DTO containing date range
     * @return Formatted date string (e.g., "Jan 01, 2025" or "Jan 01 - Jan 05, 2025")
     */
    private static String formatLeaveDates(LeaveDto leaveDto) {
        if (leaveDto.getDateRange() == null) {
            return "N/A";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        String startDate = leaveDto.getDateRange().getStartDate().format(formatter);

        if (leaveDto.getDateRange().getEndDate() == null ||
                leaveDto.getDateRange().getStartDate().equals(leaveDto.getDateRange().getEndDate())) {
            return startDate;
        }

        String endDate = leaveDto.getDateRange().getEndDate().format(formatter);
        return startDate + " - " + endDate;
    }

    /**
     * Formats duration type for display
     * <p>
     * Converts enum values to human-readable format.
     *
     * @param duration The duration type enum value
     * @return Human-readable duration string
     */
    private static String formatDuration(String duration) {
        return switch (duration) {
            case "FULL_DAY" -> "Full Day";
            case "FIRST_HALF" -> "First Half";
            case "SECOND_HALF" -> "Second Half";
            default -> duration;
        };
    }

    /**
     * Creates a confirmation message with parsed leave details and Confirm/Edit buttons.
     * <p>
     * This message shows the AI-parsed leave details and allows the user to:
     * - Confirm: Create the leave directly
     * - Edit: Open the modal with pre-filled data
     *
     * @param channelId      The channel ID where the thread exists
     * @param threadTs       The thread timestamp of the parent message
     * @param userId         The Slack user ID for tagging
     * @param confirmationText The formatted leave details
     * @param parsedRequest  The parsed leave request (stored in button value)
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveConfirmation(
            String channelId,
            String threadTs,
            String userId,
            String confirmationText,
            ParsedLeaveRequest parsedRequest
    ) {
        String userTag = "<@" + userId + ">";
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        try {
            // Serialize parsed request to JSON for button value
            String requestJson = objectMapper.writeValueAsString(parsedRequest);

            return SlackMessageBuilder
                    .create("🤖 AI-parsed leave request for " + userTag)
                    .withHeader("🤖 Leave Request Parsed", true)
                    .withSection(confirmationText)
                    .withDivider()
                    .withSection("Please confirm the details above or edit in the modal.")
                    .withButton("Confirm", "confirm_leave_action", requestJson, "primary")
                    .withButton("Edit", "edit_leave_action", requestJson, "default")
                    .toChannel(channelId)
                    .inThread(threadTs)
                    .build();

        } catch (JsonProcessingException e) {
            // Fallback without buttons if serialization fails
            return SlackMessageBuilder
                    .create("🤖 AI-parsed leave request for " + userTag)
                    .withHeader("🤖 Leave Request Parsed", true)
                    .withSection(confirmationText)
                    .withSection("⚠️ Could not create buttons. Please use the modal.")
                    .toChannel(channelId)
                    .inThread(threadTs)
                    .build();
        }
    }

    /**
     * Creates an error message when AI parsing fails.
     * <p>
     * This message informs the user that parsing failed and offers a button
     * to open the modal for manual entry.
     *
     * @param channelId    The channel ID where the thread exists
     * @param threadTs     The thread timestamp of the parent message
     * @param userId       The Slack user ID for tagging
     * @param errorMessage The error message from parsing
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveParsingError(
            String channelId,
            String threadTs,
            String userId,
            String errorMessage
    ) {
        String userTag = "<@" + userId + ">";

        return SlackMessageBuilder
                .create("⚠️ Could not parse leave request for " + userTag)
                .withHeader("⚠️ Parsing Failed", true)
                .withSection("User", userTag)
                .withSection("Error", errorMessage != null ? errorMessage : "Unknown error")
                .withDivider()
                .withSection("I couldn't understand your leave request. Please use the modal to fill in the details.")
                .withButton("Open Modal", "open_modal_action", "open_modal", "primary")
                .toChannel(channelId)
                .inThread(threadTs)
                .build();
    }

    /**
     * Creates a message when no active leaves are available for deletion.
     *
     * @param channelId The channel ID to post to
     * @param userTag   The Slack user tag
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest noLeavesAvailable(String channelId, String userTag) {
        return SlackMessageBuilder
                .create("ℹ️ No active leave requests found for " + userTag)
                .withHeader("ℹ️ No Active Leaves", true)
                .withSection("You don't have any active leave requests to delete.")
                .toChannel(channelId)
                .build();
    }

    /**
     * Creates an error message for delete-leave command failures.
     *
     * @param channelId    The channel ID to post to
     * @param userTag      The Slack user tag
     * @param errorMessage The error message
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest deleteLeaveError(String channelId, String userTag, String errorMessage) {
        return SlackMessageBuilder
                .create("❌ Failed to process delete leave request for " + userTag)
                .withHeader("❌ Delete Leave Error", true)
                .withSection("User", userTag)
                .withSection("Error", errorMessage != null ? errorMessage : "Unknown error")
                .withDivider()
                .withSection("Please try again or contact HR for assistance.")
                .toChannel(channelId)
                .build();
    }

    /**
     * Creates a success message when a leave is deleted.
     *
     * @param channelId    The channel ID to post to
     * @param userTag      The Slack user tag
     * @param deletedLeave The deleted leave details
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveDeleted(String channelId, String userTag, LeaveDto deletedLeave) {
        String dates = formatLeaveDates(deletedLeave);

        Map<String, String> fields = new HashMap<>();
        fields.put("User", userTag);
        fields.put("Leave ID", deletedLeave.getId().toString());
        fields.put("Type", deletedLeave.getType().toString());
        fields.put("Dates", dates);
        fields.put("Previous Status", deletedLeave.getStatus() == one.june.leave_management.domain.leave.model.LeaveStatus.DEACTIVATED
                ? "APPROVED/REQUESTED" : deletedLeave.getStatus().toString());

        return SlackMessageBuilder
                .create("🗑️ Leave request deleted successfully for " + userTag)
                .withHeader("🗑️ Leave Deleted", true)
                .withFields(fields)
                .toChannel(channelId)
                .build();
    }

    /**
     * Creates a failure message when leave deletion fails.
     *
     * @param channelId    The channel ID to post to
     * @param userTag      The Slack user tag
     * @param errorMessage The error message
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveDeleteFailed(String channelId, String userTag, String errorMessage) {
        return SlackMessageBuilder
                .create("❌ Failed to delete leave for " + userTag)
                .withHeader("❌ Leave Deletion Failed", true)
                .withSection("User", userTag)
                .withSection("Error", errorMessage != null ? errorMessage : "Unknown error")
                .withDivider()
                .withSection("The leave may have already been deleted or you may not have permission.")
                .toChannel(channelId)
                .build();
    }
}
