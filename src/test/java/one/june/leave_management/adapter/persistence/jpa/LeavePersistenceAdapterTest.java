package one.june.leave_management.adapter.persistence.jpa;

import one.june.leave_management.adapter.persistence.jpa.entity.LeaveJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.entity.LeaveSourceRefJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.LeaveJpaRepository;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveFilters;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Persistence Adapter Unit Tests")
class LeavePersistenceAdapterTest {

    @Mock
    private LeaveJpaRepository leaveJpaRepository;

    @Mock
    private LeaveMapper leaveMapper;

    private LeavePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LeavePersistenceAdapter(leaveJpaRepository, leaveMapper);
    }

    @Nested
    @DisplayName("Save Leave Tests")
    class SaveLeaveTests {

        @Test
        @DisplayName("Should save new leave with source references")
        void shouldSaveNewLeaveWithSourceRefs() {
            // Given
            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .build();

            Leave leave = Leave.builder()
                    .userId("user-123")
                    .dateRange(DateRange.builder()
                            .startDate(LocalDate.of(2024, 1, 1))
                            .endDate(LocalDate.of(2024, 1, 5))
                            .build())
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(List.of(sourceRef))
                    .build();

            LeaveJpaEntity jpaEntity = LeaveJpaEntity.builder()
                    .userId("user-123")
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 5))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(new java.util.ArrayList<>())
                    .build();

            LeaveJpaEntity savedEntity = LeaveJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .userId("user-123")
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 5))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(new java.util.ArrayList<>())
                    .build();

            LeaveSourceRefJpaEntity sourceRefJpaEntity = LeaveSourceRefJpaEntity.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .leave(jpaEntity)
                    .build();

            Leave resultLeave = Leave.builder()
                    .id(savedEntity.getId())
                    .userId(savedEntity.getUserId())
                    .dateRange(DateRange.builder()
                            .startDate(savedEntity.getStartDate())
                            .endDate(savedEntity.getEndDate())
                            .build())
                    .type(savedEntity.getType())
                    .status(savedEntity.getStatus())
                    .durationType(savedEntity.getDurationType())
                    .sourceRefs(List.of(sourceRef))
                    .build();

            when(leaveMapper.toJpaEntity(leave)).thenReturn(jpaEntity);
            when(leaveMapper.toJpaEntity(sourceRef)).thenReturn(sourceRefJpaEntity);
            when(leaveJpaRepository.save(jpaEntity)).thenReturn(savedEntity);
            when(leaveMapper.toDomainEntity(savedEntity)).thenReturn(resultLeave);

            // When
            Leave result = adapter.save(leave);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("user-123");

            verify(leaveMapper).toJpaEntity(leave);
            verify(leaveJpaRepository).save(jpaEntity);
            verify(leaveMapper).toDomainEntity(savedEntity);
        }

        @Test
        @DisplayName("Should update existing leave with new source references")
        void shouldUpdateExistingLeaveWithSourceRefs() {
            // Given
            UUID leaveId = UUID.randomUUID();

            LeaveSourceRef newSourceRef = LeaveSourceRef.builder()
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-456")
                    .build();

            Leave updatedLeave = Leave.builder()
                    .id(leaveId)
                    .userId("user-123")
                    .dateRange(DateRange.builder()
                            .startDate(LocalDate.of(2024, 1, 10))
                            .endDate(LocalDate.of(2024, 1, 15))
                            .build())
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .status(LeaveStatus.APPROVED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(List.of(newSourceRef))
                    .build();

            LeaveJpaEntity updatedJpaEntity = LeaveJpaEntity.builder()
                    .id(leaveId)
                    .userId("user-123")
                    .startDate(LocalDate.of(2024, 1, 10))
                    .endDate(LocalDate.of(2024, 1, 15))
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .status(LeaveStatus.APPROVED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(new java.util.ArrayList<>())
                    .build();

            LeaveJpaEntity existingEntity = LeaveJpaEntity.builder()
                    .id(leaveId)
                    .userId("user-123")
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 5))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(new java.util.ArrayList<>())
                    .build();

            LeaveJpaEntity savedEntity = LeaveJpaEntity.builder()
                    .id(leaveId)
                    .userId("user-123")
                    .startDate(LocalDate.of(2024, 1, 10))
                    .endDate(LocalDate.of(2024, 1, 15))
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .status(LeaveStatus.APPROVED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(new java.util.ArrayList<>())
                    .build();

            LeaveSourceRefJpaEntity sourceRefJpaEntity = LeaveSourceRefJpaEntity.builder()
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-456")
                    .leave(existingEntity)
                    .build();

            Leave resultLeave = Leave.builder()
                    .id(savedEntity.getId())
                    .userId(savedEntity.getUserId())
                    .dateRange(DateRange.builder()
                            .startDate(savedEntity.getStartDate())
                            .endDate(savedEntity.getEndDate())
                            .build())
                    .type(savedEntity.getType())
                    .status(savedEntity.getStatus())
                    .durationType(savedEntity.getDurationType())
                    .sourceRefs(List.of(newSourceRef))
                    .build();

            when(leaveMapper.toJpaEntity(updatedLeave)).thenReturn(updatedJpaEntity);
            when(leaveMapper.toJpaEntity(newSourceRef)).thenReturn(sourceRefJpaEntity);
            when(leaveJpaRepository.findById(leaveId)).thenReturn(Optional.of(existingEntity));
            when(leaveJpaRepository.save(any(LeaveJpaEntity.class))).thenReturn(savedEntity);
            when(leaveMapper.toDomainEntity(savedEntity)).thenReturn(resultLeave);

            // When
            Leave result = adapter.save(updatedLeave);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(leaveId);
            assertThat(result.getType()).isEqualTo(LeaveType.OPTIONAL_HOLIDAY);
            assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED);

            verify(leaveJpaRepository).findById(leaveId);
            verify(leaveJpaRepository).save(existingEntity);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent leave")
        void shouldThrowExceptionWhenUpdatingNonExistentLeave() {
            // Given
            UUID leaveId = UUID.randomUUID();

            Leave leave = Leave.builder()
                    .id(leaveId)
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

            LeaveJpaEntity jpaEntity = LeaveJpaEntity.builder()
                    .id(leaveId)
                    .userId("user-123")
                    .build();

            when(leaveMapper.toJpaEntity(leave)).thenReturn(jpaEntity);
            when(leaveJpaRepository.findById(leaveId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> adapter.save(leave))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Leave not found with id: " + leaveId);

            verify(leaveJpaRepository).findById(leaveId);
            verify(leaveJpaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find leave by ID")
        void shouldFindLeaveById() {
            // Given
            UUID leaveId = UUID.randomUUID();
            LeaveJpaEntity jpaEntity = LeaveJpaEntity.builder()
                    .id(leaveId)
                    .userId("user-123")
                    .build();

            Leave expectedLeave = Leave.builder()
                    .id(leaveId)
                    .userId("user-123")
                    .build();

            when(leaveJpaRepository.findById(leaveId)).thenReturn(Optional.of(jpaEntity));
            when(leaveMapper.toDomainEntity(jpaEntity)).thenReturn(expectedLeave);

            // When
            Optional<Leave> result = adapter.findById(leaveId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(leaveId);
            assertThat(result.get().getUserId()).isEqualTo("user-123");

            verify(leaveJpaRepository).findById(leaveId);
            verify(leaveMapper).toDomainEntity(jpaEntity);
        }

        @Test
        @DisplayName("Should return empty when leave not found by ID")
        void shouldReturnEmptyWhenLeaveNotFoundById() {
            // Given
            UUID leaveId = UUID.randomUUID();
            when(leaveJpaRepository.findById(leaveId)).thenReturn(Optional.empty());

            // When
            Optional<Leave> result = adapter.findById(leaveId);

            // Then
            assertThat(result).isEmpty();
            verify(leaveJpaRepository).findById(leaveId);
            verifyNoInteractions(leaveMapper);
        }
    }

    @Nested
    @DisplayName("Find By User ID Tests")
    class FindByUserIdTests {

        @Test
        @DisplayName("Should find all leaves by user ID")
        void shouldFindLeavesByUserId() {
            // Given
            String userId = "user-123";

            LeaveJpaEntity entity1 = LeaveJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .build();
            LeaveJpaEntity entity2 = LeaveJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .build();

            Leave leave1 = Leave.builder()
                    .id(entity1.getId())
                    .userId(userId)
                    .build();
            Leave leave2 = Leave.builder()
                    .id(entity2.getId())
                    .userId(userId)
                    .build();

            when(leaveJpaRepository.findByUserId(userId)).thenReturn(List.of(entity1, entity2));
            when(leaveMapper.toDomainEntity(entity1)).thenReturn(leave1);
            when(leaveMapper.toDomainEntity(entity2)).thenReturn(leave2);

            // When
            List<Leave> result = adapter.findByUserId(userId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUserId()).isEqualTo(userId);
            assertThat(result.get(1).getUserId()).isEqualTo(userId);

            verify(leaveJpaRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("Should return empty list when user has no leaves")
        void shouldReturnEmptyListWhenUserHasNoLeaves() {
            // Given
            String userId = "user-404";
            when(leaveJpaRepository.findByUserId(userId)).thenReturn(List.of());

            // When
            List<Leave> result = adapter.findByUserId(userId);

            // Then
            assertThat(result).isEmpty();
            verify(leaveJpaRepository).findByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Delete By ID Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should delete leave by ID")
        void shouldDeleteLeaveById() {
            // Given
            UUID leaveId = UUID.randomUUID();
            doNothing().when(leaveJpaRepository).deleteById(leaveId);

            // When
            adapter.deleteById(leaveId);

            // Then
            verify(leaveJpaRepository).deleteById(leaveId);
        }
    }

    @Nested
    @DisplayName("Exists By ID Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when leave exists")
        void shouldReturnTrueWhenLeaveExists() {
            // Given
            UUID leaveId = UUID.randomUUID();
            when(leaveJpaRepository.existsById(leaveId)).thenReturn(true);

            // When
            boolean result = adapter.existsById(leaveId);

            // Then
            assertThat(result).isTrue();
            verify(leaveJpaRepository).existsById(leaveId);
        }

        @Test
        @DisplayName("Should return false when leave does not exist")
        void shouldReturnFalseWhenLeaveDoesNotExist() {
            // Given
            UUID leaveId = UUID.randomUUID();
            when(leaveJpaRepository.existsById(leaveId)).thenReturn(false);

            // When
            boolean result = adapter.existsById(leaveId);

            // Then
            assertThat(result).isFalse();
            verify(leaveJpaRepository).existsById(leaveId);
        }
    }

    @Nested
    @DisplayName("Find Overlapping Leaves Tests")
    class FindOverlappingLeavesTests {

        @Test
        @DisplayName("Should find overlapping leaves without exclusion")
        void shouldFindOverlappingLeaves() {
            // Given
            String userId = "user-123";
            DateRange dateRange = DateRange.builder()
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 5))
                    .build();

            LeaveJpaEntity overlappingEntity = LeaveJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .build();

            Leave overlappingLeave = Leave.builder()
                    .id(overlappingEntity.getId())
                    .userId(userId)
                    .build();

            when(leaveJpaRepository.findOverlappingLeaves(userId, dateRange.getStartDate(), dateRange.getEndDate()))
                    .thenReturn(List.of(overlappingEntity));
            when(leaveMapper.toDomainEntity(overlappingEntity)).thenReturn(overlappingLeave);

            // When
            List<Leave> result = adapter.findOverlappingLeaves(userId, dateRange);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(userId);

            verify(leaveJpaRepository).findOverlappingLeaves(userId, dateRange.getStartDate(), dateRange.getEndDate());
        }

        @Test
        @DisplayName("Should find overlapping leaves with exclusion")
        void shouldFindOverlappingLeavesWithExclusion() {
            // Given
            String userId = "user-123";
            DateRange dateRange = DateRange.builder()
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 5))
                    .build();
            UUID excludeLeaveId = UUID.randomUUID();

            when(leaveJpaRepository.findOverlappingLeaves(userId, dateRange.getStartDate(), dateRange.getEndDate(), excludeLeaveId))
                    .thenReturn(List.of());

            // When
            List<Leave> result = adapter.findOverlappingLeaves(userId, dateRange, excludeLeaveId);

            // Then
            assertThat(result).isEmpty();
            verify(leaveJpaRepository).findOverlappingLeaves(userId, dateRange.getStartDate(), dateRange.getEndDate(), excludeLeaveId);
        }

        @Test
        @DisplayName("Should return empty list when no overlapping leaves found")
        void shouldReturnEmptyListWhenNoOverlappingLeaves() {
            // Given
            String userId = "user-123";
            DateRange dateRange = DateRange.builder()
                    .startDate(LocalDate.of(2024, 6, 1))
                    .endDate(LocalDate.of(2024, 6, 5))
                    .build();

            when(leaveJpaRepository.findOverlappingLeaves(userId, dateRange.getStartDate(), dateRange.getEndDate()))
                    .thenReturn(List.of());

            // When
            List<Leave> result = adapter.findOverlappingLeaves(userId, dateRange);

            // Then
            assertThat(result).isEmpty();
            verify(leaveJpaRepository).findOverlappingLeaves(userId, dateRange.getStartDate(), dateRange.getEndDate());
        }
    }

    @Nested
    @DisplayName("Find By Filters Tests")
    class FindByFiltersTests {

        @Test
        @DisplayName("Should find leaves with user IDs filter")
        void shouldFindLeavesWithUserIdsFilter() {
            // Given
            String userId = "user-123";
            LeaveFilters filters = LeaveFilters.builder()
                    .userIds(List.of(userId))
                    .build();
            Pageable pageable = PageRequest.of(0, 20);

            LeaveJpaEntity entity = LeaveJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .build();

            Page<LeaveJpaEntity> jpaPage = new PageImpl<>(List.of(entity), pageable, 1);

            when(leaveJpaRepository.findByFilters(
                    eq(List.of(userId)), eq(null), eq(null), eq(pageable)))
                    .thenReturn(jpaPage);

            Leave leave = Leave.builder()
                    .id(entity.getId())
                    .userId(userId)
                    .build();

            when(leaveMapper.toDomainEntity(entity)).thenReturn(leave);

            // When
            Page<Leave> result = adapter.findByFilters(filters, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);

            verify(leaveJpaRepository).findByFilters(
                    eq(List.of(userId)), eq(null), eq(null), eq(pageable));
        }

        @Test
        @DisplayName("Should find leaves with date range filter")
        void shouldFindLeavesWithDateRangeFilter() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);
            LeaveFilters filters = LeaveFilters.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            Pageable pageable = PageRequest.of(0, 20);

            // When both dates are provided, the adapter uses findByFiltersWithDateRange
            when(leaveJpaRepository.findByFiltersWithDateRange(
                    eq(null), eq(startDate), eq(endDate), eq(pageable)))
                    .thenReturn(Page.empty());

            // When
            Page<Leave> result = adapter.findByFilters(filters, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            verify(leaveJpaRepository).findByFiltersWithDateRange(
                    eq(null), eq(startDate), eq(endDate), eq(pageable));
        }

        @Test
        @DisplayName("Should find leaves with user IDs and date range filters")
        void shouldFindLeavesWithUserAndDateRangeFilters() {
            // Given
            String userId = "user123";
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 3, 31);
            LeaveFilters filters = LeaveFilters.builder()
                    .userIds(List.of(userId))
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            Pageable pageable = PageRequest.of(0, 20);

            Page<LeaveJpaEntity> emptyPage = Page.empty(pageable);

            // When both dates are provided, the adapter uses findByFiltersWithDateRange
            when(leaveJpaRepository.findByFiltersWithDateRange(
                    eq(List.of(userId)), eq(startDate), eq(endDate), eq(pageable)))
                    .thenReturn(emptyPage);

            // When
            Page<Leave> result = adapter.findByFilters(filters, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            verify(leaveJpaRepository).findByFiltersWithDateRange(
                    eq(List.of(userId)), eq(startDate), eq(endDate), eq(pageable));
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            // Given
            LeaveFilters filters = LeaveFilters.builder().build();
            Pageable pageable = PageRequest.of(0, 10);

            LeaveJpaEntity entity = LeaveJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .userId("user-123")
                    .build();

            Page<LeaveJpaEntity> jpaPage = new PageImpl<>(List.of(entity), pageable, 1);

            when(leaveJpaRepository.findByFilters(
                    eq(null), eq(null), eq(null), eq(pageable)))
                    .thenReturn(jpaPage);

            Leave leave = Leave.builder()
                    .id(entity.getId())
                    .userId("user-123")
                    .build();

            when(leaveMapper.toDomainEntity(entity)).thenReturn(leave);

            // When
            Page<Leave> result = adapter.findByFilters(filters, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getNumber()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
        }
    }
}
