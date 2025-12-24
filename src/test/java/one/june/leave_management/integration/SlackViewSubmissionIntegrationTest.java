package one.june.leave_management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Slack view submission API.
 * Tests the complete flow from Slack modal submission to leave creation.
 * Uses H2 in-memory database with real signature verification using test secret.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class SlackViewSubmissionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

    // Test signing secret - must match the one in application-test.properties
    private static final String TEST_SIGNING_SECRET = "test-signing-secret";

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/integrations/slack";
        restTemplate = new RestTemplate();
    }

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
                                        "type": "radio_buttons",
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
                        "private_metadata": "U12345",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        var response = restTemplate.postForEntity(
                baseUrl + "/views",
                createSlackRequestEntity(jsonPayload),
                String.class
        );

        // Then - HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNullOrEmpty(); // Slack expects empty response

        // Then - Database validation
        Map<String, Object> leaveRecord = getLeaveFromDatabase("U12345", "2024-07-01", "2024-07-03");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("U12345");
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
        // Given
        String jsonPayload = """
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
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Optional Holiday"},
                                            "value": "OPTIONAL_HOLIDAY"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "First Half"},
                                            "value": "FIRST_HALF"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-07-10"
                                    }
                                }
                            }
                        },
                        "private_metadata": "U67890",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        var response = restTemplate.postForEntity(
                baseUrl + "/views",
                createSlackRequestEntity(jsonPayload),
                String.class
        );

        // Then - HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Then - Database validation
        Map<String, Object> leaveRecord = getLeaveFromDatabase("U67890", "2024-07-10", "2024-07-10");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("U67890");
        assertThat(((java.sql.Date) leaveRecord.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 10));
        assertThat(((java.sql.Date) leaveRecord.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 10));
        assertThat(leaveRecord.get("type")).isEqualTo("OPTIONAL_HOLIDAY");
        assertThat(leaveRecord.get("status")).isEqualTo("APPROVED");
        assertThat(leaveRecord.get("duration_type")).isEqualTo("FIRST_HALF");

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
                                        "type": "radio_buttons",
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
                                        "selected_date": "2024-07-20"
                                    }
                                }
                            }
                        },
                        "private_metadata": "U11111",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        var response = restTemplate.postForEntity(
                baseUrl + "/views",
                createSlackRequestEntity(jsonPayload),
                String.class
        );

        // Then - HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Then - Database validation (end date should equal start date)
        Map<String, Object> leaveRecord = getLeaveFromDatabase("U11111", "2024-07-20", "2024-07-20");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("U11111");
        assertThat(((java.sql.Date) leaveRecord.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 20));
        assertThat(((java.sql.Date) leaveRecord.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2024, 7, 20));
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
                                        "type": "radio_buttons",
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
                        "private_metadata": "U22222",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        var response = restTemplate.postForEntity(
                baseUrl + "/views",
                createSlackRequestEntity(jsonPayload),
                String.class
        );

        // Then - HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Then - Database validation
        Map<String, Object> leaveRecord = getLeaveFromDatabase("U22222", "2024-08-01", "2024-08-05");
        assertThat(leaveRecord).isNotNull();
        assertThat(leaveRecord.get("user_id")).isEqualTo("U22222");
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
    void shouldReturnUnauthorizedForInvalidSignature() throws Exception {
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
                                        "type": "radio_buttons",
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
                        "private_metadata": "U33333",
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

        // When & Then - RestTemplate throws exception for 4xx responses
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                restTemplate.postForEntity(baseUrl + "/views", entity, String.class)
        )
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class)
                .satisfies(e -> {
                    org.springframework.web.client.HttpClientErrorException exception =
                            (org.springframework.web.client.HttpClientErrorException) e;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void shouldHandleAllLeaveDurations() throws Exception {
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
                                            "type": "radio_buttons",
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
                            "private_metadata": "U%s",
                            "title": {"type": "plain_text", "text": "Apply for Leave"}
                        }
                    }
                    """,
                    400 + i, 400 + i, 400 + i, 400 + i, 400 + i,
                    durationLabel, duration,
                    1 + i * 5, 1 + i * 5,
                    400 + i
            );

            var response = restTemplate.postForEntity(
                    baseUrl + "/views",
                    createSlackRequestEntity(jsonPayload),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
