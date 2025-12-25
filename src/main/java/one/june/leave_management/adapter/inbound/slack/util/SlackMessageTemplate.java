package one.june.leave_management.adapter.inbound.slack.util;

import one.june.leave_management.adapter.outbound.slack.builder.SlackMessageBuilder;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.application.leave.dto.LeaveDto;
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
     * @param channelId The channel ID where the message will be posted
     * @param userTag   The Slack user tag (e.g., "&lt;@U12345&gt;")
     * @return A SlackMessageRequest object ready to be sent
     */
    public static SlackMessageRequest leaveRequestInitiated(String channelId, String userTag) {
        return SlackMessageBuilder
                .create("📝 Leave request initiated for " + userTag)
                .withHeader("📝 Leave Request Initiated", true)
                .withSection(String.format(
                        "*User:* %s\n*Status:* Opening modal...\n\nPlease fill out the leave details in the modal.",
                        userTag
                ))
                .toChannel(channelId)
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
}
