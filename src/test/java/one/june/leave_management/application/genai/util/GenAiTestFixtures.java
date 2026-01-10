package one.june.leave_management.application.genai.util;

import one.june.leave_management.adapter.genai.dto.GroqChatResponse;
import one.june.leave_management.application.genai.dto.ParseResult;
import one.june.leave_management.application.genai.dto.ParsedLeaveRequest;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveType;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

/**
 * Test fixtures and sample data for GenAI component testing.
 * Provides reusable test objects, sample responses, and utility methods.
 */
public class GenAiTestFixtures {

    // Natural language test inputs
    public static final String SIMPLE_REQUEST = "tomorrow for personal reasons";
    public static final String DATE_RANGE_REQUEST = "Dec 25-27 for vacation";
    public static final String SINGLE_DAY_REQUEST = "Dec 25th for Christmas";
    public static final String TODAY_REQUEST = "today for doctor appointment";
    public static final String TOMORROW_REQUEST = "tomorrow morning for dentist";
    public static final String NEXT_MONDAY_REQUEST = "next Monday for meeting";
    public static final String NEXT_WEEK_REQUEST = "next week for vacation";
    public static final String OPTIONAL_HOLIDAY_REQUEST = "optional holiday on Christmas Eve";
    public static final String HALF_DAY_REQUEST = "tomorrow morning for personal work";
    public static final String WEEKEND_REQUEST = "this Saturday for personal work";
    public static final String INVALID_REQUEST = "gibberish text that makes no sense";
    public static final String EMPTY_REQUEST = "";
    public static final String ISO_DATE_REQUEST = "2024-01-15 for conference";
    public static final String US_DATE_REQUEST = "01/15/2024 for training";
    public static final String EU_DATE_REQUEST = "15/01/2024 for workshop";
    public static final String TEXT_DATE_REQUEST = "Jan 15, 2024 for seminar";

    // Test dates
    public static final LocalDate TODAY = LocalDate.of(2024, Month.DECEMBER, 15);
    public static final LocalDate TOMORROW = TODAY.plusDays(1);
    public static final LocalDate NEXT_MONDAY = TODAY.plusDays(3); // Assuming today is Sunday
    public static final LocalDate CHRISTMAS_EVE = LocalDate.of(2024, Month.DECEMBER, 24);
    public static final LocalDate CHRISTMAS_DAY = LocalDate.of(2024, Month.DECEMBER, 25);
    public static final LocalDate BOXING_DAY = LocalDate.of(2024, Month.DECEMBER, 26);
    public static final LocalDate JAN_15 = LocalDate.of(2024, Month.JANUARY, 15);

    // Test user IDs
    public static final String TEST_SLACK_USER_ID = "U123456";
    public static final String TEST_SLACK_USER_ID_2 = "U789012";

    /**
     * Creates a successful Groq chat response with the given JSON content.
     *
     * @param jsonContent The JSON content to return in the response
     * @return GroqChatResponse with the specified content
     */
    public static GroqChatResponse createSuccessfulResponse(String jsonContent) {
        GroqChatResponse.Message message = GroqChatResponse.Message.builder()
                .role("assistant")
                .content(jsonContent)
                .build();

        GroqChatResponse.Choice choice = GroqChatResponse.Choice.builder()
                .index(0)
                .message(message)
                .finishReason("stop")
                .build();

        GroqChatResponse.Usage usage = GroqChatResponse.Usage.builder()
                .promptTokens(100)
                .completionTokens(50)
                .totalTokens(150)
                .build();

        return GroqChatResponse.builder()
                .id("test-response-id")
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model("llama-3.1-8b-instant")
                .choices(java.util.List.of(choice))
                .usage(usage)
                .build();
    }

