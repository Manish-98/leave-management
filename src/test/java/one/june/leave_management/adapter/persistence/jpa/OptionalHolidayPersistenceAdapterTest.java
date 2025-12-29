package one.june.leave_management.adapter.persistence.jpa;

import one.june.leave_management.adapter.persistence.jpa.entity.OptionalHolidayJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.OptionalHolidayJpaRepository;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Optional Holiday Persistence Adapter Unit Tests")
class OptionalHolidayPersistenceAdapterTest {

    @Mock
    private OptionalHolidayJpaRepository jpaRepository;

    private OptionalHolidayPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OptionalHolidayPersistenceAdapter(jpaRepository);
    }

    @Nested
    @DisplayName("Save Holiday Tests")
    class SaveHolidayTests {

        @Test
        @DisplayName("Should save new holiday successfully")
        void shouldSaveNewHoliday() {
            // Given
            OptionalHoliday holiday = OptionalHoliday.builder()
                    .date(LocalDate.of(2024, 12, 25))
                    .name("Christmas Day")
                    .description("Public holiday for Christmas")
                    .build();

            OptionalHolidayJpaEntity jpaEntity = OptionalHolidayJpaEntity.builder()
                    .date(LocalDate.of(2024, 12, 25))
                    .name("Christmas Day")
                    .description("Public holiday for Christmas")
                    .build();

            OptionalHolidayJpaEntity savedEntity = OptionalHolidayJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 12, 25))
                    .name("Christmas Day")
                    .description("Public holiday for Christmas")
                    .build();

            when(jpaRepository.save(any(OptionalHolidayJpaEntity.class))).thenReturn(savedEntity);

            // When
            OptionalHoliday result = adapter.save(holiday);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo("Christmas Day");
            assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 12, 25));

            verify(jpaRepository).save(any(OptionalHolidayJpaEntity.class));
        }

        @Test
        @DisplayName("Should update existing holiday")
        void shouldUpdateExistingHoliday() {
            // Given
            UUID holidayId = UUID.randomUUID();
            OptionalHoliday holiday = OptionalHoliday.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 1, 1))
                    .name("New Year's Day")
                    .description("Updated description")
                    .build();

            OptionalHolidayJpaEntity savedEntity = OptionalHolidayJpaEntity.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 1, 1))
                    .name("New Year's Day")
                    .description("Updated description")
                    .build();

            when(jpaRepository.save(any(OptionalHolidayJpaEntity.class))).thenReturn(savedEntity);

            // When
            OptionalHoliday result = adapter.save(holiday);

            // Then
            assertThat(result.getId()).isEqualTo(holidayId);
            assertThat(result.getDescription()).isEqualTo("Updated description");

            verify(jpaRepository).save(any(OptionalHolidayJpaEntity.class));
        }

        @Test
        @DisplayName("Should handle holiday with null description")
        void shouldHandleHolidayWithNullDescription() {
            // Given
            OptionalHoliday holiday = OptionalHoliday.builder()
                    .date(LocalDate.of(2024, 7, 4))
                    .name("Independence Day")
                    .description(null)
                    .build();

            OptionalHolidayJpaEntity savedEntity = OptionalHolidayJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 7, 4))
                    .name("Independence Day")
                    .description(null)
                    .build();

            when(jpaRepository.save(any(OptionalHolidayJpaEntity.class))).thenReturn(savedEntity);

            // When
            OptionalHoliday result = adapter.save(holiday);

            // Then
            assertThat(result.getDescription()).isNull();
            verify(jpaRepository).save(any(OptionalHolidayJpaEntity.class));
        }
    }

    @Nested
    @DisplayName("Find All Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should find all holidays")
        void shouldFindAllHolidays() {
            // Given
            OptionalHolidayJpaEntity entity1 = OptionalHolidayJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 1, 1))
                    .name("New Year's Day")
                    .description("First day of the year")
                    .build();

            OptionalHolidayJpaEntity entity2 = OptionalHolidayJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 12, 25))
                    .name("Christmas Day")
                    .description("Christmas celebration")
                    .build();

            when(jpaRepository.findAll()).thenReturn(List.of(entity1, entity2));

            // When
            List<OptionalHoliday> result = adapter.findAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("New Year's Day");
            assertThat(result.get(1).getName()).isEqualTo("Christmas Day");

            verify(jpaRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no holidays exist")
        void shouldReturnEmptyListWhenNoHolidaysExist() {
            // Given
            when(jpaRepository.findAll()).thenReturn(List.of());

            // When
            List<OptionalHoliday> result = adapter.findAll();

            // Then
            assertThat(result).isEmpty();
            verify(jpaRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Find All Ordered By Date Tests")
    class FindAllOrderByDateAscTests {

        @Test
        @DisplayName("Should find all holidays ordered by date ascending")
        void shouldFindAllHolidaysOrderedByDate() {
            // Given
            OptionalHolidayJpaEntity entity1 = OptionalHolidayJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 1, 1))
                    .name("New Year's Day")
                    .build();

            OptionalHolidayJpaEntity entity2 = OptionalHolidayJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 12, 25))
                    .name("Christmas Day")
                    .build();

            OptionalHolidayJpaEntity entity3 = OptionalHolidayJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 7, 4))
                    .name("Independence Day")
                    .build();

            when(jpaRepository.findAllByOrderByDateAsc()).thenReturn(List.of(entity1, entity3, entity2));

            // When
            List<OptionalHoliday> result = adapter.findAllByOrderByDateAsc();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2024, 7, 4));
            assertThat(result.get(2).getDate()).isEqualTo(LocalDate.of(2024, 12, 25));

            verify(jpaRepository).findAllByOrderByDateAsc();
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find holiday by ID")
        void shouldFindHolidayById() {
            // Given
            UUID holidayId = UUID.randomUUID();
            OptionalHolidayJpaEntity jpaEntity = OptionalHolidayJpaEntity.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 2, 14))
                    .name("Valentine's Day")
                    .description("Day of love")
                    .build();

            when(jpaRepository.findById(holidayId)).thenReturn(Optional.of(jpaEntity));

            // When
            Optional<OptionalHoliday> result = adapter.findById(holidayId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(holidayId);
            assertThat(result.get().getName()).isEqualTo("Valentine's Day");
            assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 2, 14));

            verify(jpaRepository).findById(holidayId);
        }

        @Test
        @DisplayName("Should return empty when holiday not found by ID")
        void shouldReturnEmptyWhenHolidayNotFound() {
            // Given
            UUID holidayId = UUID.randomUUID();
            when(jpaRepository.findById(holidayId)).thenReturn(Optional.empty());

            // When
            Optional<OptionalHoliday> result = adapter.findById(holidayId);

            // Then
            assertThat(result).isEmpty();
            verify(jpaRepository).findById(holidayId);
        }
    }

    @Nested
    @DisplayName("Find By Date Tests")
    class FindByDateTests {

        @Test
        @DisplayName("Should find holiday by date")
        void shouldFindHolidayByDate() {
            // Given
            LocalDate date = LocalDate.of(2024, 12, 25);
            OptionalHolidayJpaEntity jpaEntity = OptionalHolidayJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .date(date)
                    .name("Christmas Day")
                    .description("Christmas celebration")
                    .build();

            when(jpaRepository.findByDate(date)).thenReturn(Optional.of(jpaEntity));

            // When
            Optional<OptionalHoliday> result = adapter.findByDate(date);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 12, 25));
            assertThat(result.get().getName()).isEqualTo("Christmas Day");

            verify(jpaRepository).findByDate(date);
        }

        @Test
        @DisplayName("Should return empty when no holiday found for date")
        void shouldReturnEmptyWhenNoHolidayForDate() {
            // Given
            LocalDate date = LocalDate.of(2024, 11, 30);
            when(jpaRepository.findByDate(date)).thenReturn(Optional.empty());

            // When
            Optional<OptionalHoliday> result = adapter.findByDate(date);

            // Then
            assertThat(result).isEmpty();
            verify(jpaRepository).findByDate(date);
        }
    }

    @Nested
    @DisplayName("Delete By ID Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should delete holiday by ID")
        void shouldDeleteHolidayById() {
            // Given
            UUID holidayId = UUID.randomUUID();
            doNothing().when(jpaRepository).deleteById(holidayId);

            // When
            adapter.deleteById(holidayId);

            // Then
            verify(jpaRepository).deleteById(holidayId);
        }
    }

    @Nested
    @DisplayName("Exists By ID Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when holiday exists")
        void shouldReturnTrueWhenHolidayExists() {
            // Given
            UUID holidayId = UUID.randomUUID();
            when(jpaRepository.existsById(holidayId)).thenReturn(true);

            // When
            boolean result = adapter.existsById(holidayId);

            // Then
            assertThat(result).isTrue();
            verify(jpaRepository).existsById(holidayId);
        }

        @Test
        @DisplayName("Should return false when holiday does not exist")
        void shouldReturnFalseWhenHolidayDoesNotExist() {
            // Given
            UUID holidayId = UUID.randomUUID();
            when(jpaRepository.existsById(holidayId)).thenReturn(false);

            // When
            boolean result = adapter.existsById(holidayId);

            // Then
            assertThat(result).isFalse();
            verify(jpaRepository).existsById(holidayId);
        }
    }
}
