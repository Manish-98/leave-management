package one.june.leave_management.domain.leave.model;

import one.june.leave_management.common.model.DateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LeaveTest {

    private static final String TEST_USER_ID = "test-user-123";

    @Test
    void builderShouldCreateLeaveWithAllFields() {
        UUID id = UUID.randomUUID();
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();
        List<LeaveSourceRef> sourceRefs = new ArrayList<>();

        Leave leave = Leave.builder()
                .id(id)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .sourceRefs(sourceRefs)
                .build();

        assertEquals(id, leave.getId());
        assertEquals(TEST_USER_ID, leave.getUserId());
        assertEquals(dateRange, leave.getDateRange());
        assertEquals(LeaveType.ANNUAL_LEAVE, leave.getType());
        assertEquals(LeaveStatus.REQUESTED, leave.getStatus());
        assertEquals(LeaveDurationType.FULL_DAY, leave.getDurationType());
        assertEquals(sourceRefs, leave.getSourceRefs());
    }

    @Test
    void createFactoryMethodShouldCreateValidLeave() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        Leave leave = Leave.create(
                TEST_USER_ID,
                startDate,
                endDate,
                LeaveType.ANNUAL_LEAVE,
                LeaveStatus.REQUESTED
        );

        assertNotNull(leave);
        assertEquals(TEST_USER_ID, leave.getUserId());
        assertEquals(startDate, leave.getStartDate());
        assertEquals(endDate, leave.getEndDate());
        assertEquals(LeaveType.ANNUAL_LEAVE, leave.getType());
        assertEquals(LeaveStatus.REQUESTED, leave.getStatus());
        assertEquals(LeaveDurationType.FULL_DAY, leave.getDurationType());
        assertNotNull(leave.getSourceRefs());
        assertTrue(leave.getSourceRefs().isEmpty());
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenUserIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> Leave.create(null, LocalDate.now(), LocalDate.now(), LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenStartDateIsNull() {
        assertThrows(NullPointerException.class,
                () -> Leave.create(TEST_USER_ID, null, LocalDate.now(), LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenEndDateIsNull() {
        assertThrows(NullPointerException.class,
                () -> Leave.create(TEST_USER_ID, LocalDate.now(), null, LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenTypeIsNull() {
        assertThrows(NullPointerException.class,
                () -> Leave.create(TEST_USER_ID, LocalDate.now(), LocalDate.now(), null, LeaveStatus.REQUESTED));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenStatusIsNull() {
        assertThrows(NullPointerException.class,
                () -> Leave.create(TEST_USER_ID, LocalDate.now(), LocalDate.now(), LeaveType.ANNUAL_LEAVE, null));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenStartDateIsAfterEndDate() {
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(3);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Leave.create(TEST_USER_ID, startDate, endDate, LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));

        assertEquals("startDate cannot be after endDate", exception.getMessage());
    }

    @Test
    void updateShouldUpdateAllFields() {
        LocalDate newStartDate = LocalDate.now().plusDays(10);
        LocalDate newEndDate = LocalDate.now().plusDays(15);

        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        leave.update(TEST_USER_ID, newStartDate, newEndDate, LeaveType.OPTIONAL_HOLIDAY, LeaveStatus.APPROVED);

        assertEquals(TEST_USER_ID, leave.getUserId());
        assertEquals(newStartDate, leave.getStartDate());
        assertEquals(newEndDate, leave.getEndDate());
        assertEquals(LeaveType.OPTIONAL_HOLIDAY, leave.getType());
        assertEquals(LeaveStatus.APPROVED, leave.getStatus());
    }

    @Test
    void updateShouldThrowExceptionWhenUserIdIsNull() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        assertThrows(NullPointerException.class,
                () -> leave.update(null, LocalDate.now(), LocalDate.now(), LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));
    }

    @Test
    void updateShouldThrowExceptionWhenStartDateIsNull() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        assertThrows(NullPointerException.class,
                () -> leave.update(TEST_USER_ID, null, LocalDate.now(), LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));
    }

    @Test
    void updateShouldThrowExceptionWhenEndDateIsNull() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        assertThrows(NullPointerException.class,
                () -> leave.update(TEST_USER_ID, LocalDate.now(), null, LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));
    }

    @Test
    void updateShouldThrowExceptionWhenTypeIsNull() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        assertThrows(NullPointerException.class,
                () -> leave.update(TEST_USER_ID, LocalDate.now(), LocalDate.now(), null, LeaveStatus.REQUESTED));
    }

    @Test
    void updateShouldThrowExceptionWhenStatusIsNull() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        assertThrows(NullPointerException.class,
                () -> leave.update(TEST_USER_ID, LocalDate.now(), LocalDate.now(), LeaveType.ANNUAL_LEAVE, null));
    }

    @Test
    void updateShouldThrowExceptionWhenStartDateIsAfterEndDate() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(3);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> leave.update(TEST_USER_ID, startDate, endDate, LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));

        assertEquals("startDate cannot be after endDate", exception.getMessage());
    }

    @Test
    void updateShouldThrowExceptionForHalfDayLeaveWithDifferentDates() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .durationType(LeaveDurationType.FIRST_HALF)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(7);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> leave.update(TEST_USER_ID, startDate, endDate, LeaveType.ANNUAL_LEAVE, LeaveStatus.REQUESTED));

        assertEquals("Half-day leaves must have the same start and end date", exception.getMessage());
    }

    @Test
    void addSourceRefShouldAddRefToList() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .sourceRefs(new ArrayList<>())
                .build();

        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .build();

        leave.addSourceRef(sourceRef);

        assertEquals(1, leave.getSourceRefs().size());
        assertTrue(leave.getSourceRefs().contains(sourceRef));
    }

    @Test
    void addSourceRefShouldNotAddDuplicateRef() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .sourceRefs(new ArrayList<>())
                .build();

        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .build();

        leave.addSourceRef(sourceRef);
        leave.addSourceRef(sourceRef);

        assertEquals(1, leave.getSourceRefs().size());
    }

    @Test
    void addSourceRefShouldThrowExceptionWhenRefIsNull() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .sourceRefs(new ArrayList<>())
                .build();

        assertThrows(NullPointerException.class, () -> leave.addSourceRef(null));
    }

    @Test
    void removeSourceRefShouldRemoveRefFromList() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .sourceRefs(new ArrayList<>())
                .build();

        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .build();

        leave.addSourceRef(sourceRef);
        assertEquals(1, leave.getSourceRefs().size());

        leave.removeSourceRef(sourceRef);
        assertEquals(0, leave.getSourceRefs().size());
    }

    @Test
    void hasSourceRefsShouldReturnTrueWhenListIsNotEmpty() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .sourceRefs(new ArrayList<>())
                .build();

        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .build();

        assertFalse(leave.hasSourceRefs());
        leave.addSourceRef(sourceRef);
        assertTrue(leave.hasSourceRefs());
    }

    @Test
    void getSourceRefsShouldReturnDefensiveCopy() {
        List<LeaveSourceRef> originalRefs = new ArrayList<>();
        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .build();
        originalRefs.add(sourceRef);

        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .sourceRefs(originalRefs)
                .build();

        List<LeaveSourceRef> refs = leave.getSourceRefs();
        refs.clear();

        assertFalse(leave.getSourceRefs().isEmpty(), "Original list should not be modified");
    }

    @Test
    void setSourceRefsShouldCreateDefensiveCopy() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        List<LeaveSourceRef> refs = new ArrayList<>();
        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .build();
        refs.add(sourceRef);

        leave.setSourceRefs(refs);
        assertEquals(1, leave.getSourceRefs().size());

        refs.clear();
        assertEquals(1, leave.getSourceRefs().size(), "Internal list should not be modified");
    }

    @Test
    void setSourceRefsShouldHandleNullInput() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        leave.setSourceRefs(null);

        assertNotNull(leave.getSourceRefs());
        assertTrue(leave.getSourceRefs().isEmpty());
    }

    @Test
    void getStartDateShouldReturnNullWhenDateRangeIsNull() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(null)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        assertNull(leave.getStartDate());
    }

    @Test
    void getEndDateShouldReturnNullWhenDateRangeIsNull() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(null)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        assertNull(leave.getEndDate());
    }

    @Test
    void toBuilderShouldCreateCopy() {
        UUID id = UUID.randomUUID();
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        Leave original = Leave.builder()
                .id(id)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        Leave copy = original.toBuilder().build();

        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getUserId(), copy.getUserId());
        assertEquals(original.getDateRange(), copy.getDateRange());
        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getStatus(), copy.getStatus());
        assertEquals(original.getDurationType(), copy.getDurationType());
    }

    @Test
    void equalsAndHashCodeShouldBeBasedOnId() {
        UUID id = UUID.randomUUID();

        Leave leave1 = Leave.builder()
                .id(id)
                .userId("user-1")
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        Leave leave2 = Leave.builder()
                .id(id)
                .userId("user-2")
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(5))
                        .endDate(LocalDate.now().plusDays(7))
                        .build())
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.APPROVED)
                .build();

        Leave leave3 = Leave.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        assertEquals(leave1, leave2);
        assertEquals(leave1.hashCode(), leave2.hashCode());
        assertNotEquals(leave1, leave3);
        assertNotEquals(leave1.hashCode(), leave3.hashCode());
    }

    @Test
    void validateShouldThrowExceptionForHalfDayLeaveWithDifferentDates() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FIRST_HALF)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, leave::validate);
        assertEquals("Half-day leaves must have the same start and end date", exception.getMessage());
    }

    @Test
    void validateShouldPassForValidFullDayLeave() {
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        assertDoesNotThrow(leave::validate);
    }

    @Test
    void validateShouldPassForValidHalfDayLeave() {
        LocalDate date = LocalDate.now().plusDays(1);
        Leave leave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(date)
                        .endDate(date)
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.SECOND_HALF)
                .build();

        assertDoesNotThrow(leave::validate);
    }
}
