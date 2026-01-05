package one.june.leave_management.domain.leave.validation.impl;

import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.config.LeaveProperties;
import one.june.leave_management.domain.employee.model.Employee;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.OptionalHolidayRepository;
import one.june.leave_management.domain.leave.validation.LeaveValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("OptionalHolidayValidationStrategy - Max Optional Holidays Validation Tests")
class OptionalHolidayValidationStrategyTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private OptionalHolidayRepository optionalHolidayRepository;

    @Mock
    private LeaveProperties leaveProperties;

    private OptionalHolidayValidationStrategy strategy;

    private UUID validEmployeeId;
    private LocalDate optionalHolidayDate;
    private LocalDate futureOptionalHolidayDate;

    @BeforeEach
    void setUp() {
        // Default max is 2 for most tests
        lenient().when(leaveProperties.getMaxOptionalHolidaysPerYear()).thenReturn(2);

        strategy = new OptionalHolidayValidationStrategy(
                employeeRepository,
                leaveRepository,
                optionalHolidayRepository,
                leaveProperties
        );

        validEmployeeId = UUID.randomUUID();
        optionalHolidayDate = LocalDate.of(2024, 6, 15);
        futureOptionalHolidayDate = LocalDate.of(2024, 12, 24);
    }

    @Test
    @DisplayName("Should approve optional holiday when user has no existing optional holidays")
    void shouldApproveWhenNoExistingOptionalHolidays() {
        // Given
        Leave leave = createApprovedLeave(optionalHolidayDate);

        when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(optionalHolidayDate)));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(0L);
        when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should approve optional holiday when user has one existing optional holiday")
    void shouldApproveWhenOneExistingOptionalHoliday() {
        // Given
        Leave leave = createApprovedLeave(futureOptionalHolidayDate);

        when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        when(optionalHolidayRepository.findAll()).thenReturn(
                List.of(
                        createOptionalHoliday(optionalHolidayDate),
                        createOptionalHoliday(futureOptionalHolidayDate)
                )
        );
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(1L);
        when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should approve optional holiday when user has exactly two existing optional holidays and is updating one")
    void shouldApproveUpdateWhenTwoExistingOptionalHolidays() {
        // Given
        Leave existingLeave = createApprovedLeave(optionalHolidayDate);
        existingLeave.setId(UUID.randomUUID()); // Simulating an update

        when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(optionalHolidayDate)));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(2L);
        when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class), any(UUID.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(existingLeave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should reject optional holiday when user has two existing optional holidays")
    void shouldRejectWhenTwoExistingOptionalHolidays() {
        // Given
        Leave leave = createApprovedLeave(futureOptionalHolidayDate);
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2023, 1, 1)); // Previous year joiner

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(
                List.of(
                        createOptionalHoliday(optionalHolidayDate),
                        createOptionalHoliday(futureOptionalHolidayDate)
                )
        );
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(2L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .contains("has already used 2 optional holiday(s)")
                .contains("Maximum allowed based on joining date is 2");
    }

    @Test
    @DisplayName("Should reject optional holiday when user exceeds max optional holidays")
    void shouldRejectWhenExceedsMaxOptionalHolidays() {
        // Given
        Leave leave = createApprovedLeave(futureOptionalHolidayDate);
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2023, 5, 10)); // Previous year joiner

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(futureOptionalHolidayDate)));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(3L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .contains("has already used 3 optional holiday(s)")
                .contains("Maximum allowed based on joining date is 2");
    }

    @Test
    @DisplayName("Should allow REQUESTED status optional holiday regardless of count")
    void shouldAllowRequestedStatusRegardlessOfCount() {
        // Given
        Leave leave = createLeave(futureOptionalHolidayDate, LeaveStatus.REQUESTED);

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(futureOptionalHolidayDate)));
        // Simulate user already has 2 approved optional holidays
        lenient().when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(2L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should allow CANCELLED status optional holiday regardless of count")
    void shouldAllowCancelledStatusRegardlessOfCount() {
        // Given
        Leave leave = createLeave(futureOptionalHolidayDate, LeaveStatus.CANCELLED);

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(futureOptionalHolidayDate)));
        // Simulate user already has 2 approved optional holidays
        lenient().when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(2L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should allow optional holidays in different years")
    void shouldAllowOptionalHolidaysInDifferentYears() {
        // Given
        Leave leave2024 = createApprovedLeaveForYear(LocalDate.of(2024, 12, 24));

        when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 12, 24))));
        // User has 2 optional holidays in 2023, but 0 in 2024
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(0L);
        when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave2024);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should validate correctly for leap year dates")
    void shouldValidateCorrectlyForLeapYearDates() {
        // Given
        Leave leapYearLeave = createApprovedLeaveForYear(LocalDate.of(2024, 2, 29)); // 2024 is a leap year

        when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 2, 29))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(0L);
        when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leapYearLeave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should return correct leave type")
    void shouldReturnCorrectLeaveType() {
        // When
        LeaveType type = strategy.getType();

        // Then
        assertThat(type).isEqualTo(LeaveType.OPTIONAL_HOLIDAY);
    }

    @Test
    @DisplayName("Should respect configured max limit of 3")
    void shouldRespectConfiguredMaxLimitOfThree() {
        // Given - Configure max to 3
        when(leaveProperties.getMaxOptionalHolidaysPerYear()).thenReturn(3);
        strategy = new OptionalHolidayValidationStrategy(
                employeeRepository,
                leaveRepository,
                optionalHolidayRepository,
                leaveProperties
        );

        Leave leave = createApprovedLeave(futureOptionalHolidayDate);

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(futureOptionalHolidayDate)));
        // User has 2 approved holidays, should be allowed to add a 3rd
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(2L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should reject when exceeding configured max limit of 1")
    void shouldRejectWhenExceedingConfiguredMaxLimitOfOne() {
        // Given - Configure max to 1
        when(leaveProperties.getMaxOptionalHolidaysPerYear()).thenReturn(1);
        strategy = new OptionalHolidayValidationStrategy(
                employeeRepository,
                leaveRepository,
                optionalHolidayRepository,
                leaveProperties
        );

        Leave leave = createApprovedLeave(futureOptionalHolidayDate);
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2023, 3, 15)); // Previous year joiner

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(futureOptionalHolidayDate)));
        // User has 1 approved holiday, should not be allowed to add a 2nd
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(1L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .contains("has already used 1 optional holiday(s)")
                .contains("Maximum allowed based on joining date is 1");
    }

    @Test
    @DisplayName("Should use configured max in error message")
    void shouldUseConfiguredMaxInErrorMessage() {
        // Given - Configure max to 5
        when(leaveProperties.getMaxOptionalHolidaysPerYear()).thenReturn(5);
        strategy = new OptionalHolidayValidationStrategy(
                employeeRepository,
                leaveRepository,
                optionalHolidayRepository,
                leaveProperties
        );

        Leave leave = createApprovedLeave(futureOptionalHolidayDate);
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2023, 2, 20)); // Previous year joiner

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(futureOptionalHolidayDate)));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(5L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .contains("Maximum allowed based on joining date is 5");
    }

    // Proration Tests based on Date of Joining

    @Test
    @DisplayName("Should allow full max for employee who joined in previous year")
    void shouldAllowFullMaxForPreviousYearJoiner() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 6, 15));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2023, 3, 15));

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 6, 15))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(1L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should reject when previous year joiner exceeds max")
    void shouldRejectPreviousYearJoinerWhenExceedsMax() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 12, 24));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2023, 5, 10));

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 12, 24))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(2L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .contains("has already used 2 optional holiday(s)")
                .contains("Maximum allowed based on joining date is 2");
    }

    @Test
    @DisplayName("Should allow full max for employee who joined in Jan of current year")
    void shouldAllowFullMaxForJanuaryJoiner() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 6, 15));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2024, 1, 15));

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 6, 15))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(1L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should allow full max for employee who joined in June of current year")
    void shouldAllowFullMaxForJuneJoiner() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 8, 15));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2024, 6, 30));

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 8, 15))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(0L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should allow only 1 holiday for employee who joined in July of current year")
    void shouldAllowOnlyOneForJulyJoiner() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 8, 15));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2024, 7, 1));

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 8, 15))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(0L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should reject second holiday for employee who joined in July of current year")
    void shouldRejectSecondHolidayForJulyJoiner() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 12, 24));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2024, 7, 15));

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 12, 24))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(1L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .contains("has already used 1 optional holiday(s)")
                .contains("Maximum allowed based on joining date is 1");
    }

    @Test
    @DisplayName("Should allow only 1 holiday for employee who joined in December of current year")
    void shouldAllowOnlyOneForDecemberJoiner() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 12, 24));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2024, 12, 1));

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 12, 24))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(0L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should fall back to configured max when employee not found")
    void shouldFallbackToConfiguredMaxWhenEmployeeNotFound() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 6, 15));

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.empty());
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 6, 15))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(1L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should allow prorated amount with configured max of 4")
    void shouldAllowProratedAmountWithConfiguredMaxOfFour() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 8, 15));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2024, 8, 1));

        when(leaveProperties.getMaxOptionalHolidaysPerYear()).thenReturn(4);

        strategy = new OptionalHolidayValidationStrategy(
                employeeRepository,
                leaveRepository,
                optionalHolidayRepository,
                leaveProperties
        );

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 8, 15))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(1L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should reject when exceeding prorated amount with configured max of 4")
    void shouldRejectWhenExceedingProratedAmountWithConfiguredMaxOfFour() {
        // Given
        Leave leave = createApprovedLeave(LocalDate.of(2024, 12, 24));
        Employee employee = createEmployeeWithJoiningDate(LocalDate.of(2024, 9, 15));

        when(leaveProperties.getMaxOptionalHolidaysPerYear()).thenReturn(4);

        strategy = new OptionalHolidayValidationStrategy(
                employeeRepository,
                leaveRepository,
                optionalHolidayRepository,
                leaveProperties
        );

        lenient().when(employeeRepository.existsById(validEmployeeId)).thenReturn(true);
        lenient().when(employeeRepository.findById(validEmployeeId)).thenReturn(Optional.of(employee));
        lenient().when(optionalHolidayRepository.findAll()).thenReturn(List.of(createOptionalHoliday(LocalDate.of(2024, 12, 24))));
        when(leaveRepository.countApprovedOptionalHolidaysByUserAndYear(anyString(), anyInt())).thenReturn(2L);
        lenient().when(leaveRepository.findOverlappingLeaves(anyString(), any(DateRange.class))).thenReturn(List.of());

        // When
        LeaveValidationResult result = strategy.validate(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .contains("has already used 2 optional holiday(s)")
                .contains("Maximum allowed based on joining date is 2");
    }

    // Helper methods

    private Leave createApprovedLeave(LocalDate date) {
        return createLeave(date, LeaveStatus.APPROVED);
    }

    private Leave createLeave(LocalDate date, LeaveStatus status) {
        return Leave.builder()
                .userId(validEmployeeId.toString())
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .durationType(LeaveDurationType.FULL_DAY)
                .status(status)
                .dateRange(DateRange.builder()
                        .startDate(date)
                        .endDate(date)
                        .build())
                .sourceRefs(List.of(createSourceRef()))
                .build();
    }

    private Leave createApprovedLeaveForYear(LocalDate date) {
        return Leave.builder()
                .userId(validEmployeeId.toString())
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .durationType(LeaveDurationType.FULL_DAY)
                .status(LeaveStatus.APPROVED)
                .dateRange(DateRange.builder()
                        .startDate(date)
                        .endDate(date)
                        .build())
                .sourceRefs(List.of(createSourceRef()))
                .build();
    }

    private LeaveSourceRef createSourceRef() {
        return LeaveSourceRef.builder()
                .sourceType(SourceType.SLACK)
                .sourceId("source-123")
                .build();
    }

    private OptionalHoliday createOptionalHoliday(LocalDate date) {
        return OptionalHoliday.builder()
                .date(date)
                .name("Test Optional Holiday")
                .build();
    }

    private Employee createEmployeeWithJoiningDate(LocalDate dateOfJoining) {
        return Employee.builder()
                .id(validEmployeeId)
                .name("Test Employee")
                .dateOfJoining(dateOfJoining)
                .active(true)
                .build();
    }
}
