package one.june.leave_management.adapter.inbound.web;

import one.june.leave_management.adapter.inbound.web.dto.CreateOptionalHolidayRequest;
import one.june.leave_management.adapter.inbound.web.dto.UpdateOptionalHolidayRequest;
import one.june.leave_management.application.leave.dto.OptionalHolidayDto;
import one.june.leave_management.application.leave.service.OptionalHolidayService;
import one.june.leave_management.domain.common.model.Region;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Optional Holiday Controller Unit Tests")
class OptionalHolidayControllerTest {

    @Mock
    private OptionalHolidayService optionalHolidayService;

    private OptionalHolidayController controller;

    @BeforeEach
    void setUp() {
        controller = new OptionalHolidayController(optionalHolidayService);
    }

    @Nested
    @DisplayName("POST /api/admin/optional-holidays - Create Holiday Tests")
    class CreateHolidayTests {

        @Test
        @DisplayName("Should create holiday successfully and return 201 CREATED")
        void shouldCreateHolidaySuccessfully() {
            // Given
            CreateOptionalHolidayRequest request = CreateOptionalHolidayRequest.builder()
                    .date(LocalDate.of(2024, 12, 25))
                    .name("Christmas Day")
                    .description("Public holiday for Christmas")
                    .build();

            OptionalHoliday holiday = OptionalHoliday.builder()
                    .date(request.getDate())
                    .name(request.getName())
                    .description(request.getDescription())
                    .build();

            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 12, 25))
                    .name("Christmas Day")
                    .description("Public holiday for Christmas")
                    .build();

            when(optionalHolidayService.createHoliday(any(OptionalHoliday.class)))
                    .thenReturn(expectedDto);

            // When
            ResponseEntity<OptionalHolidayDto> response = controller.createHoliday(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(expectedDto.getId());
            assertThat(response.getBody().getName()).isEqualTo("Christmas Day");
            assertThat(response.getBody().getDate()).isEqualTo(LocalDate.of(2024, 12, 25));
            assertThat(response.getBody().getDescription()).isEqualTo("Public holiday for Christmas");

            verify(optionalHolidayService).createHoliday(holiday);
        }

