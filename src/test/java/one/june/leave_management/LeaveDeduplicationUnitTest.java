package one.june.leave_management;

import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.service.LeaveDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveDeduplicationUnitTest {

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

    @Test
    void shouldAllowLeaveWhenNoOverlappingLeavesExist() {
        // Given
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

        // When & Then
        assertDoesNotThrow(() -> leaveDomainService.validateNoOverlappingLeaves(leave));
        verify(leaveRepository).findOverlappingLeaves(TEST_USER_ID, dateRange);
    }

    @Test
    void shouldRejectLeaveWhenOverlappingLeaveExists() {
        // Given
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
                .thenReturn(Arrays.asList(existingLeave));

        // When & Then
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
        // Given
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

        // Updating the same leave (same ID)
        when(leaveRepository.findOverlappingLeaves(eq(TEST_USER_ID), eq(dateRange), eq(TEST_LEAVE_ID_1)))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertDoesNotThrow(() -> leaveDomainService.validateNoOverlappingLeaves(existingLeave));
        verify(leaveRepository).findOverlappingLeaves(TEST_USER_ID, dateRange, TEST_LEAVE_ID_1);
    }

    @Test
    void shouldRejectLeaveUpdateWhenOtherOverlappingLeaveExists() {
        // Given
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
                .thenReturn(Arrays.asList(otherExistingLeave));

        // When & Then
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
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leaveDomainService.validateNoOverlappingLeaves(null)
        );

        assertEquals("Leave cannot be null", exception.getMessage());
        verifyNoInteractions(leaveRepository);
    }

    @Test
    void shouldValidateLeaveForPersistenceAndDeduplicationDirectly() {
        // Given
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
                .build();

        // Mock the repository method that will be called for deduplication
        when(leaveRepository.findOverlappingLeaves(eq(TEST_USER_ID), eq(dateRange), eq(TEST_LEAVE_ID_1)))
                .thenReturn(Collections.emptyList());

        // When & Then - Call both methods directly as they would be in the service
        assertDoesNotThrow(() -> {
            leaveDomainService.validateLeaveForPersistence(leave);
            leaveDomainService.validateNoOverlappingLeaves(leave);
        });

        // Verify that deduplication validation is called
        verify(leaveRepository).findOverlappingLeaves(TEST_USER_ID, dateRange, TEST_LEAVE_ID_1);
    }

    @Test
    void shouldFailDeduplicationValidationWhenLeaveIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    leaveDomainService.validateLeaveForPersistence(null);
                    leaveDomainService.validateNoOverlappingLeaves(null);
                }
        );

        assertEquals("Leave cannot be null", exception.getMessage());
        verifyNoInteractions(leaveRepository);
    }
}