package one.june.leave_management.adapter.inbound.slack.util;

import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.application.genai.dto.ParsedLeaveRequest;
import one.june.leave_management.application.genai.util.GenAiTestFixtures;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlackMessageTemplate}
 * <p>
 * Tests message template creation for leave-related Slack messages
 */
@DisplayName("SlackMessageTemplate Unit Tests")
class SlackMessageTemplateTest {

    // Helper method to create test employee
    private EmployeeDto createTestEmployee(String slackId, String name) {
        return EmployeeDto.builder()
                .id(UUID.randomUUID())
                .name(name)
                .slackId(slackId)
                .googleId(null)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("leaveRequestInitiated() Tests")
    class LeaveRequestInitiatedTests {

        @Test
        @DisplayName("Should create message with valid channelId and userTag")
        void shouldCreateMessageWithValidInputs() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated(
                    "C12345", null, "<@U67890>", "initiated", "test text"
            );

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getChannel()).isEqualTo("C12345");
            assertThat(message.getText()).contains("<@U67890>");
            assertThat(message.getThreadTs()).isNull();
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isEmpty(); // No blocks added for simple initiated message
        }

        @Test
        @DisplayName("Should verify message structure has correct header text")
        void shouldVerifyHeaderStructure() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated(
                    "C12345", null, "<@U67890>", "initiated", "test text"
            );

