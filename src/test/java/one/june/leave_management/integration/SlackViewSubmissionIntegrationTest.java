package one.june.leave_management.integration;

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
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Slack view submission API.
 * Tests the complete flow from Slack modal submission to leave creation.
 * Uses H2 in-memory database with real signature verification using test secret.
 * Uses @BeforeTransaction to set up test data in committed state,
 * allowing HTTP requests via RestTemplate to see the data while keeping
 * tests transactional for proper isolation.
 */
@IntegrationTest
class SlackViewSubmissionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;
    private RestTemplate restTemplate;

    // Test signing secret - must match the one in application-test.properties
    private static final String TEST_SIGNING_SECRET = "test-signing-secret";

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/integrations/slack";
        restTemplate = new RestTemplate();
    }

    /**
     * Sets up test data in the database before each test transaction.
     * This method runs in a separate transaction that commits, making the data
     * visible to subsequent HTTP requests via RestTemplate.
     */
    @BeforeTransaction
    void setUpTestData() {
        // Clean up existing test employees
        String[] slackIds = {"U12345", "U67890", "U11111", "U22222", "U99999"};
        // First, fetch the employee UUIDs for these Slack IDs
        for (String slackId : slackIds) {
            // Delete by employee ID (UUID) instead of Slack ID
            jdbcTemplate.update("DELETE FROM leave WHERE user_id IN (SELECT id FROM employee WHERE slack_id = ?)", slackId);
            jdbcTemplate.update("DELETE FROM employee WHERE slack_id = ?", slackId);
        }

        // Create test employees for all Slack user IDs used in tests
        int i = 0;
        for (String slackId : slackIds) {
            String baseUuid = "123e4567-e89b-12d3-a456-426614174" + String.format("%03d", i);
            String employeeSql = String.format(
                    "INSERT INTO employee (id, name, slack_id, date_of_joining, active, region, created_at, updated_at) " +
                    "VALUES (?, 'Test User %d', '%s', '2020-01-01', true, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    i, slackId);
            jdbcTemplate.update(employeeSql, baseUuid);
            i++;
        }

        // Clean up any existing optional holidays
        jdbcTemplate.update("DELETE FROM optional_holidays");

        // Create optional holiday needed for test on 2024-01-01
        String holidayId = java.util.UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO optional_holidays (id, date, name, description, region, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                holidayId, LocalDate.of(2024, Month.JANUARY, 1), "New Year's Day", "First day of the year"
        );

        // Store the holiday ID for use in tests
        testHolidayId = holidayId;
    }

    // Store the holiday ID for use in tests
    private static String testHolidayId;

    private HttpEntity<String> createSlackRequestEntity(String jsonPayload) {
        // Slack sends view submissions as form-encoded payload parameter
        String formPayload = "payload=" + java.net.URLEncoder.encode(jsonPayload, StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = generateSignature(timestamp, formPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("X-Slack-Signature", signature);
        headers.set("X-Slack-Request-Timestamp", timestamp);
        return new HttpEntity<>(formPayload, headers);
    }

    /**
     * Generates a valid Slack signature for testing
     */
    private String generateSignature(String timestamp, String requestBody) {
        try {
            String baseString = "v0:" + timestamp + ":" + requestBody;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    TEST_SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return "v0=" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
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
     * Helper method to query leave source reference from database
     */
    private Map<String, Object> getLeaveSourceRefFromDatabase(String leaveId, String sourceType) {
        String sql = """
                SELECT source_id, source_type
                FROM leave_source_ref
                WHERE leave_id = ? AND source_type = ?
                """;
        return jdbcTemplate.queryForMap(sql, leaveId, sourceType);
    }

    @Test
    void shouldCreateAnnualLeaveFromSlackViewSubmission() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "type": "view_submission",
                    "team": {"id": "T12345", "domain": "example"},
                    "user": {"id": "U12345", "username": "testuser", "name": "Test User", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger123",
                    "view": {
                        "id": "V12345",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "static_select",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Annual Leave"},
                                            "value": "ANNUAL_LEAVE"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Full Day"},
                                            "value": "FULL_DAY"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-07-01"
                                    }
                                },
                                "end_date_block": {
                                    "end_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-07-03"
                                    }
                                },
                                "reason_block": {
                                    "reason_action": {
                                        "type": "plain_text_input",
                                        "value": "Summer vacation"
                                    }
                                }
                            }
                        },
                        "private_metadata": "{\\"userId\\":\\"U12345\\",\\"channelId\\":\\"C12345\\",\\"channelName\\":\\"test-channel\\",\\"threadTs\\":\\"1234567890.123456\\"}",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        var response = restTemplate.postForEntity(
                baseUrl + "/interactions",
                createSlackRequestEntity(jsonPayload),
                String.class
        );

        // Then - HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNullOrEmpty(); // Slack expects empty response

        // Wait for async processing to complete
        Thread.sleep(1000);

        // Then - Database validation
        Map<String, Object> leaveRecord = getLeaveFromDatabase("123e4567-e89b-12d3-a456-426614174000", "2024-07-01", "2024-07-03");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
        assertThat(((java.sql.Date) leaveRecord.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 1));
        assertThat(((java.sql.Date) leaveRecord.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 3));
        assertThat(leaveRecord.get("type")).isEqualTo("ANNUAL_LEAVE");
        assertThat(leaveRecord.get("status")).isEqualTo("APPROVED");
        assertThat(leaveRecord.get("duration_type")).isEqualTo("FULL_DAY");

        // Verify source reference
        String leaveId = leaveRecord.get("id").toString();
        Map<String, Object> sourceRef = getLeaveSourceRefFromDatabase(leaveId, "SLACK");
        assertThat(sourceRef).isNotNull();
        assertThat(sourceRef.get("source_id")).isEqualTo("V12345"); // View ID from submission
        assertThat(sourceRef.get("source_type")).isEqualTo("SLACK");
    }

    @Test
    void shouldCreateOptionalHolidayLeaveFromSlackViewSubmission() throws Exception {
        // Given - Optional holiday is already created in @BeforeTransaction
        String jsonPayload = String.format("""
                {
                    "type": "view_submission",
                    "team": {"id": "T12345", "domain": "example"},
                    "user": {"id": "U67890", "username": "testuser2", "name": "Test User 2", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger456",
                    "view": {
                        "id": "V67890",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "static_select",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Optional Holiday"},
                                            "value": "OPTIONAL_HOLIDAY"
                                        }
                                    }
                                },
                                "holiday_select_block": {
                                    "holiday_select_action": {
                                        "type": "static_select",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "2024-01-01 - New Year's Day"},
                                            "value": "%s"
                                        }
                                    }
                                }
                            }
                        },
                        "private_metadata": "{\\"userId\\":\\"U67890\\",\\"channelId\\":\\"C67890\\",\\"channelName\\":\\"test-channel\\",\\"threadTs\\":\\"1234567890.123456\\"}",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """, testHolidayId);

        // When
        var response = restTemplate.postForEntity(
                baseUrl + "/interactions",
                createSlackRequestEntity(jsonPayload),
                String.class
        );

        // Then - HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for async processing to complete
        Thread.sleep(500);

        // Then - Database validation
        Map<String, Object> leaveRecord = getLeaveFromDatabase("123e4567-e89b-12d3-a456-426614174001", "2024-01-01", "2024-01-01");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("123e4567-e89b-12d3-a456-426614174001");
        assertThat(((java.sql.Date) leaveRecord.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(((java.sql.Date) leaveRecord.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(leaveRecord.get("type")).isEqualTo("OPTIONAL_HOLIDAY");
        assertThat(leaveRecord.get("status")).isEqualTo("APPROVED");
        assertThat(leaveRecord.get("duration_type")).isEqualTo("FULL_DAY"); // Optional holidays default to FULL_DAY

        // Verify source reference
        String leaveId = leaveRecord.get("id").toString();
        Map<String, Object> sourceRef = getLeaveSourceRefFromDatabase(leaveId, "SLACK");
        assertThat(sourceRef).isNotNull();
        assertThat(sourceRef.get("source_id")).isEqualTo("V67890");
        assertThat(sourceRef.get("source_type")).isEqualTo("SLACK");
    }

    @Test
    void shouldHandleSingleDayLeaveWhenEndDateNotProvided() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "type": "view_submission",
                    "team": {"id": "T12345", "domain": "example"},
                    "user": {"id": "U11111", "username": "testuser3", "name": "Test User 3", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger789",
                    "view": {
                        "id": "V11111",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "static_select",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Annual Leave"},
                                            "value": "ANNUAL_LEAVE"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Second Half"},
                                            "value": "SECOND_HALF"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-07-19"
                                    }
                                }
                            }
                        },
                        "private_metadata": "{\\"userId\\":\\"U11111\\",\\"channelId\\":\\"C11111\\",\\"channelName\\":\\"test-channel\\",\\"threadTs\\":\\"1234567890.123456\\"}",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        var response = restTemplate.postForEntity(
                baseUrl + "/interactions",
                createSlackRequestEntity(jsonPayload),
                String.class
        );

        // Then - HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for async processing to complete
        Thread.sleep(500);

        // Then - Database validation (end date should equal start date)
        Map<String, Object> leaveRecord = getLeaveFromDatabase("123e4567-e89b-12d3-a456-426614174002", "2024-07-19", "2024-07-19");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("123e4567-e89b-12d3-a456-426614174002");
        assertThat(((java.sql.Date) leaveRecord.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 19));
        assertThat(((java.sql.Date) leaveRecord.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 19));
        assertThat(leaveRecord.get("type")).isEqualTo("ANNUAL_LEAVE");
        assertThat(leaveRecord.get("status")).isEqualTo("APPROVED");
        assertThat(leaveRecord.get("duration_type")).isEqualTo("SECOND_HALF");

        // Verify source reference
        String leaveId = leaveRecord.get("id").toString();
        Map<String, Object> sourceRef = getLeaveSourceRefFromDatabase(leaveId, "SLACK");
        assertThat(sourceRef).isNotNull();
        assertThat(sourceRef.get("source_id")).isEqualTo("V11111");
        assertThat(sourceRef.get("source_type")).isEqualTo("SLACK");
    }

    @Test
    void shouldHandleLeaveWithoutReason() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "type": "view_submission",
                    "team": {"id": "T12345", "domain": "example"},
                    "user": {"id": "U22222", "username": "testuser4", "name": "Test User 4", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger101",
                    "view": {
                        "id": "V22222",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "static_select",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Annual Leave"},
                                            "value": "ANNUAL_LEAVE"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Full Day"},
                                            "value": "FULL_DAY"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-08-01"
                                    }
                                },
                                "end_date_block": {
                                    "end_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-08-05"
                                    }
                                }
                            }
                        },
                        "private_metadata": "{\\"userId\\":\\"U22222\\",\\"channelId\\":\\"C22222\\",\\"channelName\\":\\"test-channel\\",\\"threadTs\\":\\"1234567890.123456\\"}",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        var response = restTemplate.postForEntity(
                baseUrl + "/interactions",
                createSlackRequestEntity(jsonPayload),
                String.class
        );

        // Then - HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for async processing to complete
        Thread.sleep(500);

        // Then - Database validation
        Map<String, Object> leaveRecord = getLeaveFromDatabase("123e4567-e89b-12d3-a456-426614174003", "2024-08-01", "2024-08-05");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("123e4567-e89b-12d3-a456-426614174003");
        assertThat(((java.sql.Date) leaveRecord.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 8, 1));
        assertThat(((java.sql.Date) leaveRecord.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 8, 5));
        assertThat(leaveRecord.get("type")).isEqualTo("ANNUAL_LEAVE");
        assertThat(leaveRecord.get("status")).isEqualTo("APPROVED");
        assertThat(leaveRecord.get("duration_type")).isEqualTo("FULL_DAY");

        // Verify source reference
        String leaveId = leaveRecord.get("id").toString();
        Map<String, Object> sourceRef = getLeaveSourceRefFromDatabase(leaveId, "SLACK");
        assertThat(sourceRef).isNotNull();
        assertThat(sourceRef.get("source_id")).isEqualTo("V22222");
        assertThat(sourceRef.get("source_type")).isEqualTo("SLACK");
    }

    @Test
    void shouldReturnOkForInvalidSignature() {
        // Given
        String jsonPayload = """
                {
                    "type": "view_submission",
                    "team": {"id": "T12345", "domain": "example"},
                    "user": {"id": "U33333", "username": "testuser5", "name": "Test User 5", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger202",
                    "view": {
                        "id": "V33333",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "static_select",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Annual Leave"},
                                            "value": "ANNUAL_LEAVE"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Full Day"},
                                            "value": "FULL_DAY"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-09-01"
                                    }
                                }
                            }
                        },
                        "private_metadata": "{\\"userId\\":\\"U33333\\",\\"channelId\\":\\"C33333\\",\\"channelName\\":\\"test-channel\\",\\"threadTs\\":\\"1234567890.123456\\"}",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // Create request with invalid signature (form-encoded format)
        String formPayload = "payload=" + java.net.URLEncoder.encode(jsonPayload, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("X-Slack-Signature", "v0=invalid_signature");
        headers.set("X-Slack-Request-Timestamp", String.valueOf(Instant.now().getEpochSecond()));
        HttpEntity<String> entity = new HttpEntity<>(formPayload, headers);

        // When & Then - Should return 200 OK even with invalid signature
        // The error is logged and sent via response_url (if available), but HTTP response is always 200
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/interactions", entity, String.class);

        // Verify 200 OK status (Slack requirement)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify empty response body
        assertThat(response.getBody()).isNullOrEmpty();
    }

    @Test
    void shouldHandleAllLeaveDurations() {
        String[] durations = {"FULL_DAY", "FIRST_HALF", "SECOND_HALF"};
        String[] durationLabels = {"Full Day", "First Half", "Second Half"};

        for (int i = 0; i < durations.length; i++) {
            String duration = durations[i];
            String durationLabel = durationLabels[i];

            String jsonPayload = String.format("""
                    {
                        "type": "view_submission",
                        "team": {"id": "T12345", "domain": "example"},
                        "user": {"id": "U%s", "username": "testuser%s", "name": "Test User %s", "team_id": "T12345"},
                        "api_app_id": "A12345",
                        "token": "verification_token",
                        "trigger_id": "trigger%s",
                        "view": {
                            "id": "V%s",
                            "team_id": "T12345",
                            "type": "modal",
                            "callback_id": "leave_application_submit",
                            "state": {
                                "values": {
                                    "leave_type_category_block": {
                                        "leave_type_category_action": {
                                            "type": "static_select",
                                            "selected_option": {
                                                "text": {"type": "plain_text", "text": "Annual Leave"},
                                                "value": "ANNUAL_LEAVE"
                                            }
                                        }
                                    },
                                    "leave_duration_block": {
                                        "leave_duration_action": {
                                            "type": "radio_buttons",
                                            "selected_option": {
                                                "text": {"type": "plain_text", "text": "%s"},
                                                "value": "%s"
                                            }
                                        }
                                    },
                                    "start_date_block": {
                                        "start_date_action": {
                                            "type": "datepicker",
                                            "selected_date": "2024-10-%02d"
                                        }
                                    }
                                }
                            },
                            "private_metadata": "{\\"userId\\":\\"U%s\\",\\"channelId\\":\\"C%s\\",\\"channelName\\":\\"test-channel\\",\\"threadTs\\":\\"1234567890.123456\\"}",
                            "title": {"type": "plain_text", "text": "Apply for Leave"}
                        }
                    }
                    """,
                    400 + i, 400 + i, 400 + i, 400 + i, 400 + i,
                    durationLabel, duration,
                    1 + i * 5, 1 + i * 5,
                    400 + i, 400 + i
            );

            var response = restTemplate.postForEntity(
                    baseUrl + "/interactions",
                    createSlackRequestEntity(jsonPayload),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
