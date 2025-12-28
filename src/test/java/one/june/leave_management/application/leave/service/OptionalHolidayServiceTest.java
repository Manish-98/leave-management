package one.june.leave_management.application.leave.service;

import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import one.june.leave_management.application.leave.dto.OptionalHolidayDto;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import one.june.leave_management.domain.leave.port.OptionalHolidayRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Optional Holiday Service Unit Tests")
class OptionalHolidayServiceTest {

    @Mock
    private OptionalHolidayRepository optionalHolidayRepository;

    private OptionalHolidayService optionalHolidayService;

    @BeforeEach
    void setUp() {
        optionalHolidayService = new OptionalHolidayService(optionalHolidayRepository);
    }

    private OptionalHoliday createHoliday(LocalDate date, String name) {
        return OptionalHoliday.builder()
                .id(UUID.randomUUID())
                .date(date)
                .name(name)
                .description(name + " description")
                .build();
    }

    @Test
    @DisplayName("Should get all holidays ordered by date")
    void shouldGetAllHolidaysOrderedByDate() {
        // Given
        OptionalHoliday holiday1 = createHoliday(LocalDate.of(2024, 1, 1), "New Year's Day");
        OptionalHoliday holiday2 = createHoliday(LocalDate.of(2024, 12, 25), "Christmas Day");

        when(optionalHolidayRepository.findAllByOrderByDateAsc()).thenReturn(List.of(holiday1, holiday2));

        // When
        List<OptionalHolidayDto> result = optionalHolidayService.getAllHolidays();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("New Year's Day");
        assertThat(result.get(1).getName()).isEqualTo("Christmas Day");
        verify(optionalHolidayRepository).findAllByOrderByDateAsc();
    }

    @Test
    @DisplayName("Should return empty list when no holidays exist")
    void shouldReturnEmptyListWhenNoHolidaysExist() {
        // Given
        when(optionalHolidayRepository.findAllByOrderByDateAsc()).thenReturn(List.of());

        // When
        List<OptionalHolidayDto> result = optionalHolidayService.getAllHolidays();

        // Then
        assertThat(result).isEmpty();
        verify(optionalHolidayRepository).findAllByOrderByDateAsc();
    }

