package one.june.leave_management.domain.leave.service;

import one.june.leave_management.common.exception.InvalidOptionalHolidayException;
import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.OptionalHolidayRepository;
import one.june.leave_management.domain.leave.validation.LeaveValidationResult;
import one.june.leave_management.domain.leave.validation.LeaveValidationStrategyBase;
import one.june.leave_management.domain.leave.validation.LeaveValidationStrategyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeaveDomainServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private OptionalHolidayRepository optionalHolidayRepository;

    @Mock
    private LeaveValidationStrategyRegistry strategyRegistry;

    @Mock
    private LeaveValidationStrategyBase annualLeaveStrategy;

    @Mock
    private LeaveValidationStrategyBase optionalHolidayStrategy;

    private LeaveDomainService leaveDomainService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final UUID TEST_LEAVE_ID_1 = UUID.randomUUID();
    private static final UUID TEST_LEAVE_ID_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Setup strategy registry mock
        when(strategyRegistry.getStrategy(LeaveType.ANNUAL_LEAVE)).thenReturn(annualLeaveStrategy);
        when(strategyRegistry.getStrategy(LeaveType.OPTIONAL_HOLIDAY)).thenReturn(optionalHolidayStrategy);
        when(annualLeaveStrategy.getType()).thenReturn(LeaveType.ANNUAL_LEAVE);
        when(optionalHolidayStrategy.getType()).thenReturn(LeaveType.OPTIONAL_HOLIDAY);

        leaveDomainService = new LeaveDomainService(leaveRepository, optionalHolidayRepository, strategyRegistry);
    }

    // validateLeaveForPersistence tests

    @Test
    void shouldValidateLeaveForPersistence() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave leave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        when(annualLeaveStrategy.validate(leave)).thenReturn(LeaveValidationResult.success());

        assertDoesNotThrow(() -> leaveDomainService.validateLeaveForPersistence(leave));
    }

    @Test
    void shouldRejectNullLeaveForPersistence() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(null)
        );

        assertEquals("Leave or leave type cannot be null", exception.getMessage());
        verifyNoInteractions(annualLeaveStrategy);
    }

    @Test
    void shouldRejectNewLeaveWithoutSourceRefs() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        when(annualLeaveStrategy.validate(leave))
                .thenReturn(LeaveValidationResult.failure("New leaves must have at least one source reference"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(leave)
        );

        assertEquals("New leaves must have at least one source reference", exception.getMessage());
    }

    @Test
    void shouldRejectLeaveWithNullStatus() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave leave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(null)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        when(annualLeaveStrategy.validate(leave))
                .thenReturn(LeaveValidationResult.failure("Leave status cannot be null"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(leave)
        );

        assertEquals("Leave status cannot be null", exception.getMessage());
    }

    @Test
    void shouldRejectLeaveWithNullType() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave leave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(null)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(leave)
        );

        assertEquals("Leave or leave type cannot be null", exception.getMessage());
    }

    @Test
    void shouldRejectLeaveWithNullDurationType() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave leave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(null)
                .build();

        when(annualLeaveStrategy.validate(leave))
                .thenReturn(LeaveValidationResult.failure("Leave duration type cannot be null"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(leave)
        );

        assertEquals("Leave duration type cannot be null", exception.getMessage());
    }

    @Test
    void shouldRejectHalfDayLeaveWithDifferentDates() {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        DateRange dateRange = DateRange.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Leave leave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FIRST_HALF)
                .build();

        when(annualLeaveStrategy.validate(leave))
                .thenReturn(LeaveValidationResult.failure("Half-day leaves must have the same start and end date"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(leave)
        );

        assertEquals("Half-day leaves must have the same start and end date", exception.getMessage());
    }

    @Test
    void shouldAcceptHalfDayLeaveWithSameDate() {
        LocalDate date = LocalDate.now().plusDays(10);
        DateRange dateRange = DateRange.builder()
                .startDate(date)
                .endDate(date)
                .build();

        Leave leave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.SECOND_HALF)
                .build();

        when(annualLeaveStrategy.validate(leave)).thenReturn(LeaveValidationResult.success());

        assertDoesNotThrow(() -> leaveDomainService.validateLeaveForPersistence(leave));
    }

    @Test
    void shouldRejectApprovedLeaveWithInvalidDates() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(12))
                .endDate(LocalDate.now().plusDays(10))
                .build();

        Leave leave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.APPROVED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        when(annualLeaveStrategy.validate(leave))
                .thenReturn(LeaveValidationResult.failure("Approved leaves must be at least 1 day long"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(leave)
        );

        assertEquals("Approved leaves must be at least 1 day long", exception.getMessage());
    }
}
