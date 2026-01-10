package one.june.leave_management.application.genai.service;

import one.june.leave_management.adapter.genai.client.GroqApiClient;
import one.june.leave_management.adapter.genai.dto.GroqChatResponse;
import one.june.leave_management.application.genai.dto.ParseResult;
import one.june.leave_management.application.genai.dto.ParsedLeaveRequest;
import one.june.leave_management.application.genai.util.GenAiTestFixtures;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for LeaveParsingService.
 * Tests all parsing scenarios, edge cases, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveParsingService Tests")
class LeaveParsingServiceTest {

    @Mock
    private GroqApiClient groqApiClient;

    @InjectMocks
    private LeaveParsingService leaveParsingService;

    @BeforeEach
    void setUp() {
        // Set the model field manually since @Value doesn't work with MockitoExtension
        ReflectionTestUtils.setField(leaveParsingService, "model", "llama-3.1-8b-instant");
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("Should parse simple leave request successfully")
        void shouldParseSimpleLeaveRequest() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest()).isNotNull();
            assertThat(result.getConfidenceScore()).isGreaterThan(0.5);
            assertThat(result.getParsedRequest().getLeaveType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(result.getParsedRequest().getReason()).isEqualTo("Personal reasons");
            assertThat(result.getModelUsed()).isNotNull();
            assertThat(result.getProcessingTimeMs()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should parse date range leave request successfully")
        void shouldParseDateRangeLeaveRequest() {
            // Arrange
            String message = GenAiTestFixtures.DATE_RANGE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createDateRangeResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getStartDate()).isEqualTo(GenAiTestFixtures.CHRISTMAS_DAY);
            assertThat(result.getParsedRequest().getEndDate()).isEqualTo(GenAiTestFixtures.BOXING_DAY);
            assertThat(result.getParsedRequest().getReason()).isEqualTo("Vacation");
        }

        @Test
        @DisplayName("Should parse half-day leave request successfully")
        void shouldParseHalfDayLeaveRequest() {
            // Arrange
            String message = GenAiTestFixtures.HALF_DAY_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createHalfDayLeaveRequestResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
            assertThat(result.getParsedRequest().getReason()).isEqualTo("Dentist appointment");
        }

        @Test
        @DisplayName("Should parse optional holiday request successfully")
        void shouldParseOptionalHolidayRequest() {
            // Arrange
            String message = GenAiTestFixtures.OPTIONAL_HOLIDAY_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createOptionalHolidayResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getLeaveType()).isEqualTo(LeaveType.OPTIONAL_HOLIDAY);
            assertThat(result.getParsedRequest().isOptionalHoliday()).isTrue();
            assertThat(result.getParsedRequest().getOptionalHolidayName()).isEqualTo("Christmas Eve");
        }
    }

    @Nested
    @DisplayName("Date Parsing Tests")
    class DateParsingTests {

        @Test
        @DisplayName("Should parse ISO date format successfully")
        void shouldParseIsoDateFormat() {
            // Arrange
            String message = GenAiTestFixtures.ISO_DATE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            String jsonContent = String.format("""
                    {
                        "startDate": "2024-01-15",
                        "endDate": "2024-01-15",
                        "durationType": "FULL_DAY",
                        "leaveType": "ANNUAL_LEAVE",
                        "reason": "conference",
                        "isOptionalHoliday": false,
                        "slackUserId": "%s"
                    }
                    """, slackUserId);

            GroqChatResponse mockResponse = GenAiTestFixtures.createSuccessfulResponse(jsonContent);
            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getStartDate()).isEqualTo(GenAiTestFixtures.JAN_15);
        }

        @Test
        @DisplayName("Should default to today when date is missing")
        void shouldDefaultToTodayWhenDateMissing() {
            // Arrange
            String message = "need leave today";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            String jsonContent = String.format("""
                    {
                        "startDate": null,
                        "endDate": null,
                        "leaveType": "ANNUAL_LEAVE",
                        "slackUserId": "%s"
                    }
                    """, slackUserId);

            GroqChatResponse mockResponse = GenAiTestFixtures.createSuccessfulResponse(jsonContent);
            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getStartDate()).isNotNull();
            assertThat(result.getParsedRequest().getEndDate()).isNotNull();
        }

        @Test
        @DisplayName("Should handle end date before start date by swapping")
        void shouldHandleEndDateBeforeStartDate() {
            // Arrange
            String message = "leave from Dec 27 to Dec 25";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            String jsonContent = String.format("""
                    {
                        "startDate": "2024-12-27",
                        "endDate": "2024-12-25",
                        "durationType": "FULL_DAY",
                        "leaveType": "ANNUAL_LEAVE",
                        "reason": "test",
                        "isOptionalHoliday": false,
                        "slackUserId": "%s"
                    }
                    """, slackUserId);

            GroqChatResponse mockResponse = GenAiTestFixtures.createSuccessfulResponse(jsonContent);
            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            // End date should be adjusted to match start date when it's before
            assertThat(result.getParsedRequest().getEndDate())
                    .isAfterOrEqualTo(result.getParsedRequest().getStartDate());
        }

        @Test
        @DisplayName("Should set end date equal to start date for single day")
        void shouldSetEndDateEqualToStartDateForSingleDay() {
            // Arrange
            String message = GenAiTestFixtures.SINGLE_DAY_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            String jsonContent = String.format("""
                    {
                        "startDate": "2024-12-25",
                        "endDate": "2024-12-25",
                        "durationType": "FULL_DAY",
                        "leaveType": "ANNUAL_LEAVE",
                        "reason": "Christmas",
                        "isOptionalHoliday": false,
                        "slackUserId": "%s"
                    }
                    """, slackUserId);

            GroqChatResponse mockResponse = GenAiTestFixtures.createSuccessfulResponse(jsonContent);
            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getStartDate())
                    .isEqualTo(result.getParsedRequest().getEndDate());
        }
    }

    @Nested
    @DisplayName("Default Value Handling Tests")
    class DefaultValueHandlingTests {

        @Test
        @DisplayName("Should use default FULL_DAY when duration type is null")
        void shouldUseDefaultFullDayWhenDurationTypeIsNull() {
            // Arrange
            String message = "need leave tomorrow";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            String jsonContent = String.format("""
                    {
                        "startDate": "2024-12-16",
                        "endDate": "2024-12-16",
                        "leaveType": "ANNUAL_LEAVE",
                        "reason": "test",
                        "isOptionalHoliday": false,
                        "slackUserId": "%s"
                    }
                    """, slackUserId);

            GroqChatResponse mockResponse = GenAiTestFixtures.createSuccessfulResponse(jsonContent);
            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
        }

        @Test
        @DisplayName("Should use default ANNUAL_LEAVE when leave type is null")
        void shouldUseDefaultAnnualLeaveWhenLeaveTypeIsNull() {
            // Arrange
            String message = "need leave tomorrow";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            String jsonContent = String.format("""
                    {
                        "startDate": "2024-12-16",
                        "endDate": "2024-12-16",
                        "durationType": "FULL_DAY",
                        "isOptionalHoliday": false,
                        "slackUserId": "%s"
                    }
                    """, slackUserId);

            GroqChatResponse mockResponse = GenAiTestFixtures.createSuccessfulResponse(jsonContent);
            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getLeaveType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        }

        @Test
        @DisplayName("Should use default empty string for missing reason")
        void shouldUseDefaultEmptyStringForMissingReason() {
            // Arrange
            String message = "need leave tomorrow";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            String jsonContent = String.format("""
                    {
                        "startDate": "2024-12-16",
                        "endDate": "2024-12-16",
                        "durationType": "FULL_DAY",
                        "leaveType": "ANNUAL_LEAVE",
                        "isOptionalHoliday": false,
                        "slackUserId": "%s"
                    }
                    """, slackUserId);

            GroqChatResponse mockResponse = GenAiTestFixtures.createSuccessfulResponse(jsonContent);
            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getReason()).isNotNull();
        }

        @Test
        @DisplayName("Should handle minimal fields successfully")
        void shouldHandleMinimalFieldsSuccessfully() {
            // Arrange
            String message = "leave";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createMinimalFieldsResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest()).isNotNull();
            assertThat(result.getParsedRequest().getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
            assertThat(result.getParsedRequest().getLeaveType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        }
    }

    @Nested
    @DisplayName("Confidence Score Tests")
    class ConfidenceScoreTests {

        @Test
        @DisplayName("Should calculate high confidence score when all fields present")
        void shouldCalculateHighConfidenceScore() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getConfidenceScore()).isGreaterThan(0.8);
            assertThat(result.getConfidenceScore()).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should calculate lower confidence score when minimal fields present")
        void shouldCalculateLowerConfidenceScore() {
            // Arrange
            String message = "leave";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createMinimalFieldsResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getConfidenceScore()).isGreaterThan(0.5);
            assertThat(result.getConfidenceScore()).isLessThan(1.0); // Minimal fields: 0.5 + 0.15 + 0.15 + 0.1 = 0.9
        }

        @Test
        @DisplayName("Should calculate confidence score correctly for partial data")
        void shouldCalculateConfidenceScoreForPartialData() {
            // Arrange
            String message = "leave tomorrow";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            String jsonContent = String.format("""
                    {
                        "startDate": "2024-12-16",
                        "endDate": "2024-12-16",
                        "leaveType": "ANNUAL_LEAVE",
                        "slackUserId": "%s"
                    }
                    """, slackUserId);

            GroqChatResponse mockResponse = GenAiTestFixtures.createSuccessfulResponse(jsonContent);
            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getConfidenceScore()).isGreaterThan(0.5);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return failure when Groq API throws exception")
        void shouldReturnFailureWhenGroqApiThrowsException() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            when(groqApiClient.chat(any(), eq(message)))
                    .thenThrow(new RuntimeException("API Error"));

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isNotNull();
            assertThat(result.getErrorMessage()).contains("Failed to parse AI response");
            assertThat(result.getParsedRequest()).isNull();
        }

        @Test
        @DisplayName("Should return failure when response is empty")
        void shouldReturnFailureWhenResponseIsEmpty() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createEmptyResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isNotNull();
        }

        @Test
        @DisplayName("Should return failure when JSON is malformed")
        void shouldReturnFailureWhenJsonIsMalformed() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createMalformedJsonResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isNotNull();
        }

        @Test
        @DisplayName("Should handle null response gracefully")
        void shouldHandleNullResponse() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            when(groqApiClient.chat(any(), eq(message))).thenReturn(null);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Invalid Enum Handling Tests")
    class InvalidEnumHandlingTests {

        @Test
        @DisplayName("Should default to FULL_DAY for invalid duration type")
        void shouldDefaultToFullDayForInvalidDurationType() {
            // Arrange
            String message = "leave tomorrow";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createInvalidEnumResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue(); // Parsing succeeds with defaults
            assertThat(result.getParsedRequest().getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
        }

        @Test
        @DisplayName("Should default to ANNUAL_LEAVE for invalid leave type")
        void shouldDefaultToAnnualLeaveForInvalidLeaveType() {
            // Arrange
            String message = "leave tomorrow";
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createInvalidEnumResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act & Assert
            // Invalid enums are handled gracefully with default values
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParsedRequest().getLeaveType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        }
    }

    @Nested
    @DisplayName("Processing Time Tests")
    class ProcessingTimeTests {

        @Test
        @DisplayName("Should measure processing time accurately")
        void shouldMeasureProcessingTimeAccurately() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProcessingTimeMs()).isGreaterThan(0);
            assertThat(result.getProcessingTimeMs()).isLessThan(10000); // Should be much less than 10 seconds
        }

        @Test
        @DisplayName("Should record processing time even on failure")
        void shouldRecordProcessingTimeEvenOnFailure() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            when(groqApiClient.chat(any(), eq(message)))
                    .thenThrow(new RuntimeException("API Error"));

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getProcessingTimeMs()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Model Information Tests")
    class ModelInformationTests {

        @Test
        @DisplayName("Should include model used in result")
        void shouldIncludeModelUsedInResult() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getModelUsed()).isNotNull();
            assertThat(result.getModelUsed()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include model used even on failure")
        void shouldIncludeModelUsedEvenOnFailure() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;

            when(groqApiClient.chat(any(), eq(message)))
                    .thenThrow(new RuntimeException("API Error"));

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getModelUsed()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Raw Data Tests")
    class RawDataTests {

        @Test
        @DisplayName("Should include raw data in successful result")
        void shouldIncludeRawDataInSuccessfulResult() {
            // Arrange
            String message = GenAiTestFixtures.SIMPLE_REQUEST;
            String slackUserId = GenAiTestFixtures.TEST_SLACK_USER_ID;
            GroqChatResponse mockResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            when(groqApiClient.chat(any(), eq(message))).thenReturn(mockResponse);

            // Act
            ParseResult result = leaveParsingService.parseLeaveRequest(message, slackUserId);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getRawData()).isNotNull();
            assertThat(result.getRawData()).isNotEmpty();
        }
    }
}