    @Test
    @DisplayName("Should get all holidays as Slack options")
    void shouldGetAllHolidaysAsSlackOptions() {
        // Given
        OptionalHoliday holiday1 = createHoliday(LocalDate.of(2024, 1, 1), "New Year's Day");
        OptionalHoliday holiday2 = createHoliday(LocalDate.of(2024, 12, 25), "Christmas Day");

        when(optionalHolidayRepository.findAllByOrderByDateAsc()).thenReturn(List.of(holiday1, holiday2));

        // When
        List<SlackOption> result = optionalHolidayService.getAllHolidaysAsSlackOptions();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getValue()).isEqualTo(holiday1.getId().toString());
        assertThat(result.get(0).getText()).isNotNull();
        assertThat(result.get(1).getValue()).isEqualTo(holiday2.getId().toString());
        assertThat(result.get(1).getText()).isNotNull();
        verify(optionalHolidayRepository).findAllByOrderByDateAsc();
    }

    @Test
    @DisplayName("Should get holiday by ID when exists")
    void shouldGetHolidayByIdWhenExists() {
        // Given
        UUID holidayId = UUID.randomUUID();
        OptionalHoliday holiday = createHoliday(LocalDate.of(2024, 7, 4), "Independence Day");
        holiday.setId(holidayId);

        when(optionalHolidayRepository.findById(holidayId)).thenReturn(Optional.of(holiday));

        // When
        OptionalHolidayDto result = optionalHolidayService.getHolidayById(holidayId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(holidayId);
        assertThat(result.getName()).isEqualTo("Independence Day");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 7, 4));
        verify(optionalHolidayRepository).findById(holidayId);
    }

    @Test
    @DisplayName("Should return null when holiday by ID not found")
    void shouldReturnNullWhenHolidayByIdNotFound() {
        // Given
        UUID holidayId = UUID.randomUUID();
        when(optionalHolidayRepository.findById(holidayId)).thenReturn(Optional.empty());

        // When
        OptionalHolidayDto result = optionalHolidayService.getHolidayById(holidayId);

        // Then
        assertThat(result).isNull();
        verify(optionalHolidayRepository).findById(holidayId);
    }

    @Test
    @DisplayName("Should find holiday by ID returning Optional")
    void shouldFindHolidayByIdReturningOptional() {
        // Given
        UUID holidayId = UUID.randomUUID();
        OptionalHoliday holiday = createHoliday(LocalDate.of(2024, 7, 4), "Independence Day");
        holiday.setId(holidayId);

        when(optionalHolidayRepository.findById(holidayId)).thenReturn(Optional.of(holiday));

        // When
        Optional<OptionalHoliday> result = optionalHolidayService.findById(holidayId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(holidayId);
        assertThat(result.get().getName()).isEqualTo("Independence Day");
        verify(optionalHolidayRepository).findById(holidayId);
    }

    @Test
    @DisplayName("Should create new holiday")
    void shouldCreateNewHoliday() {
        // Given
        UUID savedId = UUID.randomUUID();
        OptionalHoliday holiday = createHoliday(LocalDate.of(2024, 7, 4), "Independence Day");
        holiday.setId(null);
        
        OptionalHoliday savedHoliday = createHoliday(LocalDate.of(2024, 7, 4), "Independence Day");
        savedHoliday.setId(savedId);

        when(optionalHolidayRepository.save(holiday)).thenReturn(savedHoliday);

        // When
        OptionalHolidayDto result = optionalHolidayService.createHoliday(holiday);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedId);
        assertThat(result.getName()).isEqualTo("Independence Day");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 7, 4));
        verify(optionalHolidayRepository).save(holiday);
    }

    @Test
    @DisplayName("Should update existing holiday")
    void shouldUpdateExistingHoliday() {
        // Given
        UUID holidayId = UUID.randomUUID();
        OptionalHoliday updatedHoliday = createHoliday(LocalDate.of(2024, 7, 4), "Independence Day (Updated)");
        updatedHoliday.setDescription("Updated description");

        OptionalHoliday savedHoliday = createHoliday(LocalDate.of(2024, 7, 4), "Independence Day (Updated)");
        savedHoliday.setId(holidayId);
        savedHoliday.setDescription("Updated description");

        when(optionalHolidayRepository.existsById(holidayId)).thenReturn(true);
        when(optionalHolidayRepository.save(any(OptionalHoliday.class))).thenReturn(savedHoliday);

        // When
        OptionalHolidayDto result = optionalHolidayService.updateHoliday(holidayId, updatedHoliday);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(holidayId);
        assertThat(result.getName()).isEqualTo("Independence Day (Updated)");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(optionalHolidayRepository).existsById(holidayId);
        verify(optionalHolidayRepository).save(any(OptionalHoliday.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent holiday")
    void shouldThrowWhenUpdatingNonExistentHoliday() {
        // Given
        UUID holidayId = UUID.randomUUID();
        OptionalHoliday updatedHoliday = createHoliday(LocalDate.of(2024, 7, 4), "Independence Day");

        when(optionalHolidayRepository.existsById(holidayId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> optionalHolidayService.updateHoliday(holidayId, updatedHoliday))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Optional holiday not found with id: " + holidayId);

        verify(optionalHolidayRepository).existsById(holidayId);
        verify(optionalHolidayRepository, never()).save(any(OptionalHoliday.class));
    }

    @Test
    @DisplayName("Should delete holiday by ID")
    void shouldDeleteHolidayById() {
        // Given
        UUID holidayId = UUID.randomUUID();
        doNothing().when(optionalHolidayRepository).deleteById(holidayId);

        // When
        optionalHolidayService.deleteHoliday(holidayId);

        // Then
        verify(optionalHolidayRepository).deleteById(holidayId);
    }
}
