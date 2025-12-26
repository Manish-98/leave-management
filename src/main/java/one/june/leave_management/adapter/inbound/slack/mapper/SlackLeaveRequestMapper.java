package one.june.leave_management.adapter.inbound.slack.mapper;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionValue;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewSubmissionRequest;
import one.june.leave_management.adapter.inbound.slack.util.SlackMetadataUtil;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.common.exception.SlackPayloadParseException;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Mapper for converting Slack view submission requests to leave ingestion requests
 * <p>
 * This component handles the transformation of data from Slack's modal form structure
 * to the domain's leave request structure.
 * <p>
 * The mapping extracts:
 * - userId from private_metadata (Slack user ID)
 * - leave type category (ANNUAL_LEAVE or OPTIONAL_HOLIDAY)
 * - leave duration (FULL_DAY, FIRST_HALF, or SECOND_HALF)
 * - start date
 * - end date (optional - defaults to start date if not provided)
 * - reason (optional)
 */
@Slf4j
@Component
public class SlackLeaveRequestMapper {

    private static final String BLOCK_LEAVE_TYPE = "leave_type_category_block";
    private static final String ACTION_LEAVE_TYPE = "leave_type_category_action";
    private static final String BLOCK_DURATION = "leave_duration_block";
    private static final String ACTION_DURATION = "leave_duration_action";
    private static final String BLOCK_START_DATE = "start_date_block";
    private static final String ACTION_START_DATE = "start_date_action";
    private static final String BLOCK_END_DATE = "end_date_block";
    private static final String ACTION_END_DATE = "end_date_action";
    private static final String BLOCK_REASON = "reason_block";
    private static final String ACTION_REASON = "reason_action";

    /**
     * Maps a Slack view submission request to a leave ingestion request
     * <p>
     * Extracts form values from the view state and maps them to the leave request structure:
     * <ul>
     *   <li>sourceType: SLACK</li>
     *   <li>sourceId: Set to view.id (will be overridden in LeaveService call)</li>
     *   <li>userId: From private_metadata (Slack user ID)</li>
     *   <li>dateRange: From start_date and end_date fields</li>
     *   <li>type: From leave_type_category_action (ANNUAL_LEAVE or OPTIONAL_HOLIDAY)</li>
     *   <li>status: APPROVED (Slack requests are pre-approved)</li>
     *   <li>durationType: From leave_duration_action (FULL_DAY, FIRST_HALF, or SECOND_HALF)</li>
     * </ul>
     *
     * @param submission The Slack view submission request
     * @return Mapped LeaveIngestionRequest
     * @throws SlackPayloadParseException if required fields are missing or invalid
     */
    public static LeaveIngestionRequest toLeaveIngestionRequest(SlackViewSubmissionRequest submission) {
        var view = submission.getView();
        var stateValues = view.getState().getValues();

        // Extract userId from private_metadata (JSON format)
        String userId = SlackMetadataUtil.extractUserId(view.getPrivateMetadata());
        log.debug("Extracted userId from metadata: {}", userId);

        // Extract leave type category (ANNUAL_LEAVE or OPTIONAL_HOLIDAY)
        String leaveTypeValue = getSelectedValue(stateValues, BLOCK_LEAVE_TYPE, ACTION_LEAVE_TYPE);
        LeaveType leaveType = LeaveType.valueOf(leaveTypeValue);
        log.debug("Extracted leave type: {}", leaveType);

        // Extract leave duration (FULL_DAY, FIRST_HALF, or SECOND_HALF)
        String durationValue = getSelectedValue(stateValues, BLOCK_DURATION, ACTION_DURATION);
        LeaveDurationType durationType = LeaveDurationType.valueOf(durationValue);
        log.debug("Extracted duration type: {}", durationType);

        // Extract start date
        String startDateStr = getSelectedDate(stateValues, BLOCK_START_DATE, ACTION_START_DATE);
        LocalDate startDate = LocalDate.parse(startDateStr);
        log.debug("Extracted start date: {}", startDate);

        // Extract end date (optional - if not provided, use start date)
        String endDateStr = getSelectedDate(stateValues, BLOCK_END_DATE, ACTION_END_DATE);
        LocalDate endDate = (endDateStr != null) ? LocalDate.parse(endDateStr) : startDate;
        log.debug("Extracted end date: {}", endDate);

        // Extract reason (optional)
        String reason = getTextValue(stateValues, BLOCK_REASON, ACTION_REASON);
        log.debug("Extracted reason: {}", reason);

        return LeaveIngestionRequest.builder()
                .sourceType(SourceType.SLACK)
                .sourceId(view.getId()) // Will use view ID as source ID
                .userId(userId)
                .dateRange(DateRange.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .type(leaveType)
                .status(LeaveStatus.APPROVED)
                .durationType(durationType)
                .build();
    }

    /**
     * Helper method to extract selected value from radio buttons or select
     *
     * @param stateValues The state values map
     * @param blockId     The block ID
     * @param actionId    The action ID
     * @return The selected value
     * @throws SlackPayloadParseException if the value is missing
     */
    private static String getSelectedValue(
            Map<String, Map<String, SlackBlockActionValue>> stateValues,
            String blockId,
            String actionId
    ) {
        if (!stateValues.containsKey(blockId)) {
            throw new SlackPayloadParseException(
                    String.format("Missing block: %s in form state", blockId));
        }

        var blockActions = stateValues.get(blockId);
        if (!blockActions.containsKey(actionId)) {
            throw new SlackPayloadParseException(
                    String.format("Missing action: %s in block: %s", actionId, blockId));
        }

        SlackBlockActionValue actionValue = blockActions.get(actionId);
        if (actionValue == null || actionValue.getSelectedOption() == null) {
            throw new SlackPayloadParseException(
                    String.format("Missing selected option for block: %s, action: %s", blockId, actionId));
        }

        return actionValue.getSelectedOption().getValue();
    }

    /**
     * Helper method to extract selected date from date picker
     *
     * @param stateValues The state values map
     * @param blockId     The block ID
     * @param actionId    The action ID
     * @return The selected date string (or null if not provided)
     */
    private static String getSelectedDate(
            Map<String, Map<String, SlackBlockActionValue>> stateValues,
            String blockId,
            String actionId
    ) {
        if (!stateValues.containsKey(blockId)) {
            return null;
        }

        SlackBlockActionValue actionValue = stateValues.get(blockId).get(actionId);
        if (actionValue == null || actionValue.getSelectedDate() == null) {
            return null;
        }

        return actionValue.getSelectedDate();
    }

    /**
     * Helper method to extract text value from plain text input
     *
     * @param stateValues The state values map
     * @param blockId     The block ID
     * @param actionId    The action ID
     * @return The text value (or null if not provided)
     */
    private static String getTextValue(
            Map<String, Map<String, SlackBlockActionValue>> stateValues,
            String blockId,
            String actionId
    ) {
        if (!stateValues.containsKey(blockId)) {
            return null;
        }

        SlackBlockActionValue actionValue = stateValues.get(blockId).get(actionId);
        if (actionValue == null || actionValue.getValue() == null) {
            return null;
        }

        return actionValue.getValue();
    }
}
