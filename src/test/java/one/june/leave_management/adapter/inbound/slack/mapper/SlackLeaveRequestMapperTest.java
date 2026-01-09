package one.june.leave_management.adapter.inbound.slack.mapper;

import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionValue;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewSubmissionRequest;
import one.june.leave_management.application.leave.service.OptionalHolidayService;
import one.june.leave_management.common.exception.SlackPayloadParseException;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.employee.model.Employee;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import one.june.leave_management.domain.leave.model.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlackLeaveRequestMapper - Employee Lookup Tests")
class SlackLeaveRequestMapperTest {

    @Mock
    private OptionalHolidayService optionalHolidayService;

    @Mock
    private EmployeeRepository employeeRepository;

    private SlackLeaveRequestMapper mapper;

    private UUID testEmployeeId;
    private String testSlackUserId;

    @BeforeEach
    void setUp() {
        mapper = new SlackLeaveRequestMapper(optionalHolidayService, employeeRepository);
        testEmployeeId = UUID.randomUUID();
        testSlackUserId = "U123456";
    }

    @Test
    @DisplayName("Should use Slack user ID directly")
    void shouldUseSlackUserIdDirectly() {
        // Given
        Employee employee = Employee.builder()
                .id(testEmployeeId)
                .slackId(testSlackUserId)
                .build();

        SlackViewSubmissionRequest submission = createValidSubmission();

        when(employeeRepository.findBySlackId(testSlackUserId))
                .thenReturn(Optional.of(employee));

        // When
        var result = mapper.toLeaveIngestionRequest(submission);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testEmployeeId.toString()); // Should store employee UUID, not Slack ID
        assertThat(result.getSourceType()).isEqualTo(SourceType.SLACK);
        assertThat(result.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED);

