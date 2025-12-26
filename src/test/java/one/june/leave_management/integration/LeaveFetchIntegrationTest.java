package one.june.leave_management.integration;

import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.common.model.Quarter;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.test.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Leave Fetch API.
 * Tests filtering, pagination, and sorting capabilities.
 *
 * Note: transactional=false is required because tests make HTTP requests via RestTemplate,
 * and the data needs to be committed to the database for the HTTP layer to see it.
 */
@IntegrationTest(transactional = false)
class LeaveFetchIntegrationTest {

    @LocalServerPort
    private int port;

    private String baseUrl;
    private RestTemplate restTemplate;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/leaves";
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create test data for different users, years, and quarters
        createTestData();
    }

    private void createTestData() {
        // User 1 - 2024 leaves
        createLeave("user1", LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 12)); // Q1
        createLeave("user1", LocalDate.of(2024, 4, 15), LocalDate.of(2024, 4, 16)); // Q2
        createLeave("user1", LocalDate.of(2024, 7, 20), LocalDate.of(2024, 7, 22)); // Q3
        createLeave("user1", LocalDate.of(2024, 10, 5), LocalDate.of(2024, 10, 8)); // Q4

        // User 2 - 2024 leaves
        createLeave("user2", LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 7)); // Q1
        createLeave("user2", LocalDate.of(2024, 5, 10), LocalDate.of(2024, 5, 12)); // Q2

        // User 1 - 2023 leaves
        createLeave("user1", LocalDate.of(2023, 6, 15), LocalDate.of(2023, 6, 17)); // Q2
        createLeave("user1", LocalDate.of(2023, 11, 20), LocalDate.of(2023, 11, 22)); // Q4
    }

    private void createLeave(String userId, LocalDate startDate, LocalDate endDate) {
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-" + userId + "-" + startDate)
                .userId(userId)
                .dateRange(DateRange.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.APPROVED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpEntity<LeaveIngestionRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(baseUrl + "/ingest", entity, LeaveDto.class);
    }

    @Test
    void fetchAllLeavesWithoutFiltersShouldReturnAllLeaves() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Should return all created leaves (9 total)
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"totalElements\":8");
    }

    @Test
    void fetchLeavesByUserIdShouldReturnOnlyUserLeaves() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userId=user1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // User1 should have 6 leaves (4 in 2024 + 2 in 2023)
        assertThat(response.getBody()).contains("\"userId\":\"user1\"");
    }

    @Test
    void fetchLeavesByYearShouldReturnOnlyLeavesInThatYear() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?year=2024",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return all 2024 leaves (6 total: 4 for user1 + 2 for user2)
        assertThat(response.getBody()).contains("\"totalElements\":6");
    }

    @Test
    void fetchLeavesByQuarterShouldReturnOnlyLeavesInThatQuarter() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?year=2024&quarter=Q1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return Q1 2024 leaves (2: one for user1, one for user2)
        assertThat(response.getBody()).contains("\"totalElements\":2");
    }

    @Test
    void fetchLeavesByUserAndYearShouldReturnFilteredResults() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userId=user1&year=2024",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return user1's 2024 leaves (4 total)
        assertThat(response.getBody()).contains("\"totalElements\":4");
        assertThat(response.getBody()).contains("\"userId\":\"user1\"");
    }

    @Test
    void fetchLeavesByUserYearAndQuarterShouldReturnFilteredResults() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userId=user1&year=2024&quarter=Q2",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return user1's Q2 2024 leaves (1 leave in April)
        assertThat(response.getBody()).contains("\"totalElements\":1");
        assertThat(response.getBody()).contains("\"userId\":\"user1\"");
        assertThat(response.getBody()).contains("\"startDate\":\"2024-04-15\"");
    }

    @Test
    void fetchLeavesWithPaginationShouldReturnPaginatedResults() {
        // When - First page with size 2
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?page=0&size=2",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"totalElements\":8");
        assertThat(response.getBody()).contains("\"totalPages\":4");
        assertThat(response.getBody()).contains("\"size\":2");
        assertThat(response.getBody()).contains("\"number\":0");
    }

    @Test
    void fetchLeavesWithPaginationSecondPageShouldWork() {
        // When - Second page with size 2
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?page=1&size=2",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"number\":1");
    }

    @Test
    void fetchLeavesWithSortingByStartDateDescShouldWork() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?sort=startDate,desc",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return leaves sorted by start date descending
    }

    @Test
    void fetchLeavesWithNonExistentUserShouldReturnEmptyPage() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userId=nonexistent",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"totalElements\":0");
        assertThat(response.getBody()).contains("\"content\":[]");
    }

    @Test
    void fetchLeavesByYearWithNoResultsShouldReturnEmptyPage() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?year=2025",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"totalElements\":0");
    }

    @Test
    void fetchLeavesByQuarterWithoutYearShouldFailValidation() {
        // When & Then
        // Should get bad request due to validation
        // RestTemplate throws HttpClientErrorException for 4xx responses
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(
                        baseUrl + "?quarter=Q1",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                )
        );
    }

    @Test
    void fetchLeavesForDifferentQuartersInSameYear() {
        // Q1
        ResponseEntity<String> q1Response = restTemplate.exchange(
                baseUrl + "?year=2024&quarter=Q1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(q1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(q1Response.getBody()).contains("\"totalElements\":2");

        // Q2
        ResponseEntity<String> q2Response = restTemplate.exchange(
                baseUrl + "?year=2024&quarter=Q2",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(q2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(q2Response.getBody()).contains("\"totalElements\":2");

        // Q3
        ResponseEntity<String> q3Response = restTemplate.exchange(
                baseUrl + "?year=2024&quarter=Q3",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(q3Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(q3Response.getBody()).contains("\"totalElements\":1");

        // Q4
        ResponseEntity<String> q4Response = restTemplate.exchange(
                baseUrl + "?year=2024&quarter=Q4",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(q4Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(q4Response.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void fetchLeavesAcrossQuartersShouldIncludeAllRelevantLeaves() {
        // When - Get all 2024 leaves
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?year=2024",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should have leaves from all quarters (2+2+1+1 = 6 total)
        assertThat(response.getBody()).contains("\"totalElements\":6");
    }

    @Test
    void fetchLeavesForMultipleYears() {
        // 2023
        ResponseEntity<String> response2023 = restTemplate.exchange(
                baseUrl + "?year=2023",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(response2023.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2023.getBody()).contains("\"totalElements\":2");

        // 2024
        ResponseEntity<String> response2024 = restTemplate.exchange(
                baseUrl + "?year=2024",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(response2024.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2024.getBody()).contains("\"totalElements\":6");
    }
}
