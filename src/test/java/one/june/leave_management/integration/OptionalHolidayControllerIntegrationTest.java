package one.june.leave_management.integration;

import one.june.leave_management.adapter.inbound.web.dto.CreateOptionalHolidayRequest;
import one.june.leave_management.adapter.inbound.web.dto.UpdateOptionalHolidayRequest;
import one.june.leave_management.application.leave.dto.OptionalHolidayDto;
import one.june.leave_management.adapter.persistence.jpa.repository.OptionalHolidayJpaRepository;
import one.june.leave_management.domain.common.model.Region;
import one.june.leave_management.test.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Optional Holiday Controller.
 * Tests CRUD operations for optional holidays via REST API.
 * Uses transactional=true for automatic test isolation and rollback.
 * Test data is created via API calls to validate controller endpoints.
 */
@IntegrationTest
@DisplayName("Optional Holiday Controller Integration Tests")
class OptionalHolidayControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OptionalHolidayJpaRepository optionalHolidayRepository;

    private String baseUrl;
    private RestTemplate restTemplate;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/admin/optional-holidays";
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Clean up any existing data
        optionalHolidayRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create optional holiday successfully")
    void shouldCreateOptionalHolidaySuccessfully() {
        // Given
        CreateOptionalHolidayRequest request = CreateOptionalHolidayRequest.builder()
                .date(LocalDate.of(2024, 12, 25))
                .name("Christmas Day")
                .description("Christmas celebration")
                .region(Region.PUNE)
                .build();

        HttpEntity<CreateOptionalHolidayRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<OptionalHolidayDto> response = restTemplate.postForEntity(
                baseUrl,
                entity,
                OptionalHolidayDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Christmas Day");
        assertThat(response.getBody().getDate()).isEqualTo(LocalDate.of(2024, 12, 25));
        assertThat(response.getBody().getDescription()).isEqualTo("Christmas celebration");
    }

    @Test
    @DisplayName("Should get all optional holidays")
    void shouldGetAllOptionalHolidays() {
        // Given
        createHolidayViaApi(LocalDate.of(2024, 1, 1), "New Year's Day", "New Year celebration");
        createHolidayViaApi(LocalDate.of(2024, 12, 25), "Christmas Day", "Christmas celebration");

        // When
        ResponseEntity<List<OptionalHolidayDto>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getName()).isEqualTo("New Year's Day");
        assertThat(response.getBody().get(1).getName()).isEqualTo("Christmas Day");
    }

    @Test
    @DisplayName("Should return empty list when no holidays exist")
    void shouldReturnEmptyListWhenNoHolidaysExist() {
        // When
        ResponseEntity<List<OptionalHolidayDto>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Should get holiday by ID when exists")
    void shouldGetHolidayByIdWhenExists() {
        // Given
        UUID holidayId = createHolidayViaApi(
                LocalDate.of(2024, 7, 4),
                "Independence Day",
                "Independence Day celebration"
        );

        // When
        ResponseEntity<OptionalHolidayDto> response = restTemplate.exchange(
                baseUrl + "/" + holidayId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                OptionalHolidayDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(holidayId);
        assertThat(response.getBody().getName()).isEqualTo("Independence Day");
    }

    @Test
    @DisplayName("Should return 400 when getting non-existent holiday")
    void shouldReturn400WhenGettingNonExistentHoliday() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        org.springframework.web.client.HttpClientErrorException.BadRequest exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(
                        baseUrl + "/" + nonExistentId,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                )
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("Optional holiday not found");
    }

    @Test
    @DisplayName("Should update existing holiday")
    void shouldUpdateExistingHoliday() {
        // Given
        UUID holidayId = createHolidayViaApi(
                LocalDate.of(2024, 7, 4),
                "Independence Day",
                "Independence Day"
        );

        UpdateOptionalHolidayRequest updateRequest = UpdateOptionalHolidayRequest.builder()
                .date(LocalDate.of(2024, 7, 4))
                .name("Independence Day (Updated)")
                .description("Updated description")
                .region(Region.PUNE)
                .build();

        HttpEntity<UpdateOptionalHolidayRequest> entity = new HttpEntity<>(updateRequest, headers);

        // When
        ResponseEntity<OptionalHolidayDto> response = restTemplate.exchange(
                baseUrl + "/" + holidayId,
                HttpMethod.PUT,
                entity,
                OptionalHolidayDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(holidayId);
        assertThat(response.getBody().getName()).isEqualTo("Independence Day (Updated)");
        assertThat(response.getBody().getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("Should return 400 when updating non-existent holiday")
    void shouldReturn400WhenUpdatingNonExistentHoliday() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        UpdateOptionalHolidayRequest updateRequest = UpdateOptionalHolidayRequest.builder()
                .date(LocalDate.of(2024, 7, 4))
                .name("Test Holiday")
                .description("Test")
                .region(Region.PUNE)
                .build();

        HttpEntity<UpdateOptionalHolidayRequest> entity = new HttpEntity<>(updateRequest, headers);

        // When & Then
        org.springframework.web.client.HttpClientErrorException.BadRequest exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(
                        baseUrl + "/" + nonExistentId,
                        HttpMethod.PUT,
                        entity,
                        String.class
                )
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("Optional holiday not found");
    }

    @Test
    @DisplayName("Should delete holiday successfully")
    void shouldDeleteHolidaySuccessfully() {
        // Given
        UUID holidayId = createHolidayViaApi(
                LocalDate.of(2024, 7, 4),
                "Independence Day",
                "Independence Day"
        );

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + holidayId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify holiday is deleted
        assertThat(optionalHolidayRepository.findById(holidayId)).isEmpty();
    }

    @Test
    @DisplayName("Should return 400 when deleting non-existent holiday")
    void shouldReturn400WhenDeletingNonExistentHoliday() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        org.springframework.web.client.HttpClientErrorException.BadRequest exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(
                        baseUrl + "/" + nonExistentId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(headers),
                        String.class
                )
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("Optional holiday not found");
    }

    @Test
    @DisplayName("Should validate request with missing required fields")
    void shouldValidateRequestWithMissingRequiredFields() {
        // Given - missing name
        CreateOptionalHolidayRequest request = CreateOptionalHolidayRequest.builder()
                .date(LocalDate.of(2024, 12, 25))
                // name is missing
                .description("Christmas celebration")
                .region(Region.PUNE)
                .build();

        HttpEntity<CreateOptionalHolidayRequest> entity = new HttpEntity<>(request, headers);

        // When & Then
        org.springframework.web.client.HttpClientErrorException.BadRequest exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.postForEntity(
                        baseUrl,
                        entity,
                        String.class
                )
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should return holidays ordered by date")
    void shouldReturnHolidaysOrderedByDate() {
        // Given - create holidays in random order
        createHolidayViaApi(LocalDate.of(2024, 12, 25), "Christmas Day", "Christmas");
        createHolidayViaApi(LocalDate.of(2024, 1, 1), "New Year's Day", "New Year");
        createHolidayViaApi(LocalDate.of(2024, 7, 4), "Independence Day", "Independence");

        // When
        ResponseEntity<List<OptionalHolidayDto>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody().get(0).getDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(response.getBody().get(1).getDate()).isEqualTo(LocalDate.of(2024, 7, 4));
        assertThat(response.getBody().get(2).getDate()).isEqualTo(LocalDate.of(2024, 12, 25));
    }

    @Test
    @DisplayName("Should handle validation error for invalid UUID format")
    void shouldHandleValidationErrorForInvalidUuidFormat() {
        // When & Then
        org.springframework.web.client.HttpClientErrorException.BadRequest exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(
                        baseUrl + "/invalid-uuid",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                )
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Helper method to create a holiday via API and return its ID.
     */
    private UUID createHolidayViaApi(LocalDate date, String name, String description) {
        CreateOptionalHolidayRequest request = CreateOptionalHolidayRequest.builder()
                .date(date)
                .name(name)
                .description(description)
                .region(Region.PUNE)
                .build();

        HttpEntity<CreateOptionalHolidayRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<OptionalHolidayDto> response = restTemplate.postForEntity(
                baseUrl,
                entity,
                OptionalHolidayDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().getId();
    }
}
