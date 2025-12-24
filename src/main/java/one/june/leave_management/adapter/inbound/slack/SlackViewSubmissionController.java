package one.june.leave_management.adapter.inbound.slack;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionValue;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewSubmissionRequest;
import one.june.leave_management.adapter.inbound.slack.util.SlackRequestSignatureVerifier;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveIngestionService;
import one.june.leave_management.common.exception.SlackSignatureVerificationException;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for handling Slack view submissions
 * Handles modal submissions when users submit the leave application form
 */
@Slf4j
@RestController
@RequestMapping("/integrations/slack")
@ConditionalOnProperty(name = "slack.enabled", havingValue = "true", matchIfMissing = true)
public class SlackViewSubmissionController {

    private final SlackRequestSignatureVerifier signatureVerifier;
    private final LeaveIngestionService leaveIngestionService;
    private final LeaveMapper leaveMapper;

    public SlackViewSubmissionController(
            SlackRequestSignatureVerifier signatureVerifier,
            LeaveIngestionService leaveIngestionService,
            LeaveMapper leaveMapper
    ) {
        this.signatureVerifier = signatureVerifier;
        this.leaveIngestionService = leaveIngestionService;
        this.leaveMapper = leaveMapper;
    }

    /**
     * Handles Slack view submissions from modal submits
     *
     * Process:
     * 1. Read request body
     * 2. Verify Slack signature to ensure request is from Slack
     * 3. Parse view submission payload
     * 4. Extract form values from view.state.values
     * 5. Map to LeaveIngestionRequest
     * 6. Create leave via LeaveIngestionService
     * 7. Return empty response (Slack closes modal automatically)
     *
     * @param request The HTTP request from Slack
     * @param rawBody The raw request body for signature verification
     * @return ResponseEntity with empty body (Slack expects empty response for view_submission)
     */
    @PostMapping("/views")
    public ResponseEntity<?> handleViewSubmission(
            HttpServletRequest request,
            @RequestBody byte[] rawBody
    ) {
        try {
            String requestBody = new String(rawBody, StandardCharsets.UTF_8);
            log.info("Received Slack view submission");

            // Extract headers for signature verification
            String signature = request.getHeader("X-Slack-Signature");
            String timestamp = request.getHeader("X-Slack-Request-Timestamp");
            signatureVerifier.verify(signature, timestamp, requestBody);

            // Parse the view submission payload
            SlackViewSubmissionRequest submissionRequest = parseViewSubmissionRequest(requestBody);
            log.info("View submission from user: {}, view ID: {}",
                    submissionRequest.getUser().getId(),
                    submissionRequest.getView().getId());

            // Extract form values and map to LeaveIngestionRequest
            LeaveIngestionRequest leaveRequest = mapToLeaveIngestionRequest(submissionRequest);
            log.info("Mapped to leave request: {}", leaveRequest);

            // Create the leave
            LeaveIngestionCommand command = leaveMapper.toCommand(
                    leaveRequest,
                    SourceType.SLACK,
                    submissionRequest.getView().getId()
            );

            LeaveDto result = leaveIngestionService.ingest(command);
            log.info("Successfully created leave with ID: {} from Slack submission",
                    result.getId());

            // Return empty response - Slack closes the modal automatically
            return ResponseEntity.ok().build();

        } catch (SlackSignatureVerificationException e) {
            log.error("Invalid Slack signature", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error processing Slack view submission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Parses the view submission request from form-encoded payload
     * <p>
     * Slack sends view submissions as application/x-www-form-urlencoded
     * with the JSON in a "payload" parameter. Format: payload={json_string}
     *
     * @param requestBody The raw request body string
     * @return Parsed Slack view submission request
     */
    private SlackViewSubmissionRequest parseViewSubmissionRequest(String requestBody) {
        try {
            // Parse form-encoded payload (format: payload={json})
            Map<String, String> formPayload = Arrays.stream(requestBody.split("&"))
                    .map(param -> param.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                            parts -> parts[0],
                            parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    ));

            String payloadJson = formPayload.get("payload");
            if (payloadJson == null || payloadJson.isEmpty()) {
                throw new RuntimeException("Missing payload parameter in request");
            }

            log.debug("Parsed view submission payload: {}", payloadJson);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(payloadJson, SlackViewSubmissionRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse view submission request. Request body: {}", requestBody, e);
            throw new RuntimeException("Failed to parse view submission request", e);
        }
    }

    /**
     * Maps Slack view submission to LeaveIngestionRequest
     * <p>
     * Extracts form values from the view state and maps them to the leave request structure:
     * - sourceType: SLACK
     * - sourceId: Set to view.id (overridden in LeaveIngestionService call)
     * - userId: From private_metadata (Slack user ID)
     * - dateRange: From start_date and end_date fields
     * - type: From leave_type_category_action (ANNUAL_LEAVE or OPTIONAL_HOLIDAY)
     * - status: PENDING
     * - durationType: From leave_duration_action (FULL_DAY, FIRST_HALF, or SECOND_HALF)
     *
     * @param submission The Slack view submission request
     * @return Mapped LeaveIngestionRequest
     */
    private LeaveIngestionRequest mapToLeaveIngestionRequest(SlackViewSubmissionRequest submission) {
        var view = submission.getView();
        var stateValues = view.getState().getValues();

        // Extract leave type category (ANNUAL_LEAVE or OPTIONAL_HOLIDAY)
        String leaveTypeValue = getSelectedValue(stateValues, "leave_type_category_block", "leave_type_category_action");
        LeaveType leaveType = LeaveType.valueOf(leaveTypeValue);

        // Extract leave duration (FULL_DAY, FIRST_HALF, or SECOND_HALF)
        String durationValue = getSelectedValue(stateValues, "leave_duration_block", "leave_duration_action");
        LeaveDurationType durationType = LeaveDurationType.valueOf(durationValue);

        // Extract start date
        String startDateStr = getSelectedDate(stateValues, "start_date_block", "start_date_action");
        LocalDate startDate = LocalDate.parse(startDateStr);

        // Extract end date (optional - if not provided, use start date)
        String endDateStr = getSelectedDate(stateValues, "end_date_block", "end_date_action");
        LocalDate endDate = (endDateStr != null) ? LocalDate.parse(endDateStr) : startDate;

        // Extract reason (optional)
        String reason = getTextValue(stateValues, "reason_block", "reason_action");

        return LeaveIngestionRequest.builder()
                .sourceType(SourceType.SLACK)
                .sourceId(view.getId()) // Will use view ID as source ID
                .userId(view.getPrivateMetadata()) // Slack user ID stored in private_metadata
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
     */
    private String getSelectedValue(
            Map<String, Map<String, SlackBlockActionValue>> stateValues,
            String blockId,
            String actionId
    ) {
        SlackBlockActionValue actionValue = stateValues.get(blockId).get(actionId);
        if (actionValue == null || actionValue.getSelectedOption() == null) {
            throw new IllegalArgumentException("Missing value for block: " + blockId + ", action: " + actionId);
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
    private String getSelectedDate(
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
    private String getTextValue(
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
