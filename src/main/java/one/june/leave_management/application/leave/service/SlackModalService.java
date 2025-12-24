package one.june.leave_management.application.leave.service;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.adapter.outbound.slack.builder.SlackBlockBuilder;
import one.june.leave_management.adapter.outbound.slack.builder.SlackModalBuilder;
import one.june.leave_management.adapter.outbound.slack.client.SlackApiClient;
import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for handling Slack modal operations
 * Responsible for building and opening modals in Slack for leave application
 */
@Slf4j
@Service
public class SlackModalService {

    private final SlackApiClient slackApiClient;

    public SlackModalService(SlackApiClient slackApiClient) {
        this.slackApiClient = slackApiClient;
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
     */
    @Async
    public void openLeaveApplicationModalAsync(SlackCommandRequest slackRequest) {
        try {
            log.info("Building and opening leave application modal for user: {}", slackRequest.getUserId());

            SlackModalView modalView = buildLeaveApplicationModal(slackRequest);

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
     * - Leave type (Full day / Half day)
     * - Start date
     * - End date
     * - Reason (optional)
     * <p>
     * This structure follows Slack's Block Kit format for modals.
     * Reference: <a href="https://api.slack.com/block-kit/building">...</a>
     *
     * @param slackRequest The Slack command request containing user context
     * @return A configured SlackModalView for leave application
     */
    private SlackModalView buildLeaveApplicationModal(SlackCommandRequest slackRequest) {
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

        return SlackModalBuilder.create("Apply for Leave", "leave_application_submit")
                .withBlocks(blocks)
                .withPrivateMetadata(slackRequest.getUserId())
                .build();
    }
}