    /**
     * Creates a successful Groq response with a complete leave request JSON.
     *
     * @return GroqChatResponse with a complete parsed leave request
     */
    public static GroqChatResponse createCompleteLeaveRequestResponse() {
        String jsonContent = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "durationType": "FULL_DAY",
                    "leaveType": "ANNUAL_LEAVE",
                    "reason": "Personal reasons",
                    "isOptionalHoliday": false,
                    "optionalHolidayName": null,
                    "slackUserId": "%s"
                }
                """, TOMORROW, TOMORROW, TEST_SLACK_USER_ID);

        return createSuccessfulResponse(jsonContent);
    }

    /**
     * Creates a successful Groq response with a date range leave request.
     *
     * @return GroqChatResponse with a date range leave request
     */
    public static GroqChatResponse createDateRangeResponse() {
        String jsonContent = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "durationType": "FULL_DAY",
                    "leaveType": "ANNUAL_LEAVE",
                    "reason": "Vacation",
                    "isOptionalHoliday": false,
                    "optionalHolidayName": null,
                    "slackUserId": "%s"
                }
                """, CHRISTMAS_DAY, BOXING_DAY, TEST_SLACK_USER_ID);

        return createSuccessfulResponse(jsonContent);
    }

    /**
     * Creates a successful Groq response with a half-day leave request.
     *
     * @return GroqChatResponse with a half-day leave request
     */
    public static GroqChatResponse createHalfDayLeaveRequestResponse() {
        String jsonContent = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "durationType": "FIRST_HALF",
                    "leaveType": "ANNUAL_LEAVE",
                    "reason": "Dentist appointment",
                    "isOptionalHoliday": false,
                    "optionalHolidayName": null,
                    "slackUserId": "%s"
                }
                """, TOMORROW, TOMORROW, TEST_SLACK_USER_ID);

        return createSuccessfulResponse(jsonContent);
    }

    /**
     * Creates a successful Groq response with an optional holiday request.
     *
     * @return GroqChatResponse with an optional holiday request
     */
    public static GroqChatResponse createOptionalHolidayResponse() {
        String jsonContent = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "durationType": "FULL_DAY",
                    "leaveType": "OPTIONAL_HOLIDAY",
                    "reason": "Christmas Eve",
                    "isOptionalHoliday": true,
                    "optionalHolidayName": "Christmas Eve",
                    "slackUserId": "%s"
                }
                """, CHRISTMAS_EVE, CHRISTMAS_EVE, TEST_SLACK_USER_ID);

        return createSuccessfulResponse(jsonContent);
    }

    /**
     * Creates a Groq response with minimal fields (missing optional fields).
     *
     * @return GroqChatResponse with minimal fields
     */
    public static GroqChatResponse createMinimalFieldsResponse() {
        String jsonContent = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "leaveType": "ANNUAL_LEAVE",
                    "slackUserId": "%s"
                }
                """, TOMORROW, TOMORROW, TEST_SLACK_USER_ID);

        return createSuccessfulResponse(jsonContent);
    }

    /**
     * Creates an empty Groq response (no content).
     *
     * @return GroqChatResponse with empty content
     */
    public static GroqChatResponse createEmptyResponse() {
        GroqChatResponse.Message message = GroqChatResponse.Message.builder()
                .role("assistant")
                .content("")
                .build();

        GroqChatResponse.Choice choice = GroqChatResponse.Choice.builder()
                .index(0)
                .message(message)
                .finishReason("stop")
                .build();

        return GroqChatResponse.builder()
                .id("test-response-id")
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model("llama-3.1-8b-instant")
                .choices(java.util.List.of(choice))
                .build();
    }

    /**
     * Creates a Groq response with malformed JSON.
     *
     * @return GroqChatResponse with malformed JSON content
     */
    public static GroqChatResponse createMalformedJsonResponse() {
        String malformedJson = "{ invalid json }";

        GroqChatResponse.Message message = GroqChatResponse.Message.builder()
                .role("assistant")
                .content(malformedJson)
                .build();

        GroqChatResponse.Choice choice = GroqChatResponse.Choice.builder()
                .index(0)
                .message(message)
                .finishReason("stop")
                .build();

        return GroqChatResponse.builder()
                .id("test-response-id")
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model("llama-3.1-8b-instant")
                .choices(java.util.List.of(choice))
                .build();
    }

    /**
     * Creates a Groq response with unexpected enum value.
     *
     * @return GroqChatResponse with invalid enum value
     */
    public static GroqChatResponse createInvalidEnumResponse() {
        String jsonContent = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "durationType": "INVALID_TYPE",
                    "leaveType": "INVALID_LEAVE",
                    "reason": "Test",
                    "isOptionalHoliday": false,
                    "optionalHolidayName": null,
                    "slackUserId": "%s"
                }
                """, TOMORROW, TOMORROW, TEST_SLACK_USER_ID);

        return createSuccessfulResponse(jsonContent);
    }

    /**
     * Creates a simple ParsedLeaveRequest with default values.
     *
     * @return ParsedLeaveRequest with default values
     */
    public static ParsedLeaveRequest createSimpleLeaveRequest() {
        return ParsedLeaveRequest.builder()
                .startDate(TOMORROW)
                .endDate(TOMORROW)
                .durationType(LeaveDurationType.FULL_DAY)
                .leaveType(LeaveType.ANNUAL_LEAVE)
                .reason("Personal reasons")
                .isOptionalHoliday(false)
                .slackUserId(TEST_SLACK_USER_ID)
                .build();
    }

    /**
     * Creates a ParsedLeaveRequest for a date range.
     *
     * @return ParsedLeaveRequest with date range
     */
    public static ParsedLeaveRequest createDateRangeLeaveRequest() {
        return ParsedLeaveRequest.builder()
                .startDate(CHRISTMAS_DAY)
                .endDate(BOXING_DAY)
                .durationType(LeaveDurationType.FULL_DAY)
                .leaveType(LeaveType.ANNUAL_LEAVE)
                .reason("Vacation")
                .isOptionalHoliday(false)
                .slackUserId(TEST_SLACK_USER_ID)
                .build();
    }

    /**
     * Creates a ParsedLeaveRequest for a half-day leave.
     *
     * @return ParsedLeaveRequest with half-day duration
     */
    public static ParsedLeaveRequest createHalfDayLeaveRequest() {
        return ParsedLeaveRequest.builder()
                .startDate(TOMORROW)
                .endDate(TOMORROW)
                .durationType(LeaveDurationType.FIRST_HALF)
                .leaveType(LeaveType.ANNUAL_LEAVE)
                .reason("Dentist appointment")
                .isOptionalHoliday(false)
                .slackUserId(TEST_SLACK_USER_ID)
                .build();
    }

    /**
     * Creates a ParsedLeaveRequest for an optional holiday.
     *
     * @return ParsedLeaveRequest for optional holiday
     */
    public static ParsedLeaveRequest createOptionalHolidayRequest() {
        return ParsedLeaveRequest.builder()
                .startDate(CHRISTMAS_EVE)
                .endDate(CHRISTMAS_EVE)
                .durationType(LeaveDurationType.FULL_DAY)
                .leaveType(LeaveType.OPTIONAL_HOLIDAY)
                .reason("Christmas Eve")
                .isOptionalHoliday(true)
                .optionalHolidayName("Christmas Eve")
                .slackUserId(TEST_SLACK_USER_ID)
                .build();
    }

    /**
     * Creates a ParsedLeaveRequest with minimal fields.
     *
     * @return ParsedLeaveRequest with minimal required fields
     */
    public static ParsedLeaveRequest createMinimalLeaveRequest() {
        return ParsedLeaveRequest.builder()
                .startDate(TOMORROW)
                .endDate(TOMORROW)
                .leaveType(LeaveType.ANNUAL_LEAVE)
                .slackUserId(TEST_SLACK_USER_ID)
                .build();
    }

    /**
     * Creates a ParsedLeaveRequest with null values for testing defaults.
     *
     * @return ParsedLeaveRequest with null duration type
     */
    public static ParsedLeaveRequest createLeaveRequestWithNullDefaults() {
        return ParsedLeaveRequest.builder()
                .startDate(TOMORROW)
                .endDate(TOMORROW)
                .durationType(null) // Should default to FULL_DAY
                .leaveType(LeaveType.ANNUAL_LEAVE)
                .slackUserId(TEST_SLACK_USER_ID)
                .build();
    }

    /**
     * Creates a successful ParseResult.
     *
     * @param parsedRequest The parsed leave request
     * @param confidenceScore The confidence score (0.0 to 1.0)
     * @param processingTimeMs Processing time in milliseconds
     * @return ParseResult indicating success
     */
    public static ParseResult createSuccessParseResult(ParsedLeaveRequest parsedRequest,
                                                        double confidenceScore,
                                                        long processingTimeMs) {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("model", "llama-3.1-8b-instant");
        rawData.put("provider", "groq");

        return ParseResult.builder()
                .parsedRequest(parsedRequest)
                .confidenceScore(confidenceScore)
                .isSuccess(true)
                .rawData(rawData)
                .modelUsed("llama-3.1-8b-instant")
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * Creates a successful ParseResult with default confidence score.
     *
     * @param parsedRequest The parsed leave request
     * @return ParseResult indicating success
     */
    public static ParseResult createSuccessParseResult(ParsedLeaveRequest parsedRequest) {
        return createSuccessParseResult(parsedRequest, 0.95, 500L);
    }

    /**
     * Creates a failed ParseResult.
     *
     * @param errorMessage The error message
     * @return ParseResult indicating failure
     */
    public static ParseResult createFailureParseResult(String errorMessage) {
        return ParseResult.builder()
                .confidenceScore(0.0)
                .isSuccess(false)
                .errorMessage(errorMessage)
                .processingTimeMs(300L)
                .build();
    }

    /**
     * Creates a sample raw data map for testing.
     *
     * @return Map with sample raw data
     */
    public static Map<String, Object> createSampleRawData() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("model", "llama-3.1-8b-instant");
        rawData.put("provider", "groq");
        rawData.put("temperature", 0.3);
        rawData.put("maxTokens", 1024);
        return rawData;
    }
}
