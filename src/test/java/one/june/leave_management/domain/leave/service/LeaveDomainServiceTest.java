package one.june.leave_management.domain.leave.service;

import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveDomainServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    private LeaveDomainService leaveDomainService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final UUID TEST_LEAVE_ID_1 = UUID.randomUUID();
    private static final UUID TEST_LEAVE_ID_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        leaveDomainService = new LeaveDomainService(leaveRepository);
    }

    // validateNoOverlappingLeaves tests

    @Test
    void shouldAllowLeaveWhenNoOverlappingLeavesExist() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        when(leaveRepository.findOverlappingLeaves(eq(TEST_USER_ID), eq(dateRange)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> leaveDomainService.validateNoOverlappingLeaves(leave));
        verify(leaveRepository).findOverlappingLeaves(TEST_USER_ID, dateRange);
    }

    @Test
    void shouldRejectLeaveWhenOverlappingLeaveExists() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave newLeave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        Leave existingLeave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(11))
                        .endDate(LocalDate.now().plusDays(15))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.APPROVED)
                .build();

        when(leaveRepository.findOverlappingLeaves(eq(TEST_USER_ID), eq(dateRange)))
                .thenReturn(Collections.singletonList(existingLeave));

        OverlappingLeaveException exception = assertThrows(
                OverlappingLeaveException.class,
                () -> leaveDomainService.validateNoOverlappingLeaves(newLeave)
        );

        assertEquals(TEST_USER_ID, exception.getUserId());
        assertEquals(dateRange.getStartDate(), exception.getStartDate());
        assertEquals(dateRange.getEndDate(), exception.getEndDate());
        assertEquals(existingLeave.getId(), exception.getExistingLeaveId());
        verify(leaveRepository).findOverlappingLeaves(TEST_USER_ID, dateRange);
    }

    @Test
    void shouldAllowLeaveUpdateWhenOnlySameLeaveExists() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave existingLeave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        when(leaveRepository.findOverlappingLeaves(eq(TEST_USER_ID), eq(dateRange), eq(TEST_LEAVE_ID_1)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> leaveDomainService.validateNoOverlappingLeaves(existingLeave));
        verify(leaveRepository).findOverlappingLeaves(TEST_USER_ID, dateRange, TEST_LEAVE_ID_1);
    }

    @Test
    void shouldRejectLeaveUpdateWhenOtherOverlappingLeaveExists() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();

        Leave updatingLeave = Leave.builder()
                .id(TEST_LEAVE_ID_1)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        Leave otherExistingLeave = Leave.builder()
                .id(TEST_LEAVE_ID_2)
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(11))
                        .endDate(LocalDate.now().plusDays(15))
                        .build())
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.APPROVED)
                .build();

        when(leaveRepository.findOverlappingLeaves(eq(TEST_USER_ID), eq(dateRange), eq(TEST_LEAVE_ID_1)))
                .thenReturn(Collections.singletonList(otherExistingLeave));

        OverlappingLeaveException exception = assertThrows(
                OverlappingLeaveException.class,
                () -> leaveDomainService.validateNoOverlappingLeaves(updatingLeave)
        );

        assertEquals(TEST_USER_ID, exception.getUserId());
        assertEquals(dateRange.getStartDate(), exception.getStartDate());
        assertEquals(dateRange.getEndDate(), exception.getEndDate());
        assertEquals(otherExistingLeave.getId(), exception.getExistingLeaveId());
        verify(leaveRepository).findOverlappingLeaves(TEST_USER_ID, dateRange, TEST_LEAVE_ID_1);
    }

    @Test
    void shouldRejectLeaveWhenNullLeaveIsProvided() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateNoOverlappingLeaves(null)
        );

        assertEquals("Leave cannot be null", exception.getMessage());
        verifyNoInteractions(leaveRepository);
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

        assertDoesNotThrow(() -> leaveDomainService.validateLeaveForPersistence(leave));
    }

    @Test
    void shouldRejectNullLeaveForPersistence() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(null)
        );

        assertEquals("Leave cannot be null", exception.getMessage());
        verifyNoInteractions(leaveRepository);
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

        assertEquals("Leave type cannot be null", exception.getMessage());
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

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateLeaveForPersistence(leave)
        );

        assertEquals("Approved leaves must be at least 1 day long", exception.getMessage());
    }
}