        @Test
        @DisplayName("Should create holiday with null description")
        void shouldCreateHolidayWithNullDescription() {
            // Given
            CreateOptionalHolidayRequest request = CreateOptionalHolidayRequest.builder()
                    .date(LocalDate.of(2024, 1, 1))
                    .name("New Year's Day")
                    .description(null)
                    .build();

            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 1, 1))
                    .name("New Year's Day")
                    .description(null)
                    .build();

            when(optionalHolidayService.createHoliday(any(OptionalHoliday.class)))
                    .thenReturn(expectedDto);

            // When
            ResponseEntity<OptionalHolidayDto> response = controller.createHoliday(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getDescription()).isNull();

            verify(optionalHolidayService).createHoliday(any(OptionalHoliday.class));
        }

        @Test
        @DisplayName("Should create holiday with empty description")
        void shouldCreateHolidayWithEmptyDescription() {
            // Given
            CreateOptionalHolidayRequest request = CreateOptionalHolidayRequest.builder()
                    .date(LocalDate.of(2024, 7, 4))
                    .name("Independence Day")
                    .description("")
                    .build();

            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 7, 4))
                    .name("Independence Day")
                    .description("")
                    .build();

            when(optionalHolidayService.createHoliday(any(OptionalHoliday.class)))
                    .thenReturn(expectedDto);

            // When
            ResponseEntity<OptionalHolidayDto> response = controller.createHoliday(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getDescription()).isEmpty();

            verify(optionalHolidayService).createHoliday(any(OptionalHoliday.class));
        }

        @Test
        @DisplayName("Should pass all request fields to domain model")
        void shouldPassAllRequestFieldsToDomainModel() {
            // Given
            CreateOptionalHolidayRequest request = CreateOptionalHolidayRequest.builder()
                    .date(LocalDate.of(2024, 11, 11))
                    .name("Veterans Day")
                    .description("Honoring military veterans")
                    .build();

            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 11, 11))
                    .name("Veterans Day")
                    .description("Honoring military veterans")
                    .build();

            when(optionalHolidayService.createHoliday(any(OptionalHoliday.class)))
                    .thenReturn(expectedDto);

            // When
            controller.createHoliday(request);

            // Then
            verify(optionalHolidayService).createHoliday(argThat(holiday ->
                    holiday.getDate().equals(LocalDate.of(2024, 11, 11)) &&
                            holiday.getName().equals("Veterans Day") &&
                            holiday.getDescription().equals("Honoring military veterans")
            ));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/optional-holidays - Get All Holidays Tests")
    class GetAllHolidaysTests {

        @Test
        @DisplayName("Should return all holidays and return 200 OK")
        void shouldReturnAllHolidays() {
            // Given
            OptionalHolidayDto holiday1 = OptionalHolidayDto.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 1, 1))
                    .name("New Year's Day")
                    .description("First day of the year")
                    .build();

            OptionalHolidayDto holiday2 = OptionalHolidayDto.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 12, 25))
                    .name("Christmas Day")
                    .description("Christmas celebration")
                    .build();

            OptionalHolidayDto holiday3 = OptionalHolidayDto.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 7, 4))
                    .name("Independence Day")
                    .description("USA Independence Day")
                    .build();

            List<OptionalHolidayDto> expectedHolidays = List.of(holiday1, holiday2, holiday3);

            when(optionalHolidayService.getHolidaysByRegion(Region.PUNE)).thenReturn(expectedHolidays);

            // When
            ResponseEntity<List<OptionalHolidayDto>> response = controller.getAllHolidays(Region.PUNE);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(3);
            assertThat(response.getBody()).containsExactly(holiday1, holiday2, holiday3);

            verify(optionalHolidayService).getHolidaysByRegion(Region.PUNE);
        }

        @Test
        @DisplayName("Should return empty list when no holidays exist")
        void shouldReturnEmptyListWhenNoHolidaysExist() {
            // Given
            when(optionalHolidayService.getHolidaysByRegion(Region.PUNE)).thenReturn(List.of());

            // When
            ResponseEntity<List<OptionalHolidayDto>> response = controller.getAllHolidays(Region.PUNE);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isEmpty();

            verify(optionalHolidayService).getHolidaysByRegion(Region.PUNE);
        }

        @Test
        @DisplayName("Should return single holiday when only one exists")
        void shouldReturnSingleHoliday() {
            // Given
            OptionalHolidayDto holiday = OptionalHolidayDto.builder()
                    .id(UUID.randomUUID())
                    .date(LocalDate.of(2024, 5, 1))
                    .name("Labor Day")
                    .build();

            when(optionalHolidayService.getHolidaysByRegion(Region.PUNE)).thenReturn(List.of(holiday));

            // When
            ResponseEntity<List<OptionalHolidayDto>> response = controller.getAllHolidays(Region.PUNE);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getName()).isEqualTo("Labor Day");
        }
    }

    @Nested
    @DisplayName("GET /api/admin/optional-holidays/{id} - Get Holiday By ID Tests")
    class GetHolidayByIdTests {

        @Test
        @DisplayName("Should return holiday by ID and return 200 OK")
        void shouldReturnHolidayById() {
            // Given
            UUID holidayId = UUID.randomUUID();
            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 2, 14))
                    .name("Valentine's Day")
                    .description("Day of love and romance")
                    .build();

            when(optionalHolidayService.getHolidayById(holidayId)).thenReturn(expectedDto);

            // When
            ResponseEntity<OptionalHolidayDto> response = controller.getHolidayById(holidayId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(holidayId);
            assertThat(response.getBody().getName()).isEqualTo("Valentine's Day");
            assertThat(response.getBody().getDate()).isEqualTo(LocalDate.of(2024, 2, 14));

            verify(optionalHolidayService).getHolidayById(holidayId);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when holiday not found")
        void shouldThrowExceptionWhenHolidayNotFound() {
            // Given
            UUID holidayId = UUID.randomUUID();
            when(optionalHolidayService.getHolidayById(holidayId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> controller.getHolidayById(holidayId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Optional holiday not found with id: " + holidayId);

            verify(optionalHolidayService).getHolidayById(holidayId);
        }

        @Test
        @DisplayName("Should handle holiday with all fields populated")
        void shouldHandleHolidayWithAllFieldsPopulated() {
            // Given
            UUID holidayId = UUID.randomUUID();
            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 10, 31))
                    .name("Halloween")
                    .description("Spooky holiday")
                    .build();

            when(optionalHolidayService.getHolidayById(holidayId)).thenReturn(expectedDto);

            // When
            ResponseEntity<OptionalHolidayDto> response = controller.getHolidayById(holidayId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getDate()).isEqualTo(LocalDate.of(2024, 10, 31));
            assertThat(response.getBody().getName()).isEqualTo("Halloween");
            assertThat(response.getBody().getDescription()).isEqualTo("Spooky holiday");
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/optional-holidays/{id} - Update Holiday Tests")
    class UpdateHolidayTests {

        @Test
        @DisplayName("Should update holiday successfully and return 200 OK")
        void shouldUpdateHolidaySuccessfully() {
            // Given
            UUID holidayId = UUID.randomUUID();
            UpdateOptionalHolidayRequest request = UpdateOptionalHolidayRequest.builder()
                    .date(LocalDate.of(2024, 12, 26))
                    .name("Boxing Day")
                    .description("Day after Christmas")
                    .build();

            OptionalHoliday holiday = OptionalHoliday.builder()
                    .date(request.getDate())
                    .name(request.getName())
                    .description(request.getDescription())
                    .build();

            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 12, 26))
                    .name("Boxing Day")
                    .description("Day after Christmas")
                    .build();

            when(optionalHolidayService.updateHoliday(eq(holidayId), any(OptionalHoliday.class)))
                    .thenReturn(expectedDto);

            // When
            ResponseEntity<OptionalHolidayDto> response = controller.updateHoliday(holidayId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(holidayId);
            assertThat(response.getBody().getName()).isEqualTo("Boxing Day");
            assertThat(response.getBody().getDate()).isEqualTo(LocalDate.of(2024, 12, 26));
            assertThat(response.getBody().getDescription()).isEqualTo("Day after Christmas");

            verify(optionalHolidayService).updateHoliday(holidayId, holiday);
        }

        @Test
        @DisplayName("Should pass all request fields to domain model")
        void shouldPassAllFieldsToDomainModel() {
            // Given
            UUID holidayId = UUID.randomUUID();
            UpdateOptionalHolidayRequest request = UpdateOptionalHolidayRequest.builder()
                    .date(LocalDate.of(2024, 3, 17))
                    .name("St. Patrick's Day")
                    .description("Irish cultural celebration")
                    .build();

            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 3, 17))
                    .name("St. Patrick's Day")
                    .description("Irish cultural celebration")
                    .build();

            when(optionalHolidayService.updateHoliday(eq(holidayId), any(OptionalHoliday.class)))
                    .thenReturn(expectedDto);

            // When
            controller.updateHoliday(holidayId, request);

            // Then
            verify(optionalHolidayService).updateHoliday(eq(holidayId), argThat(holiday ->
                    holiday.getDate().equals(LocalDate.of(2024, 3, 17)) &&
                            holiday.getName().equals("St. Patrick's Day") &&
                            holiday.getDescription().equals("Irish cultural celebration")
            ));
        }

        @Test
        @DisplayName("Should update holiday with null description")
        void shouldUpdateHolidayWithNullDescription() {
            // Given
            UUID holidayId = UUID.randomUUID();
            UpdateOptionalHolidayRequest request = UpdateOptionalHolidayRequest.builder()
                    .date(LocalDate.of(2024, 11, 28))
                    .name("Thanksgiving")
                    .description(null)
                    .build();

            OptionalHolidayDto expectedDto = OptionalHolidayDto.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 11, 28))
                    .name("Thanksgiving")
                    .description(null)
                    .build();

            when(optionalHolidayService.updateHoliday(eq(holidayId), any(OptionalHoliday.class)))
                    .thenReturn(expectedDto);

            // When
            ResponseEntity<OptionalHolidayDto> response = controller.updateHoliday(holidayId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getDescription()).isNull();

            verify(optionalHolidayService).updateHoliday(eq(holidayId), any(OptionalHoliday.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/optional-holidays/{id} - Delete Holiday Tests")
    class DeleteHolidayTests {

        @Test
        @DisplayName("Should delete holiday successfully and return 204 NO CONTENT")
        void shouldDeleteHolidaySuccessfully() {
            // Given
            UUID holidayId = UUID.randomUUID();
            OptionalHolidayDto existingHoliday = OptionalHolidayDto.builder()
                    .id(holidayId)
                    .date(LocalDate.of(2024, 4, 1))
                    .name("April Fools' Day")
                    .build();

            when(optionalHolidayService.getHolidayById(holidayId)).thenReturn(existingHoliday);
            doNothing().when(optionalHolidayService).deleteHoliday(holidayId);

            // When
            ResponseEntity<Void> response = controller.deleteHoliday(holidayId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();

            verify(optionalHolidayService).getHolidayById(holidayId);
            verify(optionalHolidayService).deleteHoliday(holidayId);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when holiday to delete not found")
        void shouldThrowExceptionWhenHolidayToDeleteNotFound() {
            // Given
            UUID holidayId = UUID.randomUUID();
            when(optionalHolidayService.getHolidayById(holidayId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> controller.deleteHoliday(holidayId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Optional holiday not found with id: " + holidayId);

            verify(optionalHolidayService).getHolidayById(holidayId);
            verify(optionalHolidayService, never()).deleteHoliday(any());
        }

        @Test
        @DisplayName("Should check existence before deleting")
        void shouldCheckExistenceBeforeDeleting() {
            // Given
            UUID holidayId = UUID.randomUUID();
            OptionalHolidayDto existingHoliday = OptionalHolidayDto.builder()
                    .id(holidayId)
                    .name("Test Holiday")
                    .build();

            when(optionalHolidayService.getHolidayById(holidayId)).thenReturn(existingHoliday);
            doNothing().when(optionalHolidayService).deleteHoliday(holidayId);

            // When
            controller.deleteHoliday(holidayId);

            // Then
            verify(optionalHolidayService).getHolidayById(holidayId);
            verify(optionalHolidayService).deleteHoliday(holidayId);
        }

        @Test
        @DisplayName("Should not call delete if holiday does not exist")
        void shouldNotCallDeleteIfHolidayDoesNotExist() {
            // Given
            UUID holidayId = UUID.randomUUID();
            when(optionalHolidayService.getHolidayById(holidayId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> controller.deleteHoliday(holidayId))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(optionalHolidayService).getHolidayById(holidayId);
            verify(optionalHolidayService, never()).deleteHoliday(holidayId);
        }
    }
}
