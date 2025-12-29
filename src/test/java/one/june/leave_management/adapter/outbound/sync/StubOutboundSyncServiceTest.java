package one.june.leave_management.adapter.outbound.sync;

import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Stub Outbound Sync Service Unit Tests")
class StubOutboundSyncServiceTest {

    private StubOutboundSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new StubOutboundSyncService();
    }

    @Nested
    @DisplayName("Sync Tests")
    class SyncTests {

        @Test
        @DisplayName("Should sync leave successfully without throwing exception")
        void shouldSyncLeaveSuccessfully() {
            // Given
            Leave leave = Leave.builder()
                    .id(UUID.randomUUID())
                    .userId("user-123")
                    .dateRange(DateRange.builder()
                            .startDate(LocalDate.of(2024, 1, 1))
                            .endDate(LocalDate.of(2024, 1, 5))
                            .build())
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(List.of())
                    .build();

            SourceType originatingSource = SourceType.WEB;

            // When & Then - Should not throw exception
            assertThatCode(() -> syncService.sync(leave, originatingSource))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should sync leave with source references")
        void shouldSyncLeaveWithSourceRefs() {
            // Given
            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-123")
                    .build();

            Leave leave = Leave.builder()
                    .id(UUID.randomUUID())
                    .userId("user-456")
                    .dateRange(DateRange.builder()
                            .startDate(LocalDate.of(2024, 6, 1))
                            .endDate(LocalDate.of(2024, 6, 3))
                            .build())
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .status(LeaveStatus.APPROVED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(List.of(sourceRef))
                    .build();

            SourceType originatingSource = SourceType.SLACK;

            // When & Then - Should not throw exception
            assertThatCode(() -> syncService.sync(leave, originatingSource))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should sync leave from different originating sources")
        void shouldSyncLeaveFromDifferentSources() {
            // Given
            Leave leave = Leave.builder()
                    .id(UUID.randomUUID())
                    .userId("user-789")
                    .dateRange(DateRange.builder()
                            .startDate(LocalDate.of(2024, 12, 25))
                            .endDate(LocalDate.of(2024, 12, 25))
                            .build())
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.APPROVED)
                    .durationType(LeaveDurationType.FIRST_HALF)
                    .sourceRefs(List.of())
                    .build();

            // When & Then - Test all source types
            for (SourceType sourceType : SourceType.values()) {
                assertThatCode(() -> syncService.sync(leave, sourceType))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should handle leave with null values")
        void shouldHandleLeaveWithNullValues() {
            // Given
            Leave leave = Leave.builder()
                    .userId("user-null-test")
                    .sourceRefs(List.of())
                    .build();

            SourceType originatingSource = SourceType.WEB;

            // When & Then - Should not throw exception even with null fields
            assertThatCode(() -> syncService.sync(leave, originatingSource))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle all leave types")
        void shouldHandleAllLeaveTypes() {
            // Given
            for (LeaveType leaveType : LeaveType.values()) {
                Leave leave = Leave.builder()
                        .id(UUID.randomUUID())
                        .userId("user-all-types")
                        .dateRange(DateRange.builder()
                                .startDate(LocalDate.of(2024, 1, 1))
                                .endDate(LocalDate.of(2024, 1, 1))
                                .build())
                        .type(leaveType)
                        .status(LeaveStatus.REQUESTED)
                        .durationType(LeaveDurationType.FULL_DAY)
                        .sourceRefs(List.of())
                        .build();

                // When & Then - Should handle all leave types
                assertThatCode(() -> syncService.sync(leave, SourceType.WEB))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should handle all leave statuses")
        void shouldHandleAllLeaveStatuses() {
            // Given
            for (LeaveStatus status : LeaveStatus.values()) {
                Leave leave = Leave.builder()
                        .id(UUID.randomUUID())
                        .userId("user-all-statuses")
                        .dateRange(DateRange.builder()
                                .startDate(LocalDate.of(2024, 1, 1))
                                .endDate(LocalDate.of(2024, 1, 1))
                                .build())
                        .type(LeaveType.ANNUAL_LEAVE)
                        .status(status)
                        .durationType(LeaveDurationType.FULL_DAY)
                        .sourceRefs(List.of())
                        .build();

                // When & Then - Should handle all statuses
                assertThatCode(() -> syncService.sync(leave, SourceType.SLACK))
                        .doesNotThrowAnyException();
            }
        }
    }
}
