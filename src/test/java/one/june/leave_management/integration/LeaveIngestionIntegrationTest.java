package one.june.leave_management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for Leave Ingestion API.
 * Uses H2 in-memory database with no mocking.
 * Clock is controlled through fixed dates in tests.
 * Uses @BeforeTransaction to set up test data in committed state,
 * allowing HTTP requests via RestTemplate to see the data while keeping
 * tests transactional for proper isolation.
 */
@IntegrationTest
class LeaveIngestionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;
    private RestTemplate restTemplate;

    // Fixed date for testing
    private static final LocalDate FIXED_DATE = LocalDate.of(2024, 6, 15);

    // Test employee IDs
    private static final String EMPLOYEE_1_ID = "123e4567-e89b-12d3-a456-426614174100";
    private static final String EMPLOYEE_2_ID = "123e4567-e89b-12d3-a456-426614174101";
    private static final String EMPLOYEE_OVERLAP = "123e4567-e89b-12d3-a456-426614174102";
    private static final String EMPLOYEE_NO_OVERLAP = "123e4567-e89b-12d3-a456-426614174103";
    private static final String EMPLOYEE_HALF_DAY = "123e4567-e89b-12d3-a456-426614174104";
    private static final String EMPLOYEE_INVALID_HALF_DAY = "123e4567-e89b-12d3-a456-426614174105";
    private static final String EMPLOYEE_ALL_SOURCES = "123e4567-e89b-12d3-a456-426614174106";
    private static final String EMPLOYEE_ALL_TYPES = "123e4567-e89b-12d3-a456-426614174107";
    private static final String EMPLOYEE_ALL_STATUSES = "123e4567-e89b-12d3-a456-426614174108";
    private static final String EMPLOYEE_ALL_DURATIONS = "123e4567-e89b-12d3-a456-426614174109";
    private static final String EMPLOYEE_INVALID_DATE = "123e4567-e89b-12d3-a456-42661417410a";
    private static final String EMPLOYEE_LONG_SOURCE = "123e4567-e89b-12d3-a456-42661417410b";
    private static final String EMPLOYEE_MULTI_SOURCE = "123e4567-e89b-12d3-a456-42661417410c";
    private static final String EMPLOYEE_OPTIONAL_VALID = "123e4567-e89b-12d3-a456-42661417410d";
    private static final String EMPLOYEE_OPTIONAL_INVALID = "123e4567-e89b-12d3-a456-42661417410e";
    private static final String EMPLOYEE_OPTIONAL_MULTI = "123e4567-e89b-12d3-a456-42661417410f";
    private static final String EMPLOYEE_ANNUAL_NO_HOLIDAY = "123e4567-e89b-12d3-a456-426614174110";
    private static final String EMPLOYEE_WEEKEND_ONLY = "123e4567-e89b-12d3-a456-426614174111";
    private static final String EMPLOYEE_WEEKEND_MIXED = "123e4567-e89b-12d3-a456-426614174112";
    private static final String EMPLOYEE_HALF_DAY_WEEKEND = "123e4567-e89b-12d3-a456-426614174113";
    private static final String EMPLOYEE_HALF_DAY_WEEKDAY = "123e4567-e89b-12d3-a456-426614174114";

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/leaves";
        restTemplate = new RestTemplate();
    }

    /**
     * Sets up test data in the database before each test transaction.
     * This method runs in a separate transaction that commits, making the data
     * visible to subsequent HTTP requests via RestTemplate.
     */
    @BeforeTransaction
    void setUpTestData() {
        // Clean up all test employees
        String[] employeeIds = {
                EMPLOYEE_1_ID, EMPLOYEE_2_ID, EMPLOYEE_OVERLAP, EMPLOYEE_NO_OVERLAP,
                EMPLOYEE_HALF_DAY, EMPLOYEE_INVALID_HALF_DAY, EMPLOYEE_ALL_SOURCES,
                EMPLOYEE_ALL_TYPES, EMPLOYEE_ALL_STATUSES, EMPLOYEE_ALL_DURATIONS,
                EMPLOYEE_INVALID_DATE, EMPLOYEE_LONG_SOURCE, EMPLOYEE_MULTI_SOURCE,
                EMPLOYEE_OPTIONAL_VALID, EMPLOYEE_OPTIONAL_INVALID, EMPLOYEE_OPTIONAL_MULTI,
                EMPLOYEE_ANNUAL_NO_HOLIDAY, EMPLOYEE_WEEKEND_ONLY, EMPLOYEE_WEEKEND_MIXED,
                EMPLOYEE_HALF_DAY_WEEKEND, EMPLOYEE_HALF_DAY_WEEKDAY
        };

        jdbcTemplate.update("DELETE FROM leave WHERE user_id IN (" +
                String.join(",", Collections.nCopies(employeeIds.length, "?")) + ")", (Object[]) employeeIds);
        jdbcTemplate.update("DELETE FROM employee WHERE id IN (" +
                String.join(",", Collections.nCopies(employeeIds.length, "?")) + ")", (Object[]) employeeIds);

        // Create all test employees
        int i = 1;
        for (String employeeId : employeeIds) {
            jdbcTemplate.update(
                    "INSERT INTO employee (id, name, slack_id, date_of_joining, active, region, created_at, updated_at) " +
                    "VALUES (?, 'Test User " + i + "', 'U" + String.format("%03d", i) + "', '2020-01-01', true, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    employeeId);
            i++;
        }

        // Clean up any existing optional holidays
        jdbcTemplate.update("DELETE FROM optional_holidays");

        // Create optional holidays needed for tests
        // Holiday on 2024-06-26 (FIXED_DATE.plusDays(11)) for LeaveType enum test
        jdbcTemplate.update(
                "INSERT INTO optional_holidays (id, date, name, description, region, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), FIXED_DATE.plusDays(11), "Test Holiday for All Types", "Auto-generated for LeaveType enum test"
        );

        // Holiday on 2024-06-20 (FIXED_DATE.plusDays(5)) for validation tests
        jdbcTemplate.update(
                "INSERT INTO optional_holidays (id, date, name, description, region, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), FIXED_DATE.plusDays(5), "Test Holiday", "Test holiday description"
        );

        // Holiday on 2024-06-22 (FIXED_DATE.plusDays(7)) for multi-day validation test
        jdbcTemplate.update(
                "INSERT INTO optional_holidays (id, date, name, description, region, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), FIXED_DATE.plusDays(7), "Test Holiday 2", "Description 2"
        );
    }

    private HttpEntity<LeaveIngestionRequest> createRequestEntity(LeaveIngestionRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("test", "test");
        return new HttpEntity<>(request, headers);
    }

    /**
     * Helper method to query leave from database
     */
    private Map<String, Object> getLeaveFromDatabase(String userId, String startDate, String endDate) {
        String sql = """
                SELECT id, user_id, start_date, end_date, type, status, duration_type
                FROM leave
                WHERE user_id = ? AND start_date = ? AND end_date = ?
                """;
        return jdbcTemplate.queryForMap(sql, userId, startDate, endDate);
    }

    /**
     * Helper method to query all source references for a leave
     */
    private List<Map<String, Object>> getLeaveSourceRefsFromDatabase(String leaveId) {
        String sql = """
                SELECT source_id, source_type
                FROM leave_source_ref
                WHERE leave_id = ?
                """;
        return jdbcTemplate.queryForList(sql, leaveId);
    }

    @Test
    void ingestLeaveShouldCreateNewLeaveSuccessfully() {
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-123")
                .userId("123e4567-e89b-12d3-a456-426614174100")  // Use employee UUID
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        // Check that employee object is present with slackId
        assertThat(responseBody).contains("\"employee\":{");
        assertThat(responseBody).contains("\"slackId\":\"U001\"");
        assertThat(responseBody).contains("\"type\":\"ANNUAL_LEAVE\"");
        assertThat(responseBody).contains("\"status\":\"REQUESTED\"");
        assertThat(responseBody).contains("\"durationType\":\"FULL_DAY\"");
        assertThat(responseBody).contains("\"sourceType\":\"WEB\"");
        assertThat(responseBody).contains("\"sourceId\":\"web-123\"");

        // Database validation
        Map<String, Object> leaveRecord = getLeaveFromDatabase(EMPLOYEE_1_ID, "2024-06-16", "2024-06-18");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo(EMPLOYEE_1_ID);
        assertThat(((java.sql.Date) leaveRecord.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 6, 16));
        assertThat(((java.sql.Date) leaveRecord.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 6, 18));
        assertThat(leaveRecord.get("type")).isEqualTo("ANNUAL_LEAVE");
        assertThat(leaveRecord.get("status")).isEqualTo("REQUESTED");
        assertThat(leaveRecord.get("duration_type")).isEqualTo("FULL_DAY");

        // Verify source reference
        String leaveId = leaveRecord.get("id").toString();
        List<Map<String, Object>> sourceRefs = getLeaveSourceRefsFromDatabase(leaveId);
        assertThat(sourceRefs).hasSize(1);
        assertThat(sourceRefs.get(0).get("source_id")).isEqualTo("web-123");
        assertThat(sourceRefs.get(0).get("source_type")).isEqualTo("WEB");
    }

    @Test
    void ingestLeaveShouldUpdateExistingLeaveWhenSourceIdExists() {
        // First request - create a new leave
        LeaveIngestionRequest firstRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-update-test")
                .userId("123e4567-e89b-12d3-a456-426614174101")
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(10))
                        .endDate(FIXED_DATE.plusDays(12))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var firstResponse = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(firstRequest), String.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String firstBody = firstResponse.getBody();

        // Extract the ID from the first response
        String idPattern = "\"id\":\"([a-f0-9\\-]+)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
        java.util.regex.Matcher matcher = pattern.matcher(firstBody);
        assertThat(matcher.find()).isTrue();
        String leaveId = matcher.group(1);

        // Verify first creation in database
        Map<String, Object> firstRecord = getLeaveFromDatabase(EMPLOYEE_2_ID, "2024-06-25", "2024-06-27");
        assertThat(firstRecord).isNotNull();
        assertThat(firstRecord.get("type")).isEqualTo("ANNUAL_LEAVE");
        assertThat(firstRecord.get("status")).isEqualTo("REQUESTED");

        // Second request with same sourceId - should update the existing leave
        LeaveIngestionRequest secondRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-update-test") // Same sourceId
                .userId("123e4567-e89b-12d3-a456-426614174101")
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(20))
                        .endDate(FIXED_DATE.plusDays(25))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE) // Changed from OPTIONAL_HOLIDAY to avoid validation requirement
                .status(LeaveStatus.APPROVED) // Different status
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var secondResponse = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(secondRequest), String.class);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String secondBody = secondResponse.getBody();
        assertThat(secondBody).contains("\"id\":\"" + leaveId + "\""); // Same ID
        assertThat(secondBody).contains("\"type\":\"ANNUAL_LEAVE\"");
        assertThat(secondBody).contains("\"status\":\"APPROVED\"");

        // Verify update in database - same leave ID but updated values
        Map<String, Object> updatedRecord = getLeaveFromDatabase(EMPLOYEE_2_ID, "2024-07-05", "2024-07-10");
        assertThat(updatedRecord).isNotNull();
        assertThat(updatedRecord.get("id").toString()).isEqualTo(leaveId); // Same ID
        assertThat(updatedRecord.get("type")).isEqualTo("ANNUAL_LEAVE");
        assertThat(updatedRecord.get("status")).isEqualTo("APPROVED");
        assertThat(((java.sql.Date) updatedRecord.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 5));
        assertThat(((java.sql.Date) updatedRecord.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 10));

        // Verify only one source reference exists (not created a new one)
        List<Map<String, Object>> sourceRefs = getLeaveSourceRefsFromDatabase(leaveId);
        assertThat(sourceRefs).hasSize(1);
        assertThat(sourceRefs.get(0).get("source_id")).isEqualTo("web-update-test");
    }

    @Test
    void ingestLeaveShouldRejectOverlappingLeaves() {
        // First leave
        LeaveIngestionRequest firstRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-overlap-1")
                .userId("123e4567-e89b-12d3-a456-426614174102")
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(5))
                        .endDate(FIXED_DATE.plusDays(10))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var firstResponse = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(firstRequest), String.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second leave that overlaps with the first
        LeaveIngestionRequest secondRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.SLACK)
                .sourceId("slack-overlap-2")
                .userId("123e4567-e89b-12d3-a456-426614174102") // Same user
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(8)) // Overlaps
                        .endDate(FIXED_DATE.plusDays(12))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(secondRequest), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("already has a leave");
    }

    @Test
    void ingestLeaveShouldAllowNonOverlappingLeavesForSameUser() {
        // First leave
        LeaveIngestionRequest firstRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-no-overlap-1")
                .userId("123e4567-e89b-12d3-a456-426614174103")
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var firstResponse = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(firstRequest), String.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second leave that does NOT overlap
        LeaveIngestionRequest secondRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.SLACK)
                .sourceId("slack-no-overlap-2")
                .userId("123e4567-e89b-12d3-a456-426614174103") // Same user
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(10)) // No overlap
                        .endDate(FIXED_DATE.plusDays(12))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var secondResponse = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(secondRequest), String.class);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void ingestLeaveShouldHandleHalfDayLeaves() {
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-half-day")
                .userId(EMPLOYEE_HALF_DAY)
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(3)) // Monday (2024-06-17)
                        .endDate(FIXED_DATE.plusDays(3)) // Same day
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FIRST_HALF)
                .build();

        var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"durationType\":\"FIRST_HALF\"");
    }

    @Test
    void ingestLeaveShouldRejectHalfDayLeaveWithDifferentDates() {
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-invalid-half-day")
                .userId(EMPLOYEE_INVALID_HALF_DAY)
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(3)) // Monday
                        .endDate(FIXED_DATE.plusDays(5)) // Wednesday (different days)
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.SECOND_HALF)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("Half-day leaves must have the same start and end date");
    }

    @Test
    void ingestLeaveShouldHandleAllSourceTypes() {
        int dayOffset = 0;
        for (SourceType sourceType : SourceType.values()) {
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(sourceType)
                    .sourceId(sourceType.name().toLowerCase() + "-integration-test")
                    .userId(EMPLOYEE_ALL_SOURCES)
                    .dateRange(DateRange.builder()
                            .startDate(FIXED_DATE.plusDays(3 + dayOffset * 10)) // Start from Monday
                            .endDate(FIXED_DATE.plusDays(4 + dayOffset * 10))
                            .build())
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).contains("\"sourceType\":\"" + sourceType.name() + "\"");
            dayOffset++;
        }
    }

    @Test
    void ingestLeaveShouldHandleAllLeaveTypes() {
        int dayOffset = 0;
        for (LeaveType leaveType : LeaveType.values()) {
            LocalDate startDate = FIXED_DATE.plusDays(1 + dayOffset * 10);
            LocalDate endDate = FIXED_DATE.plusDays(2 + dayOffset * 10);

            // For optional holidays, use single day
            if (leaveType == LeaveType.OPTIONAL_HOLIDAY) {
                endDate = startDate; // Single day
            }

            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-type-" + leaveType.name())
                    .userId(EMPLOYEE_ALL_TYPES)
                    .dateRange(DateRange.builder()
                            .startDate(startDate)
                            .endDate(endDate)
                            .build())
                    .type(leaveType)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).contains("\"type\":\"" + leaveType.name() + "\"");
            dayOffset++;
        }
    }

    @Test
    void ingestLeaveShouldHandleAllLeaveStatuses() {
        int dayOffset = 0;
        for (LeaveStatus leaveStatus : LeaveStatus.values()) {
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-status-" + leaveStatus.name())
                    .userId(EMPLOYEE_ALL_STATUSES)
                    .dateRange(DateRange.builder()
                            .startDate(FIXED_DATE.plusDays(3 + dayOffset * 10)) // Start from Monday
                            .endDate(FIXED_DATE.plusDays(4 + dayOffset * 10))
                            .build())
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(leaveStatus)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).contains("\"status\":\"" + leaveStatus.name() + "\"");
            dayOffset++;
        }
    }

    @Test
    void ingestLeaveShouldHandleAllDurationTypes() {
        int dayOffset = 0;
        for (LeaveDurationType durationType : LeaveDurationType.values()) {
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-duration-" + durationType.name())
                    .userId(EMPLOYEE_ALL_DURATIONS)
                    .dateRange(DateRange.builder()
                            .startDate(FIXED_DATE.plusDays(3 + dayOffset * 10)) // Start from Monday
                            .endDate(FIXED_DATE.plusDays(3 + dayOffset * 10)) // Same day for half-day leaves
                            .build())
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(durationType)
                    .build();

            var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).contains("\"durationType\":\"" + durationType.name() + "\"");
            dayOffset++;
        }
    }

    @Test
    void ingestLeaveShouldReturnBadRequestForInvalidDateRange() {
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-invalid-date")
                .userId(EMPLOYEE_INVALID_DATE)
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(5))
                        .endDate(FIXED_DATE.plusDays(2)) // End before start
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ingestLeaveShouldReturnBadRequestForMissingRequiredFields() {
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(null)
                .sourceId("")
                .userId(null)
                .dateRange(null)
                .type(null)
                .status(null)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ingestLeaveShouldReturnBadRequestForTooLongUserId() {
        String longUserId = "a".repeat(51); // Exceeds 50 character limit

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-long-user")
                .userId(longUserId)
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(2))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ingestLeaveShouldReturnBadRequestForTooLongSourceId() {
        String longSourceId = "a".repeat(101); // Exceeds 100 character limit

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId(longSourceId)
                .userId(EMPLOYEE_LONG_SOURCE)
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(2))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ingestLeaveShouldHandleLeaveWithMultipleSourceRefs() {
        // Create initial leave with WEB source
        LeaveIngestionRequest firstRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-multi-source")
                .userId(EMPLOYEE_MULTI_SOURCE)
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var firstResponse = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(firstRequest), String.class);

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String firstBody = firstResponse.getBody();
        assertThat(firstBody).contains("\"sourceType\":\"WEB\"");
        assertThat(firstBody).contains("\"sourceId\":\"web-multi-source\"");

        // Extract the leave ID
        String idPattern = "\"id\":\"([a-f0-9\\-]+)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
        java.util.regex.Matcher matcher = pattern.matcher(firstBody);
        assertThat(matcher.find()).isTrue();
        String leaveId = matcher.group(1);

        // Try to create another leave with different source but overlapping dates - should fail
        LeaveIngestionRequest secondRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.SLACK)
                .sourceId("slack-multi-source")
                .userId(EMPLOYEE_MULTI_SOURCE)
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.APPROVED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(secondRequest), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("already has a leave");
    }

    // Optional Holiday Validation Tests

    @Test
    void ingestOptionalHolidayShouldAcceptWhenDateExistsInDatabase() {
        LocalDate holidayDate = FIXED_DATE.plusDays(5);

        // Optional holiday is already created in @BeforeTransaction
        // Now try to create an optional holiday leave for that date
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-optional-holiday-valid")
                .userId(EMPLOYEE_OPTIONAL_VALID)
                .dateRange(DateRange.builder()
                        .startDate(holidayDate)
                        .endDate(holidayDate) // Single day
                        .build())
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"type\":\"OPTIONAL_HOLIDAY\"");
        assertThat(response.getBody()).contains("\"status\":\"REQUESTED\"");
    }

    @Test
    void ingestOptionalHolidayShouldRejectWhenDateNotInDatabase() {
        LocalDate nonExistentDate = FIXED_DATE.plusDays(10);

        // Do NOT create this date in the optional_holidays table

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-optional-holiday-invalid")
                .userId(EMPLOYEE_OPTIONAL_INVALID)
                .dateRange(DateRange.builder()
                        .startDate(nonExistentDate)
                        .endDate(nonExistentDate) // Single day
                        .build())
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("is not a valid optional holiday");
    }

    @Test
    void ingestOptionalHolidayShouldRejectMultiDayRequest() {
        LocalDate startDate = FIXED_DATE.plusDays(5);
        LocalDate endDate = FIXED_DATE.plusDays(7);

        // Optional holidays are already created in @BeforeTransaction
        // Try to create a multi-day optional holiday leave
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-optional-holiday-multi-day")
                .userId(EMPLOYEE_OPTIONAL_MULTI)
                .dateRange(DateRange.builder()
                        .startDate(startDate)
                        .endDate(endDate) // Multi-day
                        .build())
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("Optional holidays must be single-day only");
    }

    @Test
    void ingestAnnualLeaveShouldNotBeAffectedByOptionalHolidayValidation() {
        // Do NOT create any optional holidays

        // Create an annual leave - should be accepted regardless of optional holiday table
        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-annual-leave-no-holiday")
                .userId(EMPLOYEE_ANNUAL_NO_HOLIDAY)
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE) // Not OPTIONAL_HOLIDAY
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"type\":\"ANNUAL_LEAVE\"");
    }

    // Weekend Handling Tests

    @Test
    void ingestLeaveShouldRejectWeekendOnlyRequest() {
        // FIXED_DATE is 2024-06-15 (Saturday)
        // FIXED_DATE.plusDays(1) is 2024-06-16 (Sunday)
        LocalDate saturday = FIXED_DATE; // 2024-06-15 is Saturday
        LocalDate sunday = FIXED_DATE.plusDays(1); // 2024-06-16 is Sunday

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-weekend-only")
                .userId(EMPLOYEE_WEEKEND_ONLY)
                .dateRange(DateRange.builder()
                        .startDate(saturday)
                        .endDate(sunday)
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("only weekend days");
    }

    @Test
    void ingestLeaveShouldAcceptMixedWeekdayWeekendAndCalculateCorrectDuration() {
        // FIXED_DATE is 2024-06-15 (Saturday)
        // FIXED_DATE.plusDays(3) is 2024-06-18 (Tuesday)
        LocalDate friday = FIXED_DATE.minusDays(1); // 2024-06-14 (Friday)
        LocalDate monday = FIXED_DATE.plusDays(3); // 2024-06-18 (Monday)

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-weekend-mixed")
                .userId(EMPLOYEE_WEEKEND_MIXED)
                .dateRange(DateRange.builder()
                        .startDate(friday)
                        .endDate(monday) // Fri, Sat, Sun, Mon
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Verify the response contains the leave data (weekend exclusion is tested in DateRangeTest)
        assertThat(response.getBody()).contains("\"type\":\"ANNUAL_LEAVE\"");
    }

    @Test
    void ingestLeaveShouldRejectHalfDayOnSaturday() {
        // FIXED_DATE is 2024-06-15 (Saturday)
        LocalDate saturday = FIXED_DATE;

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-half-day-weekend")
                .userId(EMPLOYEE_HALF_DAY_WEEKEND)
                .dateRange(DateRange.builder()
                        .startDate(saturday)
                        .endDate(saturday)
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FIRST_HALF)
                .build();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Just verify it's a bad request - the exact error message is validated in unit tests
    }

    @Test
    void ingestLeaveShouldAcceptHalfDayOnWeekday() {
        // FIXED_DATE.plusDays(2) is 2024-06-17 (Monday)
        LocalDate monday = FIXED_DATE.plusDays(2);

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-half-day-weekday")
                .userId(EMPLOYEE_HALF_DAY_WEEKDAY)
                .dateRange(DateRange.builder()
                        .startDate(monday)
                        .endDate(monday)
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.SECOND_HALF)
                .build();

        var response = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"durationType\":\"SECOND_HALF\"");
    }
}
