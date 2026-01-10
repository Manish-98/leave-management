package one.june.leave_management.application.genai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.genai.client.GroqApiClient;
import one.june.leave_management.adapter.genai.dto.GroqChatResponse;
import one.june.leave_management.application.genai.dto.ParseResult;
import one.june.leave_management.application.genai.dto.ParsedLeaveRequest;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for parsing natural language leave requests using GenAI.
 * Leverages Groq API with Llama models for fast, accurate parsing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveParsingService {

    private final GroqApiClient groqApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String model;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy")
    };

    /**
     * Parse a natural language leave request message.
     *
     * @param message Natural language message from user
     * @param slackUserId Slack user ID of the user making the request
     * @return ParseResult containing parsed leave request and metadata
     */
    public ParseResult parseLeaveRequest(String message, String slackUserId) {
        log.debug("Parsing leave request from message: {} for slack user: {}", message, slackUserId);

        long startTime = System.currentTimeMillis();

        try {
            GroqChatResponse response = groqApiClient.chat(buildSystemPrompt(slackUserId), message);

            String content = extractContent(response);
            log.info("Received response from Groq: {}", content);

            Map<String, Object> parsedData = objectMapper.readValue(content, Map.class);
            ParsedLeaveRequest request = mapToParsedLeaveRequest(parsedData);
            log.info("Parsed request: {}", request);

            double confidenceScore = calculateConfidenceScore(parsedData);

            return ParseResult.builder()
                    .parsedRequest(request)
                    .confidenceScore(confidenceScore)
                    .isSuccess(true)
                    .rawData(parsedData)
                    .modelUsed(model)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing leave request", e);
            return ParseResult.builder()
                    .isSuccess(false)
                    .errorMessage("Failed to parse AI response: " + e.getMessage())
                    .modelUsed(model)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Build the system prompt for the AI model.
     * Defines the parsing task and expected output format.
     */
    private String buildSystemPrompt(String slackUserId) {
        return """
                You are a professional leave request parser for an HR management system.
                Your task is to extract structured leave information from natural language messages.

                IMPORTANT: You must respond ONLY with valid JSON. No additional text or explanations.

                Extract the following fields from the user's message:
                - startDate: The start date in YYYY-MM-DD format. If not specified, use today's date.
                - endDate: The end date in YYYY-MM-DD format. If not specified or it's a single day, use the same as startDate.
                - durationType: One of: "FULL_DAY", "FIRST_HALF", "SECOND_HALF". Default to "FULL_DAY" if unclear.
                - leaveType: One of: "ANNUAL_LEAVE", "OPTIONAL_HOLIDAY". Default to "ANNUAL_LEAVE".
                - reason: The reason for leave as a string. If not provided, use empty string.
                - isOptionalHoliday: Boolean, true if this is an optional holiday request.
                - optionalHolidayName: The name of the optional holiday if applicable.
                - slackUserId: The Slack user ID making this request. Always include this in your response as: "%s"

                Rules for date parsing:
                - "today", "now" → today's date
                - "tomorrow" → tomorrow's date
                - "next Monday" or specific day names → calculate the date
                - "Dec 25", "December 25th", "25 December" → current year date
                - If only start date is mentioned, treat as single day leave
                - If date range is mentioned (e.g., "Dec 25-27"), set both dates

                Workweek and business-day rules:
                - The workweek is Monday to Friday.
                - Saturday and Sunday are non-working days and must NOT be used as leave start or end dates unless explicitly stated.
                - If a calculated date falls on a weekend, shift it to the next Monday.
                - The phrase "next week" always refers to the next WORK week.
                - "Next week" starts on the upcoming Monday following the current date.
                - "This week" refers to the current workweek (Monday–Friday).
                - If "next week" is mentioned without specific dates, the leave period is Monday to Friday of the next workweek.

                Examples:
                Input: "I'll be on leave tomorrow for personal commitments"
                Output: {
                    "startDate": "2024-01-10",
                    "endDate": "2024-01-10",
                    "durationType": "FULL_DAY",
                    "leaveType": "ANNUAL_LEAVE",
                    "reason": "personal commitments",
                    "isOptionalHoliday": false,
                    "optionalHolidayName": "",
                    "slackUserId": "%s"
                }

                Input: "Need to take optional holiday on Christmas Eve"
                Output: {
                    "startDate": "2024-12-24",
                    "endDate": "2024-12-24",
                    "durationType": "FULL_DAY",
                    "leaveType": "OPTIONAL_HOLIDAY",
                    "reason": "optional holiday",
                    "isOptionalHoliday": true,
                    "optionalHolidayName": "Christmas Eve",
                    "slackUserId": "%s"
                }

                Current date: %s
                Current day: %s

                Respond with ONLY the JSON object, nothing else.
                """.formatted(slackUserId, slackUserId, slackUserId, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.now().getDayOfWeek().name());
    }

    /**
     * Extract content from Groq response.
     */
    private String extractContent(GroqChatResponse response) {
        if (response != null &&
            response.getChoices() != null &&
            !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }
        throw new IllegalStateException("Empty response from Groq API");
    }

    /**
     * Map raw parsed data to ParsedLeaveRequest with validation and defaults.
     */
    private ParsedLeaveRequest mapToParsedLeaveRequest(Map<String, Object> data) {
        String startDateStr = (String) data.get("startDate");
        String endDateStr = (String) data.get("endDate");
        String durationTypeStr = (String) data.getOrDefault("durationType", "FULL_DAY");
        String leaveTypeStr = (String) data.getOrDefault("leaveType", "ANNUAL_LEAVE");
        String reason = (String) data.getOrDefault("reason", "");
        Boolean isOptionalHoliday = (Boolean) data.getOrDefault("isOptionalHoliday", false);
        String optionalHolidayName = (String) data.getOrDefault("optionalHolidayName", "");
        String slackUserId = (String) data.get("slackUserId");

        LocalDate startDate = parseDateWithDefaults(startDateStr);
        LocalDate endDate = parseDateWithDefaults(endDateStr != null ? endDateStr : startDateStr);

        // Ensure end date is not before start date
        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }

        LeaveDurationType durationType = parseDurationType(durationTypeStr);
        LeaveType leaveType = parseLeaveType(leaveTypeStr);

        return ParsedLeaveRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .durationType(durationType)
                .leaveType(leaveType)
                .reason(reason)
                .isOptionalHoliday(isOptionalHoliday)
                .optionalHolidayName(optionalHolidayName)
                .slackUserId(slackUserId)
                .build();
    }

    /**
     * Parse date string with intelligent defaults.
     */
    private LocalDate parseDateWithDefaults(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }

        // Try standard ISO format first
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            // Try other formats
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    return LocalDate.parse(dateStr, formatter);
                } catch (DateTimeParseException ignored) {
                    // Continue to next formatter
                }
            }
        }

        // If all parsing fails, default to today
        log.warn("Failed to parse date: {}, defaulting to today", dateStr);
        return LocalDate.now();
    }

    /**
     * Parse duration type with validation.
     */
    private LeaveDurationType parseDurationType(String durationTypeStr) {
        if (durationTypeStr == null) {
            return LeaveDurationType.FULL_DAY;
        }

        try {
            return LeaveDurationType.valueOf(durationTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid duration type: {}, defaulting to FULL_DAY", durationTypeStr);
            return LeaveDurationType.FULL_DAY;
        }
    }

    /**
     * Parse leave type with validation.
     */
    private LeaveType parseLeaveType(String leaveTypeStr) {
        if (leaveTypeStr == null) {
            return LeaveType.ANNUAL_LEAVE;
        }

        try {
            return LeaveType.valueOf(leaveTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid leave type: {}, defaulting to ANNUAL_LEAVE", leaveTypeStr);
            return LeaveType.ANNUAL_LEAVE;
        }
    }

    /**
     * Calculate confidence score based on extracted fields.
     * Higher score when more specific information is provided.
     */
    private double calculateConfidenceScore(Map<String, Object> data) {
        double score = 0.5; // Base score

        // Increment score for each well-formed field
        if (data.containsKey("startDate") && data.get("startDate") != null) score += 0.15;
        if (data.containsKey("endDate") && data.get("endDate") != null) score += 0.15;
        if (data.containsKey("durationType")) score += 0.1;
        if (data.containsKey("leaveType")) score += 0.1;
        if (data.containsKey("reason") && data.get("reason") != null && !data.get("reason").toString().isBlank()) {
            score += 0.1;
        }

        return Math.min(score, 1.0);
    }
}
