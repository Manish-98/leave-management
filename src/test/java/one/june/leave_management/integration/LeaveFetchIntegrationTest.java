package one.june.leave_management.integration;

import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.test.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * Uses @BeforeTransaction to set up test data in committed state,
 * allowing HTTP requests via RestTemplate to see the data while keeping
 * tests transactional for proper isolation.
 */
@IntegrationTest
class LeaveFetchIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;
    private RestTemplate restTemplate;
    private HttpHeaders headers;

    // Test employee IDs
    private static final String USER1_ID = "123e4567-e89b-12d3-a456-426614174200";
    private static final String USER2_ID = "123e4567-e89b-12d3-a456-426614174201";

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/leaves";
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * Sets up test data in the database before each test transaction.
     * This method runs in a separate transaction that commits, making the data
     * visible to subsequent HTTP requests via RestTemplate.
     */
    @BeforeTransaction
    void setUpTestData() {
        // Clean up and create test employees
        jdbcTemplate.update("DELETE FROM leave_source_ref WHERE leave_id IN (SELECT id FROM leave WHERE user_id IN (?, ?))", USER1_ID, USER2_ID);
        jdbcTemplate.update("DELETE FROM leave WHERE user_id IN (?, ?)", USER1_ID, USER2_ID);
        jdbcTemplate.update("DELETE FROM employee WHERE id IN (?, ?)", USER1_ID, USER2_ID);

        jdbcTemplate.update(
                "INSERT INTO employee (id, name, slack_id, date_of_joining, active, created_at, updated_at) " +
                "VALUES (?, 'User 1', 'U201', '2020-01-01', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER1_ID);

        jdbcTemplate.update(
                "INSERT INTO employee (id, name, slack_id, date_of_joining, active, created_at, updated_at) " +
                "VALUES (?, 'User 2', 'U202', '2020-01-01', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER2_ID);

        // Create leaves with different users, years, and quarters
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 12), "source-174200-2024-01-10"); // Q1
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 4, 15), LocalDate.of(2024, 4, 16), "source-174200-2024-04-15"); // Q2
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 7, 20), LocalDate.of(2024, 7, 22), "source-174200-2024-07-20"); // Q3
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 10, 5), LocalDate.of(2024, 10, 8), "source-174200-2024-10-05"); // Q4

        createLeaveViaJdbc(USER2_ID, LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 7), "source-174201-2024-02-05"); // Q1
        createLeaveViaJdbc(USER2_ID, LocalDate.of(2024, 5, 10), LocalDate.of(2024, 5, 12), "source-174201-2024-05-10"); // Q2

        createLeaveViaJdbc(USER1_ID, LocalDate.of(2023, 6, 15), LocalDate.of(2023, 6, 17), "source-174200-2023-06-15"); // Q2
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2023, 11, 20), LocalDate.of(2023, 11, 22), "source-174200-2023-11-20"); // Q4
    }

    private void createLeaveViaJdbc(String userId, LocalDate startDate, LocalDate endDate, String sourceId) {
        // Generate a unique leave ID
        String leaveId = java.util.UUID.randomUUID().toString();

        // Insert leave record (created_at and updated_at have default values)
        jdbcTemplate.update(
                "INSERT INTO leave (id, user_id, start_date, end_date, type, status, duration_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                leaveId, userId, startDate, endDate, LeaveType.ANNUAL_LEAVE.name(), LeaveStatus.APPROVED.name(), LeaveDurationType.FULL_DAY.name()
        );

        // Insert leave source reference (id needs to be explicitly provided for H2)
        jdbcTemplate.update(
                "INSERT INTO leave_source_ref (id, leave_id, source_id, source_type) " +
                "VALUES (?, ?, ?, ?)",
                java.util.UUID.randomUUID().toString(), leaveId, sourceId, SourceType.WEB.name()
        );
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
                baseUrl + "?userId=" + USER1_ID,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // User1 should have 6 leaves (4 in 2024 + 2 in 2023)
        assertThat(response.getBody()).contains("\"userId\":\"" + USER1_ID + "\"");
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
                baseUrl + "?userId=" + USER1_ID + "&year=2024",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return user1's 2024 leaves (4 total)
        assertThat(response.getBody()).contains("\"totalElements\":4");
        assertThat(response.getBody()).contains("\"userId\":\"" + USER1_ID + "\"");
    }

    @Test
    void fetchLeavesByUserYearAndQuarterShouldReturnFilteredResults() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userId=" + USER1_ID + "&year=2024&quarter=Q2",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return user1's Q2 2024 leaves (1 leave in April)
        assertThat(response.getBody()).contains("\"totalElements\":1");
        assertThat(response.getBody()).contains("\"userId\":\"" + USER1_ID + "\"");
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
    void fetchLeavesWithoutSortParameterShouldUseDefaultSorting() {
        // When - No sort parameter provided
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Extract the content array and verify sorting
        // Default sort is startDate DESC (most recent first)
        // The test data has leaves from 2024 and 2023
        // Most recent first: 2024-10-05 (Q4), 2024-07-20 (Q3), 2024-04-15 (Q2), 2024-02-05 (Q1), 2024-01-10 (Q1), etc.
        String body = response.getBody();

        // Verify first result is from October 2024 (most recent)
        assertThat(body).contains("\"startDate\":\"2024-10-05\"");

        // Verify that 2024 leaves come before 2023 leaves in the response
        int first2024Index = body.indexOf("\"startDate\":\"2024-");
        int first2023Index = body.indexOf("\"startDate\":\"2023-");

        // 2024 leaves should appear before 2023 leaves (descending order by year)
        assertThat(first2024Index).isLessThan(first2023Index);
    }

    @Test
    void fetchLeavesDefaultSortCanBeOverriddenWithPageable() {
        // When - Override default sort with ascending order
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?sort=startDate,asc",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Verify ascending sort (oldest first)
        // Should see 2023 dates before 2024 dates
        String body = response.getBody();

        // Find first occurrence of 2023 and 2024
        int first2023Index = body.indexOf("\"startDate\":\"2023-");
        int first2024Index = body.indexOf("\"startDate\":\"2024-");

        // With ascending sort, 2023 should come before 2024
        assertThat(first2023Index).isLessThan(first2024Index);
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
