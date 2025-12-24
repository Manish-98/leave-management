package one.june.leave_management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for Leave Ingestion API.
 * Uses H2 in-memory database with no mocking.
 * Clock is controlled through fixed dates in tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class LeaveIngestionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

    // Fixed date for testing
    private static final LocalDate FIXED_DATE = LocalDate.of(2024, 6, 15);

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/leaves";
        restTemplate = new RestTemplate();
    }

    @AfterEach
    void tearDown() {
        // Cleanup is handled by @Transactional and H2's in-memory nature
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
                .userId("user-123")
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
        assertThat(responseBody).contains("\"userId\":\"user-123\"");
        assertThat(responseBody).contains("\"type\":\"ANNUAL_LEAVE\"");
        assertThat(responseBody).contains("\"status\":\"REQUESTED\"");
        assertThat(responseBody).contains("\"durationType\":\"FULL_DAY\"");
        assertThat(responseBody).contains("\"sourceType\":\"WEB\"");
        assertThat(responseBody).contains("\"sourceId\":\"web-123\"");

        // Database validation
        Map<String, Object> leaveRecord = getLeaveFromDatabase("user-123", "2024-06-16", "2024-06-18");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("user-123");
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
                .userId("user-456")
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
        Map<String, Object> firstRecord = getLeaveFromDatabase("user-456", "2024-06-25", "2024-06-27");
        assertThat(firstRecord).isNotNull();
        assertThat(firstRecord.get("type")).isEqualTo("ANNUAL_LEAVE");
        assertThat(firstRecord.get("status")).isEqualTo("REQUESTED");

        // Second request with same sourceId - should update the existing leave
        LeaveIngestionRequest secondRequest = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("web-update-test") // Same sourceId
                .userId("user-456")
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(20))
                        .endDate(FIXED_DATE.plusDays(25))
                        .build())
                .type(LeaveType.OPTIONAL_HOLIDAY) // Different type
                .status(LeaveStatus.APPROVED) // Different status
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        var secondResponse = restTemplate.postForEntity(baseUrl + "/ingest", createRequestEntity(secondRequest), String.class);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String secondBody = secondResponse.getBody();
        assertThat(secondBody).contains("\"id\":\"" + leaveId + "\""); // Same ID
        assertThat(secondBody).contains("\"type\":\"OPTIONAL_HOLIDAY\"");
        assertThat(secondBody).contains("\"status\":\"APPROVED\"");

        // Verify update in database - same leave ID but updated values
        Map<String, Object> updatedRecord = getLeaveFromDatabase("user-456", "2024-07-05", "2024-07-10");
        assertThat(updatedRecord).isNotNull();
        assertThat(updatedRecord.get("id").toString()).isEqualTo(leaveId); // Same ID
        assertThat(updatedRecord.get("type")).isEqualTo("OPTIONAL_HOLIDAY");
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
                .userId("user-overlap")
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
                .userId("user-overlap") // Same user
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
                .userId("user-no-overlap")
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
                .userId("user-no-overlap") // Same user
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
                .userId("user-half-day")
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(1)) // Same day
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
                .userId("user-invalid-half-day")
                .dateRange(DateRange.builder()
                        .startDate(FIXED_DATE.plusDays(1))
                        .endDate(FIXED_DATE.plusDays(3)) // Different days
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
                    .userId("user-all-sources")
                    .dateRange(DateRange.builder()
                            .startDate(FIXED_DATE.plusDays(1 + dayOffset * 10))
                            .endDate(FIXED_DATE.plusDays(2 + dayOffset * 10))
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
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-type-" + leaveType.name())
                    .userId("user-all-types")
                    .dateRange(DateRange.builder()
                            .startDate(FIXED_DATE.plusDays(1 + dayOffset * 10))
                            .endDate(FIXED_DATE.plusDays(2 + dayOffset * 10))
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
                    .userId("user-all-statuses")
                    .dateRange(DateRange.builder()
                            .startDate(FIXED_DATE.plusDays(1 + dayOffset * 10))
                            .endDate(FIXED_DATE.plusDays(2 + dayOffset * 10))
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
                    .userId("user-all-durations")
                    .dateRange(DateRange.builder()
                            .startDate(FIXED_DATE.plusDays(1 + dayOffset * 10))
                            .endDate(FIXED_DATE.plusDays(1 + dayOffset * 10)) // Same day for half-day leaves
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
                .userId("user-invalid-date")
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
                .userId("user-long-source-id")
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
                .userId("user-multi-source")
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
                .userId("user-multi-source")
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
}
