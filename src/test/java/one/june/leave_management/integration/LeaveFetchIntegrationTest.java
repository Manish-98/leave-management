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
                "INSERT INTO employee (id, name, slack_display_name, slack_id, date_of_joining, active, region, created_at, updated_at) " +
                "VALUES (?, 'User 1', 'user1', 'U201', '2020-01-01', true, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER1_ID);

        jdbcTemplate.update(
                "INSERT INTO employee (id, name, slack_display_name, slack_id, date_of_joining, active, region, created_at, updated_at) " +
                "VALUES (?, 'User 2', 'user2', 'U202', '2020-01-01', true, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER2_ID);

        // Create leaves with different users and date ranges
        // Note: Using slackId as userId since the mapper looks up employees by slackId/googleId
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 12), "source-174200-2024-01-10"); // Jan
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 4, 15), LocalDate.of(2024, 4, 16), "source-174200-2024-04-15"); // Apr
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 7, 20), LocalDate.of(2024, 7, 22), "source-174200-2024-07-20"); // Jul
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 10, 5), LocalDate.of(2024, 10, 8), "source-174200-2024-10-05"); // Oct

        createLeaveViaJdbc(USER2_ID, LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 7), "source-174201-2024-02-05"); // Feb
        createLeaveViaJdbc(USER2_ID, LocalDate.of(2024, 5, 10), LocalDate.of(2024, 5, 12), "source-174201-2024-05-10"); // May

        createLeaveViaJdbc(USER1_ID, LocalDate.of(2023, 6, 15), LocalDate.of(2023, 6, 17), "source-174200-2023-06-15"); // Jun 2023
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2023, 11, 20), LocalDate.of(2023, 11, 22), "source-174200-2023-11-20"); // Nov 2023

        // Create leaves that span across date ranges for testing adjusted duration
        createLeaveViaJdbc(USER1_ID, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 10), "source-174200-2024-03-01"); // 10 days
        createLeaveViaJdbc(USER2_ID, LocalDate.of(2024, 3, 5), LocalDate.of(2024, 3, 15), "source-174201-2024-03-05"); // 11 days
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
        // Should return all created leaves (10 total)
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"totalElements\":10");
    }

    @Test
    void fetchLeavesByUserIdShouldReturnOnlyUserLeaves() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userName=User 1",  // Search by employee name
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // User1 should have 7 leaves
        // Check that employee object is present with slackId
        assertThat(response.getBody()).contains("\"employee\":{");
        assertThat(response.getBody()).contains("\"slackId\":\"U201\"");
    }

    @Test
    void fetchLeavesByDateRangeShouldReturnOnlyLeavesInRange() {
        // When - Query for Q1 2024 (Jan 1 to Mar 31)
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?startDate=2024-01-01&endDate=2024-03-31",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return Q1 2024 leaves (4 total: 2 in Jan, 1 in Feb, 1 spanning March)
        assertThat(response.getBody()).contains("\"totalElements\":4");
    }

    @Test
    void fetchLeavesByDateRangeShouldIncludeLeavesThatSpanBeyondRange() {
        // When - Query for first half of March (Mar 1 to Mar 10)
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?startDate=2024-03-01&endDate=2024-03-10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return 2 leaves that overlap with this range:
        // - User1 leave: Mar 1-10 (fully within range)
        // - User2 leave: Mar 5-15 (overlaps with range)
        assertThat(response.getBody()).contains("\"totalElements\":2");
    }

    @Test
    void fetchLeavesByDateRangeShouldCalculateAdjustedDuration() {
        // When - Query for first week of March (Mar 1 to Mar 7)
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?startDate=2024-03-01&endDate=2024-03-07",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // User1's leave (Mar 1-10) should show durationInDays = 7 (only days within query range)
        // User2's leave (Mar 5-15) should show durationInDays = 3 (only days within query range: Mar 5-7)
        String body = response.getBody();
        assertThat(body).contains("\"durationInDays\":7.0");  // User1: Mar 1-10, but query is Mar 1-7 = 7 days
        assertThat(body).contains("\"durationInDays\":3.0");  // User2: Mar 5-15, but query is Mar 1-7 = 3 days (Mar 5, 6, 7)

        // Verify that actual dates are not modified
        assertThat(body).contains("\"startDate\":\"2024-03-01\",\"endDate\":\"2024-03-10\""); // User1's actual dates
        assertThat(body).contains("\"startDate\":\"2024-03-05\",\"endDate\":\"2024-03-15\""); // User2's actual dates
    }

    @Test
    void fetchLeavesByUserAndDateRangeShouldReturnFilteredResults() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userName=User 1&startDate=2024-01-01&endDate=2024-12-31",  // Search by employee name
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return user1's 2024 leaves (5 total)
        assertThat(response.getBody()).contains("\"totalElements\":5");
        // Check that employee object is present with slackId
        assertThat(response.getBody()).contains("\"employee\":{");
        assertThat(response.getBody()).contains("\"slackId\":\"U201\"");
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
        assertThat(response.getBody()).contains("\"totalElements\":10");
        assertThat(response.getBody()).contains("\"totalPages\":5");
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
        // Most recent first: Oct 2024, Jul 2024, May 2024, Mar 2024, etc.
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
                baseUrl + "?userName=nonexistent",
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
    void fetchLeavesByDateRangeWithNoResultsShouldReturnEmptyPage() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?startDate=2025-01-01&endDate=2025-12-31",
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
    void fetchLeavesWithOnlyStartDateShouldFailValidation() {
        // When & Then
        // Should get bad request due to validation
        // RestTemplate throws HttpClientErrorException for 4xx responses
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(
                        baseUrl + "?startDate=2024-01-01",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                )
        );
    }

    @Test
    void fetchLeavesWithOnlyEndDateShouldFailValidation() {
        // When & Then
        // Should get bad request due to validation
        // RestTemplate throws HttpClientErrorException for 4xx responses
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(
                        baseUrl + "?endDate=2024-12-31",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                )
        );
    }

    @Test
    void fetchLeavesWithStartDateAfterEndDateShouldFailValidation() {
        // When & Then
        // Should get bad request due to validation
        // RestTemplate throws HttpClientErrorException for 4xx responses
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(
                        baseUrl + "?startDate=2024-12-31&endDate=2024-01-01",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                )
        );
    }

    @Test
    void fetchLeavesForDifferentDateRanges() {
        // Q1 2024
        ResponseEntity<String> q1Response = restTemplate.exchange(
                baseUrl + "?startDate=2024-01-01&endDate=2024-03-31",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(q1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(q1Response.getBody()).contains("\"totalElements\":4");

        // Q2 2024
        ResponseEntity<String> q2Response = restTemplate.exchange(
                baseUrl + "?startDate=2024-04-01&endDate=2024-06-30",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(q2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(q2Response.getBody()).contains("\"totalElements\":2");

        // Full year 2024
        ResponseEntity<String> yearResponse = restTemplate.exchange(
                baseUrl + "?startDate=2024-01-01&endDate=2024-12-31",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(yearResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(yearResponse.getBody()).contains("\"totalElements\":8");
    }

    @Test
    void fetchLeavesForMultipleYears() {
        // 2023
        ResponseEntity<String> response2023 = restTemplate.exchange(
                baseUrl + "?startDate=2023-01-01&endDate=2023-12-31",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(response2023.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2023.getBody()).contains("\"totalElements\":2");

        // 2024
        ResponseEntity<String> response2024 = restTemplate.exchange(
                baseUrl + "?startDate=2024-01-01&endDate=2024-12-31",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(response2024.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2024.getBody()).contains("\"totalElements\":8");
    }

    @Test
    void fetchLeavesWithoutDateRangeShouldShowActualDuration() {
        // When - No date range filter
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userName=User 1",  // Search by employee name
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();

        // Should show durations excluding weekends
        assertThat(body).contains("\"startDate\":\"2024-03-01\",\"endDate\":\"2024-03-10\"");
        // Duration should be 6 business days (excluding weekends: 2 weekends in March 1-10)
        assertThat(body).contains("\"durationInDays\":6.0");
    }

    @Test
    void fetchLeavesByNameShouldReturnMatchingEmployeeLeaves() {
        // When - Search by employee name
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userName=User 1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // User1 should have 7 leaves
        assertThat(response.getBody()).contains("\"totalElements\":7");
        // Check that employee object is present with name
        assertThat(response.getBody()).contains("\"employee\":{");
        assertThat(response.getBody()).contains("\"name\":\"User 1\"");
        assertThat(response.getBody()).contains("\"slackId\":\"U201\"");
    }

    @Test
    void fetchLeavesBySlackDisplayNameShouldReturnMatchingEmployeeLeaves() {
        // When - Search by slack display name
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userName=user1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // User1 should have 7 leaves
        assertThat(response.getBody()).contains("\"totalElements\":7");
        // Check that employee object is present
        assertThat(response.getBody()).contains("\"employee\":{");
        assertThat(response.getBody()).contains("\"name\":\"User 1\"");
        assertThat(response.getBody()).contains("\"slackDisplayName\":\"user1\"");
    }

    @Test
    void fetchLeavesByPartialNameShouldReturnMatchingEmployeeLeaves() {
        // When - Search by partial name
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userName=User",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Both "User 1" and "User 2" match, so should return all 10 leaves
        assertThat(response.getBody()).contains("\"totalElements\":10");
    }

    @Test
    void fetchLeavesByNonExistentNameShouldReturnEmptyPage() {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userName=NonExistent",
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
    void fetchLeavesByNameAndDateRangeShouldReturnFilteredResults() {
        // When - Search by name with date range
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userName=User 1&startDate=2024-01-01&endDate=2024-12-31",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should return user1's 2024 leaves (5 total)
        assertThat(response.getBody()).contains("\"totalElements\":5");
        // Check that employee object is present
        assertThat(response.getBody()).contains("\"employee\":{");
        assertThat(response.getBody()).contains("\"name\":\"User 1\"");
    }
}