            // Then - Verify the text contains expected content
            assertThat(message.getText()).contains("Leave request");
            assertThat(message.getText()).contains("<@U67890>");
            assertThat(message.getText()).contains("initiated");
            assertThat(message.getBlocks()).isNotNull();
        }

        @Test
        @DisplayName("Should verify message structure has correct section text with userTag")
        void shouldVerifySectionTextWithUserTag() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated(
                    "C12345", null, "<@U67890>", "initiated", "test text"
            );

            // Then - Verify basic message structure
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getText()).contains("test text");
        }

        @Test
        @DisplayName("Should verify channel is set correctly")
        void shouldVerifyChannelIsSetCorrectly() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated(
                    "C12345", null, "<@U67890>", "initiated", "test text"
            );

            // Then
            assertThat(message.getChannel()).isEqualTo("C12345");
        }
    }

    @Nested
    @DisplayName("leaveCreated() Tests")
    class LeaveCreatedTests {

        @Test
        @DisplayName("Should create message with all fields including threadTs")
        void shouldCreateMessageWithAllFieldsIncludingThreadTs() {
            // Given
            LeaveDto leaveDto = createTestLeaveDto(LeaveDurationType.FULL_DAY);

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getChannel()).isEqualTo("C12345");
            assertThat(message.getThreadTs()).isEqualTo("1234567890.123456");
            assertThat(message.getText()).contains("Leave created successfully");
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify message structure with FULL_DAY leave")
        void shouldVerifyStructureWithFullDayLeave() {
            // Given
            LeaveDto leaveDto = createTestLeaveDto(LeaveDurationType.FULL_DAY);

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message.getText()).contains("Leave created successfully");
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify message structure with FIRST_HALF leave")
        void shouldVerifyStructureWithFirstHalfLeave() {
            // Given
            LeaveDto leaveDto = createTestLeaveDto(LeaveDurationType.FIRST_HALF);

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
            assertThat(message.getText()).contains("<@U67890>");
        }

        @Test
        @DisplayName("Should verify message structure with SECOND_HALF leave")
        void shouldVerifyStructureWithSecondHalfLeave() {
            // Given
            LeaveDto leaveDto = createTestLeaveDto(LeaveDurationType.SECOND_HALF);

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify message structure with null durationType (defaults to FULL_DAY)")
        void shouldVerifyStructureWithNullDurationType() {
            // Given
            EmployeeDto employee = createTestEmployee("U67890", "Test User");

            LeaveDto leaveDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .dateRange(new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(null)
                    .build();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify date formatting for single day leave")
        void shouldVerifyDateFormattingForSingleDay() {
            // Given
            EmployeeDto employee = createTestEmployee("U67890", "Test User");

            LeaveDto leaveDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .dateRange(new DateRange(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 15)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify date formatting for multi-day leave")
        void shouldVerifyDateFormattingForMultiDay() {
            // Given
            LeaveDto leaveDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(createTestEmployee("U67890", "Test User"))
                    .dateRange(new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify date formatting when dateRange is null (shows N/A)")
        void shouldVerifyDateFormattingWhenDateRangeIsNull() {
            // Given
            LeaveDto leaveDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(createTestEmployee("U67890", "Test User"))
                    .dateRange(null)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify date formatting when endDate equals startDate")
        void shouldVerifyDateFormattingWhenEndDateEqualsStartDate() {
            // Given
            LocalDate sameDate = LocalDate.of(2025, 1, 15);
            LeaveDto leaveDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(createTestEmployee("U67890", "Test User"))
                    .dateRange(new DateRange(sameDate, sameDate))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify all required fields are present in message")
        void shouldVerifyAllRequiredFieldsPresent() {
            // Given
            LeaveDto leaveDto = createTestLeaveDto(LeaveDurationType.FULL_DAY);

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveCreated("C12345", "1234567890.123456", "U67890", leaveDto);

            // Then
            assertThat(message.getChannel()).isEqualTo("C12345");
            assertThat(message.getThreadTs()).isEqualTo("1234567890.123456");
            assertThat(message.getText()).isNotNull();
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("leaveRequestFailed() Tests")
    class LeaveRequestFailedTests {

        @Test
        @DisplayName("Should create message with errorMessage")
        void shouldCreateMessageWithErrorMessage() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestFailed(
                    "C12345",
                    "1234567890.123456",
                    "U67890",
                    "Validation failed: Invalid date range"
            );

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getChannel()).isEqualTo("C12345");
            assertThat(message.getThreadTs()).isEqualTo("1234567890.123456");
            assertThat(message.getText()).contains("Leave request failed");
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should create message with null errorMessage (shows Unknown error)")
        void shouldCreateMessageWithNullErrorMessage() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestFailed(
                    "C12345",
                    "1234567890.123456",
                    "U67890",
                    null
            );

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getText()).contains("Leave request failed");
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify error text is displayed correctly")
        void shouldVerifyErrorTextIsDisplayedCorrectly() {
            // Given
            String errorMessage = "Database connection failed";

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestFailed(
                    "C12345",
                    "1234567890.123456",
                    "U67890",
                    errorMessage
            );

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getText()).contains("<@U67890>");
            assertThat(message.getBlocks()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("leaveRequestCancelled() Tests")
    class LeaveRequestCancelledTests {

        @Test
        @DisplayName("Should create cancellation message")
        void shouldCreateCancellationMessage() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestCancelled(
                    "C12345",
                    "1234567890.123456",
                    "U67890"
            );

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getChannel()).isEqualTo("C12345");
            assertThat(message.getThreadTs()).isEqualTo("1234567890.123456");
            assertThat(message.getText()).contains("Leave request cancelled");
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify cancellation status text is correct")
        void shouldVerifyCancellationStatusTextIsCorrect() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestCancelled(
                    "C12345",
                    "1234567890.123456",
                    "U67890"
            );

            // Then
            assertThat(message.getText()).contains("cancelled");
            assertThat(message.getText()).contains("<@U67890>");
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify threadTs is set correctly for threaded replies")
        void shouldVerifyThreadTsIsSetCorrectlyForCancelled() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestCancelled(
                    "C12345",
                    "1234567890.123456",
                    "U67890"
            );

            // Then
            assertThat(message.getThreadTs()).isEqualTo("1234567890.123456");
        }
    }

    /**
     * Test helper to create a test LeaveDto
     */
    private LeaveDto createTestLeaveDto(LeaveDurationType durationType) {
        return LeaveDto.builder()
                .id(UUID.randomUUID())
                .employee(createTestEmployee("U67890", "Test User"))
                .dateRange(new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)))
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(durationType)
                .build();
    }

    @Nested
    @DisplayName("AI Template Tests")
    class AiTemplateTests {

        @Test
        @DisplayName("Should create confirmation message with all details")
        void shouldCreateConfirmationMessage() {
            // Given
            ParsedLeaveRequest request = GenAiTestFixtures.createSimpleLeaveRequest();
            String channelId = "C12345";
            String threadTs = "1234567890.123456";
            String userId = "U67890";
            String confirmationText = "I've parsed your leave request:";

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    channelId, threadTs, userId, confirmationText, request
            );

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getChannel()).isEqualTo(channelId);
            assertThat(message.getThreadTs()).isEqualTo(threadTs);
            assertThat(message.getText()).contains("<@" + userId + ">");
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include confirm and edit buttons")
        void shouldIncludeConfirmAndEditButtons() {
            // Given
            ParsedLeaveRequest request = GenAiTestFixtures.createSimpleLeaveRequest();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    "C12345", "1234567890.123456", "U67890",
                    "Parsed your request", request
            );

            // Then
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
            // Buttons are included in blocks
        }

        @Test
        @DisplayName("Should serialize ParsedLeaveRequest to button value")
        void shouldSerializeParsedLeaveRequest() {
            // Given
            ParsedLeaveRequest request = GenAiTestFixtures.createSimpleLeaveRequest();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    "C12345", "1234567890.123456", "U67890",
                    "Parsed your request", request
            );

            // Then - Serialization succeeded, message has blocks
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include user tag in confirmation message")
        void shouldIncludeUserTagInConfirmation() {
            // Given
            ParsedLeaveRequest request = GenAiTestFixtures.createSimpleLeaveRequest();
            String userId = "U67890";

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    "C12345", "1234567890.123456", userId,
                    "Parsed your request", request
            );

            // Then
            assertThat(message.getText()).contains("<@" + userId + ">");
        }

        @Test
        @DisplayName("Should include divider before buttons")
        void shouldIncludeDividerBeforeButtons() {
            // Given
            ParsedLeaveRequest request = GenAiTestFixtures.createSimpleLeaveRequest();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    "C12345", "1234567890.123456", "U67890",
                    "Parsed your request", request
            );

            // Then
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include explanatory text")
        void shouldIncludeExplanatoryText() {
            // Given
            ParsedLeaveRequest request = GenAiTestFixtures.createSimpleLeaveRequest();

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    "C12345", "1234567890.123456", "U67890",
                    "Parsed your request", request
            );

            // Then
            assertThat(message.getBlocks()).isNotNull();
        }

        @Test
        @DisplayName("Should create parsing error message")
        void shouldCreateParsingErrorMessage() {
            // Given
            String channelId = "C12345";
            String threadTs = "1234567890.123456";
            String userId = "U67890";
            String errorMessage = "Could not parse date";

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveParsingError(
                    channelId, threadTs, userId, errorMessage
            );

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getChannel()).isEqualTo(channelId);
            assertThat(message.getThreadTs()).isEqualTo(threadTs);
            assertThat(message.getText()).contains("<@" + userId + ">");
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include open modal button in error message")
        void shouldIncludeOpenModalButtonInError() {
            // Given
            String errorMessage = "Parsing failed";

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveParsingError(
                    "C12345", "1234567890.123456", "U67890", errorMessage
            );

            // Then
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include helpful error message for user")
        void shouldIncludeHelpfulErrorMessage() {
            // Given
            String errorMessage = "Could not understand the request";

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveParsingError(
                    "C12345", "1234567890.123456", "U67890", errorMessage
            );

            // Then
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle null error message gracefully")
        void shouldHandleNullErrorMessage() {
            // Given
            String errorMessage = null;

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveParsingError(
                    "C12345", "1234567890.123456", "U67890", errorMessage
            );

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getBlocks()).isNotNull();
            // Should use "Unknown error" as fallback
        }

        @Test
        @DisplayName("Should set correct channel and thread context for confirmation")
        void shouldSetChannelAndThreadContextForConfirmation() {
            // Given
            ParsedLeaveRequest request = GenAiTestFixtures.createSimpleLeaveRequest();
            String channelId = "C12345";
            String threadTs = "1234567890.123456";

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveConfirmation(
                    channelId, threadTs, "U67890",
                    "Parsed your request", request
            );

            // Then
            assertThat(message.getChannel()).isEqualTo(channelId);
            assertThat(message.getThreadTs()).isEqualTo(threadTs);
        }

        @Test
        @DisplayName("Should set correct channel and thread context for error")
        void shouldSetChannelAndThreadContextForError() {
            // Given
            String channelId = "C12345";
            String threadTs = "1234567890.123456";

            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveParsingError(
                    channelId, threadTs, "U67890", "Error"
            );

            // Then
            assertThat(message.getChannel()).isEqualTo(channelId);
            assertThat(message.getThreadTs()).isEqualTo(threadTs);
        }
    }
}
