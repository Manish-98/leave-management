package one.june.leave_management.adapter.persistence.jpa;

import one.june.leave_management.adapter.persistence.jpa.entity.LeaveJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.entity.LeaveSourceRefJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.LeaveSourceRefJpaRepository;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Source Reference Persistence Adapter Unit Tests")
class LeaveSourceRefPersistenceAdapterTest {

    @Mock
    private LeaveSourceRefJpaRepository leaveSourceRefJpaRepository;

    @Mock
    private LeaveMapper leaveMapper;

    private LeaveSourceRefPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LeaveSourceRefPersistenceAdapter(leaveSourceRefJpaRepository, leaveMapper);
    }

    @Nested
    @DisplayName("Save Source Reference Tests")
    class SaveSourceRefTests {

        @Test
        @DisplayName("Should save source reference with associated leave")
        void shouldSaveSourceRefWithLeave() {
            // Given
            UUID leaveId = UUID.randomUUID();
            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .leaveId(leaveId)
                    .build();

            LeaveSourceRefJpaEntity jpaEntity = LeaveSourceRefJpaEntity.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .leave(LeaveJpaEntity.builder().id(leaveId).build())
                    .build();

            LeaveSourceRefJpaEntity savedEntity = LeaveSourceRefJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .leave(LeaveJpaEntity.builder().id(leaveId).build())
                    .build();

            when(leaveMapper.toJpaEntity(sourceRef)).thenReturn(jpaEntity);
            when(leaveSourceRefJpaRepository.save(jpaEntity)).thenReturn(savedEntity);
            when(leaveMapper.toDomainEntity(savedEntity)).thenReturn(sourceRef);

            // When
            LeaveSourceRef result = adapter.save(sourceRef);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSourceType()).isEqualTo(SourceType.WEB);
            assertThat(result.getSourceId()).isEqualTo("web-123");

            verify(leaveMapper).toJpaEntity(sourceRef);
            verify(leaveSourceRefJpaRepository).save(jpaEntity);
            verify(leaveMapper).toDomainEntity(savedEntity);
        }

        @Test
        @DisplayName("Should throw exception when source reference has no associated leave")
        void shouldThrowExceptionWhenSourceRefHasNoLeave() {
            // Given
            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-456")
                    .build();

            LeaveSourceRefJpaEntity jpaEntity = LeaveSourceRefJpaEntity.builder()
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-456")
                    .leave(null)
                    .build();

            when(leaveMapper.toJpaEntity(sourceRef)).thenReturn(jpaEntity);

            // When & Then
            assertThatThrownBy(() -> adapter.save(sourceRef))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("LeaveSourceRef must be associated with a Leave");

            verify(leaveMapper).toJpaEntity(sourceRef);
            verify(leaveSourceRefJpaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find source reference by ID")
        void shouldFindSourceRefById() {
            // Given
            UUID sourceRefId = UUID.randomUUID();
            LeaveSourceRefJpaEntity jpaEntity = LeaveSourceRefJpaEntity.builder()
                    .id(sourceRefId)
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .build();

            LeaveSourceRef expectedRef = LeaveSourceRef.builder()
                    .id(sourceRefId)
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .build();

            when(leaveSourceRefJpaRepository.findById(sourceRefId)).thenReturn(Optional.of(jpaEntity));
            when(leaveMapper.toDomainEntity(jpaEntity)).thenReturn(expectedRef);

            // When
            Optional<LeaveSourceRef> result = adapter.findById(sourceRefId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(sourceRefId);
            assertThat(result.get().getSourceType()).isEqualTo(SourceType.WEB);

            verify(leaveSourceRefJpaRepository).findById(sourceRefId);
            verify(leaveMapper).toDomainEntity(jpaEntity);
        }

        @Test
        @DisplayName("Should return empty when source reference not found by ID")
        void shouldReturnEmptyWhenSourceRefNotFound() {
            // Given
            UUID sourceRefId = UUID.randomUUID();
            when(leaveSourceRefJpaRepository.findById(sourceRefId)).thenReturn(Optional.empty());

            // When
            Optional<LeaveSourceRef> result = adapter.findById(sourceRefId);

            // Then
            assertThat(result).isEmpty();
            verify(leaveSourceRefJpaRepository).findById(sourceRefId);
            verifyNoInteractions(leaveMapper);
        }
    }

    @Nested
    @DisplayName("Find By Source Type and Source ID Tests")
    class FindBySourceTypeAndSourceIdTests {

        @Test
        @DisplayName("Should find source reference by source type and source ID")
        void shouldFindSourceRefBySourceTypeAndSourceId() {
            // Given
            SourceType sourceType = SourceType.SLACK;
            String sourceId = "slack-123";

            LeaveSourceRefJpaEntity jpaEntity = LeaveSourceRefJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .build();

            LeaveSourceRef expectedRef = LeaveSourceRef.builder()
                    .id(jpaEntity.getId())
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .build();

            when(leaveSourceRefJpaRepository.findBySourceTypeAndSourceId(sourceType, sourceId))
                    .thenReturn(Optional.of(jpaEntity));
            when(leaveMapper.toDomainEntity(jpaEntity)).thenReturn(expectedRef);

            // When
            Optional<LeaveSourceRef> result = adapter.findBySourceTypeAndSourceId(sourceType, sourceId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getSourceType()).isEqualTo(SourceType.SLACK);
            assertThat(result.get().getSourceId()).isEqualTo("slack-123");

            verify(leaveSourceRefJpaRepository).findBySourceTypeAndSourceId(sourceType, sourceId);
        }

        @Test
        @DisplayName("Should return empty when source reference not found by source type and source ID")
        void shouldReturnEmptyWhenSourceRefNotFoundByTypeAndId() {
            // Given
            SourceType sourceType = SourceType.CALENDAR;
            String sourceId = "calendar-404";

            when(leaveSourceRefJpaRepository.findBySourceTypeAndSourceId(sourceType, sourceId))
                    .thenReturn(Optional.empty());

            // When
            Optional<LeaveSourceRef> result = adapter.findBySourceTypeAndSourceId(sourceType, sourceId);

            // Then
            assertThat(result).isEmpty();
            verify(leaveSourceRefJpaRepository).findBySourceTypeAndSourceId(sourceType, sourceId);
        }
    }

    @Nested
    @DisplayName("Find By Leave ID Tests")
    class FindByLeaveIdTests {

        @Test
        @DisplayName("Should find all source references by leave ID")
        void shouldFindAllSourceRefsByLeaveId() {
            // Given
            UUID leaveId = UUID.randomUUID();

            LeaveSourceRefJpaEntity entity1 = LeaveSourceRefJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.WEB)
                    .sourceId("web-1")
                    .build();

            LeaveSourceRefJpaEntity entity2 = LeaveSourceRefJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-1")
                    .build();

            LeaveSourceRef ref1 = LeaveSourceRef.builder()
                    .id(entity1.getId())
                    .sourceType(SourceType.WEB)
                    .sourceId("web-1")
                    .build();

            LeaveSourceRef ref2 = LeaveSourceRef.builder()
                    .id(entity2.getId())
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-1")
                    .build();

            when(leaveSourceRefJpaRepository.findByLeaveId(leaveId)).thenReturn(List.of(entity1, entity2));
            when(leaveMapper.toDomainEntity(entity1)).thenReturn(ref1);
            when(leaveMapper.toDomainEntity(entity2)).thenReturn(ref2);

            // When
            List<LeaveSourceRef> result = adapter.findByLeaveId(leaveId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSourceType()).isEqualTo(SourceType.WEB);
            assertThat(result.get(1).getSourceType()).isEqualTo(SourceType.SLACK);

            verify(leaveSourceRefJpaRepository).findByLeaveId(leaveId);
        }

        @Test
        @DisplayName("Should return empty list when no source references found for leave ID")
        void shouldReturnEmptyListWhenNoSourceRefsForLeaveId() {
            // Given
            UUID leaveId = UUID.randomUUID();
            when(leaveSourceRefJpaRepository.findByLeaveId(leaveId)).thenReturn(List.of());

            // When
            List<LeaveSourceRef> result = adapter.findByLeaveId(leaveId);

            // Then
            assertThat(result).isEmpty();
            verify(leaveSourceRefJpaRepository).findByLeaveId(leaveId);
        }
    }

    @Nested
    @DisplayName("Delete By ID Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should delete source reference by ID")
        void shouldDeleteSourceRefById() {
            // Given
            UUID sourceRefId = UUID.randomUUID();
            doNothing().when(leaveSourceRefJpaRepository).deleteById(sourceRefId);

            // When
            adapter.deleteById(sourceRefId);

            // Then
            verify(leaveSourceRefJpaRepository).deleteById(sourceRefId);
        }
    }

    @Nested
    @DisplayName("Exists By ID Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when source reference exists")
        void shouldReturnTrueWhenSourceRefExists() {
            // Given
            UUID sourceRefId = UUID.randomUUID();
            when(leaveSourceRefJpaRepository.existsById(sourceRefId)).thenReturn(true);

            // When
            boolean result = adapter.existsById(sourceRefId);

            // Then
            assertThat(result).isTrue();
            verify(leaveSourceRefJpaRepository).existsById(sourceRefId);
        }

        @Test
        @DisplayName("Should return false when source reference does not exist")
        void shouldReturnFalseWhenSourceRefDoesNotExist() {
            // Given
            UUID sourceRefId = UUID.randomUUID();
            when(leaveSourceRefJpaRepository.existsById(sourceRefId)).thenReturn(false);

            // When
            boolean result = adapter.existsById(sourceRefId);

            // Then
            assertThat(result).isFalse();
            verify(leaveSourceRefJpaRepository).existsById(sourceRefId);
        }
    }

    @Nested
    @DisplayName("Find By Source Type and Source ID With Leave Tests")
    class FindBySourceTypeAndSourceIdWithLeaveTests {

        @Test
        @DisplayName("Should find source reference with leave and populate leave ID")
        void shouldFindSourceRefWithLeaveAndPopulateLeaveId() {
            // Given
            SourceType sourceType = SourceType.WEB;
            String sourceId = "web-456";
            UUID leaveId = UUID.randomUUID();

            LeaveJpaEntity leaveEntity = LeaveJpaEntity.builder()
                    .id(leaveId)
                    .userId("user-123")
                    .build();

            LeaveSourceRefJpaEntity jpaEntity = LeaveSourceRefJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .leave(leaveEntity)
                    .build();

            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .id(jpaEntity.getId())
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .build();

            when(leaveSourceRefJpaRepository.findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId))
                    .thenReturn(Optional.of(jpaEntity));
            when(leaveMapper.toDomainEntity(jpaEntity)).thenReturn(sourceRef);

            // When
            Optional<LeaveSourceRef> result = adapter.findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getSourceType()).isEqualTo(SourceType.WEB);
            assertThat(result.get().getSourceId()).isEqualTo("web-456");
            assertThat(result.get().getLeaveId()).isEqualTo(leaveId);

            verify(leaveSourceRefJpaRepository).findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId);
            verify(leaveMapper).toDomainEntity(jpaEntity);
        }

        @Test
        @DisplayName("Should handle source reference without associated leave")
        void shouldHandleSourceRefWithoutAssociatedLeave() {
            // Given
            SourceType sourceType = SourceType.KIMAI;
            String sourceId = "kimai-789";

            LeaveSourceRefJpaEntity jpaEntity = LeaveSourceRefJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .leave(null)
                    .build();

            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .id(jpaEntity.getId())
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .build();

            when(leaveSourceRefJpaRepository.findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId))
                    .thenReturn(Optional.of(jpaEntity));
            when(leaveMapper.toDomainEntity(jpaEntity)).thenReturn(sourceRef);

            // When
            Optional<LeaveSourceRef> result = adapter.findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getLeaveId()).isNull();

            verify(leaveSourceRefJpaRepository).findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId);
        }

        @Test
        @DisplayName("Should return empty when source reference not found with leave")
        void shouldReturnEmptyWhenSourceRefNotFoundWithLeave() {
            // Given
            SourceType sourceType = SourceType.CSV_BULK;
            String sourceId = "csv-999";

            when(leaveSourceRefJpaRepository.findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId))
                    .thenReturn(Optional.empty());

            // When
            Optional<LeaveSourceRef> result = adapter.findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId);

            // Then
            assertThat(result).isEmpty();
            verify(leaveSourceRefJpaRepository).findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId);
            verifyNoInteractions(leaveMapper);
        }
    }
}