        verify(employeeRepository).findBySlackId(testSlackUserId);
    }

    @Test
    @DisplayName("Should throw SlackPayloadParseException when employee not found for Slack user ID")
    void shouldThrowExceptionWhenEmployeeNotFound() {
        // Given
        SlackViewSubmissionRequest submission = createValidSubmission();

        when(employeeRepository.findBySlackId(testSlackUserId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> mapper.toLeaveIngestionRequest(submission))
                .isInstanceOf(SlackPayloadParseException.class)
                .hasMessageContaining("Employee not found for Slack user ID")
                .hasMessageContaining(testSlackUserId);

        verify(employeeRepository).findBySlackId(testSlackUserId);
    }

    @Test
    @DisplayName("Should use Slack user ID for optional holiday leave")
    void shouldUseSlackUserIdForOptionalHoliday() {
        // Given
        Employee employee = Employee.builder()
                .id(testEmployeeId)
                .slackId(testSlackUserId)
                .build();

        UUID holidayId = UUID.randomUUID();
        LocalDate holidayDate = LocalDate.of(2024, 12, 25);
        OptionalHoliday holiday = OptionalHoliday.builder()
                .id(holidayId)
                .date(holidayDate)
                .name("Christmas")
                .build();

        SlackViewSubmissionRequest submission = createOptionalHolidaySubmission(holidayId);

        when(employeeRepository.findBySlackId(testSlackUserId))
                .thenReturn(Optional.of(employee));
        when(optionalHolidayService.findById(holidayId))
                .thenReturn(Optional.of(holiday));

        // When
        var result = mapper.toLeaveIngestionRequest(submission);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testEmployeeId.toString()); // Should store employee UUID, not Slack ID
        assertThat(result.getType()).isEqualTo(LeaveType.OPTIONAL_HOLIDAY);
        assertThat(result.getDateRange()).isEqualTo(
                DateRange.builder()
                        .startDate(holidayDate)
                        .endDate(holidayDate)
                        .build()
        );
        assertThat(result.getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);

        verify(employeeRepository).findBySlackId(testSlackUserId);
        verify(optionalHolidayService).findById(holidayId);
    }

    @Test
    @DisplayName("Should propagate holiday not found exception after employee lookup")
    void shouldPropagateHolidayNotFoundException() {
        // Given
        Employee employee = Employee.builder()
                .id(testEmployeeId)
                .slackId(testSlackUserId)
                .build();

        UUID holidayId = UUID.randomUUID();
        SlackViewSubmissionRequest submission = createOptionalHolidaySubmission(holidayId);

        when(employeeRepository.findBySlackId(testSlackUserId))
                .thenReturn(Optional.of(employee));
        when(optionalHolidayService.findById(holidayId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> mapper.toLeaveIngestionRequest(submission))
                .isInstanceOf(SlackPayloadParseException.class)
                .hasMessageContaining("Holiday with ID")
                .hasMessageContaining("not found");

        verify(employeeRepository).findBySlackId(testSlackUserId);
        verify(optionalHolidayService).findById(holidayId);
    }

    // Helper methods

    private SlackViewSubmissionRequest createValidSubmission() {
        Map<String, Map<String, SlackBlockActionValue>> stateValues = new HashMap<>();

        // Leave type block
        Map<String, SlackBlockActionValue> leaveTypeBlock = new HashMap<>();
        leaveTypeBlock.put("leave_type_category_action",
                createSelectedOptionAction("ANNUAL_LEAVE"));
        stateValues.put("leave_type_category_block", leaveTypeBlock);

        // Duration block
        Map<String, SlackBlockActionValue> durationBlock = new HashMap<>();
        durationBlock.put("leave_duration_action",
                createSelectedOptionAction("FULL_DAY"));
        stateValues.put("leave_duration_block", durationBlock);

        // Start date block
        Map<String, SlackBlockActionValue> startDateBlock = new HashMap<>();
        startDateBlock.put("start_date_action",
                createDatePickerAction("2024-01-15"));
        stateValues.put("start_date_block", startDateBlock);

        // End date block
        Map<String, SlackBlockActionValue> endDateBlock = new HashMap<>();
        endDateBlock.put("end_date_action",
                createDatePickerAction("2024-01-15"));
        stateValues.put("end_date_block", endDateBlock);

        // Reason block (optional)
        Map<String, SlackBlockActionValue> reasonBlock = new HashMap<>();
        reasonBlock.put("reason_action",
                createPlainTextAction("Vacation"));
        stateValues.put("reason_block", reasonBlock);

        return createSubmissionRequest(stateValues);
    }

    private SlackViewSubmissionRequest createOptionalHolidaySubmission(UUID holidayId) {
        Map<String, Map<String, SlackBlockActionValue>> stateValues = new HashMap<>();

        // Leave type block
        Map<String, SlackBlockActionValue> leaveTypeBlock = new HashMap<>();
        leaveTypeBlock.put("leave_type_category_action",
                createSelectedOptionAction("OPTIONAL_HOLIDAY"));
        stateValues.put("leave_type_category_block", leaveTypeBlock);

        // Holiday select block
        Map<String, SlackBlockActionValue> holidaySelectBlock = new HashMap<>();
        holidaySelectBlock.put("holiday_select_action",
                createSelectedOptionAction(holidayId.toString()));
        stateValues.put("holiday_select_block", holidaySelectBlock);

        return createSubmissionRequest(stateValues);
    }

    private SlackViewSubmissionRequest createSubmissionRequest(
            Map<String, Map<String, SlackBlockActionValue>> stateValues) {
        SlackViewSubmissionRequest request = new SlackViewSubmissionRequest();

        SlackViewSubmissionRequest.SlackView view = new SlackViewSubmissionRequest.SlackView();
        view.setId("V" + UUID.randomUUID());
        view.setPrivateMetadata("{\"userId\":\"" + testSlackUserId + "\"}");

        one.june.leave_management.adapter.inbound.slack.dto.SlackViewState state =
                new one.june.leave_management.adapter.inbound.slack.dto.SlackViewState();
        state.setValues(stateValues);
        view.setState(state);

        request.setView(view);
        return request;
    }

    private SlackBlockActionValue createSelectedOptionAction(String value) {
        SlackBlockActionValue actionValue = new SlackBlockActionValue();
        SlackBlockActionValue.SlackSelectedOption selectedOption =
                new SlackBlockActionValue.SlackSelectedOption();
        selectedOption.setValue(value);
        actionValue.setSelectedOption(selectedOption);
        return actionValue;
    }

    private SlackBlockActionValue createDatePickerAction(String date) {
        SlackBlockActionValue actionValue = new SlackBlockActionValue();
        actionValue.setSelectedDate(date);
        return actionValue;
    }

    private SlackBlockActionValue createPlainTextAction(String text) {
        SlackBlockActionValue actionValue = new SlackBlockActionValue();
        actionValue.setValue(text);
        return actionValue;
    }
}
