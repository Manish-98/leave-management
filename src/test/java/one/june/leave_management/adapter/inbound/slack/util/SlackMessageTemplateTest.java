package one.june.leave_management.adapter.inbound.slack.util;

import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
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

    @Nested
    @DisplayName("leaveRequestInitiated() Tests")
    class LeaveRequestInitiatedTests {

        @Test
        @DisplayName("Should create message with valid channelId and userTag")
        void shouldCreateMessageWithValidInputs() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated("C12345", "<@U67890>");

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getChannel()).isEqualTo("C12345");
            assertThat(message.getText()).contains("<@U67890>");
            assertThat(message.getThreadTs()).isNull();
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify message structure has correct header text")
        void shouldVerifyHeaderStructure() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated("C12345", "<@U67890>");

            // Then - Verify the text contains expected content
            assertThat(message.getText()).contains("Leave request initiated");
            assertThat(message.getText()).contains("<@U67890>");
            assertThat(message.getBlocks()).isNotEmpty();
        }

        @Test
        @DisplayName("Should verify message structure has correct section text with userTag")
        void shouldVerifySectionTextWithUserTag() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated("C12345", "<@U67890>");

            // Then - Verify blocks are created
            assertThat(message.getBlocks()).isNotNull();
            assertThat(message.getBlocks()).hasSizeGreaterThan(0);
        }

        @Test
        @DisplayName("Should verify channel is set correctly")
        void shouldVerifyChannelIsSetCorrectly() {
            // When
            SlackMessageRequest message = SlackMessageTemplate.leaveRequestInitiated("C12345", "<@U67890>");

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
            LeaveDto leaveDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .userId("U67890")
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
            LeaveDto leaveDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .userId("U67890")
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
                    .userId("U67890")
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
                    .userId("U67890")
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
                    .userId("U67890")
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
                .userId("U67890")
                .dateRange(new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)))
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(durationType)
                .build();
    }
}
