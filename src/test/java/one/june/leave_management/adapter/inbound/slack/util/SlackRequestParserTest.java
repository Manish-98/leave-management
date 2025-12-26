package one.june.leave_management.adapter.inbound.slack.util;

import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.common.exception.SlackPayloadParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SlackRequestParser}
 * <p>
 * Tests form-encoded payload parsing and JSON deserialization for Slack requests
 */
@DisplayName("SlackRequestParser Unit Tests")
class SlackRequestParserTest {

    @Nested
    @DisplayName("extractPayloadJson() Tests")
    class ExtractPayloadJsonTests {

        @Test
        @DisplayName("Should successfully extract valid JSON payload from form-encoded body")
        void shouldExtractValidJsonPayload() {
            // Given
            String jsonPayload = """
                    {"type":"view_submission","team":{"id":"T12345"},"user":{"id":"U12345"}}
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When
            String extracted = SlackRequestParser.extractPayloadJson(requestBody);

            // Then
            assertThat(extracted).isNotNull();
            assertThat(extracted).contains("\"type\"");
            assertThat(extracted).contains("\"view_submission\"");
        }

        @Test
        @DisplayName("Should throw exception when 'payload' parameter is missing")
        void shouldThrowExceptionWhenPayloadParameterIsMissing() {
            // Given
            String requestBody = "team_id=T12345&user_id=U12345";

            // When & Then - Exception is wrapped, so check the outer message
            assertThatThrownBy(() -> SlackRequestParser.extractPayloadJson(requestBody))
                    .isInstanceOf(SlackPayloadParseException.class)
                    .hasMessageContaining("Failed to extract payload from request body");
        }

        @Test
        @DisplayName("Should throw exception when 'payload' parameter is empty")
        void shouldThrowExceptionWhenPayloadParameterIsEmpty() {
            // Given
            String requestBody = "payload=";

            // When & Then - Exception is wrapped, so check the outer message
            assertThatThrownBy(() -> SlackRequestParser.extractPayloadJson(requestBody))
                    .isInstanceOf(SlackPayloadParseException.class)
                    .hasMessageContaining("Failed to extract payload from request body");
        }

        @Test
        @DisplayName("Should throw exception when request body is malformed")
        void shouldThrowExceptionWhenRequestBodyIsMalformed() {
            // Given - Malformed form encoding
            String requestBody = "invalid&form=data&without&equals";

            // When & Then
            assertThatThrownBy(() -> SlackRequestParser.extractPayloadJson(requestBody))
                    .isInstanceOf(SlackPayloadParseException.class)
                    .hasMessageContaining("Failed to extract payload");
        }

        @Test
        @DisplayName("Should successfully extract payload with URL-encoded special characters")
        void shouldExtractPayloadWithUrlEncodedSpecialCharacters() {
            // Given - JSON with special characters that will be URL-encoded
            String jsonPayload = """
                    {"type":"view_submission","user":"john@example.com","message":"Hello & goodbye!"}
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When
            String extracted = SlackRequestParser.extractPayloadJson(requestBody);

            // Then
            assertThat(extracted).isNotNull();
            assertThat(extracted).contains("john@example.com");
            assertThat(extracted).contains("Hello & goodbye!");
        }

        @Test
        @DisplayName("Should successfully extract payload with URL-encoded unicode characters")
        void shouldExtractPayloadWithUrlEncodedUnicode() {
            // Given - JSON with unicode characters
            String jsonPayload = """
                    {"type":"view_submission","user":"日本語","message":"Привет мир"}
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When
            String extracted = SlackRequestParser.extractPayloadJson(requestBody);

            // Then
            assertThat(extracted).isNotNull();
            assertThat(extracted).contains("日本語");
            assertThat(extracted).contains("Привет мир");
        }
    }

    @Nested
    @DisplayName("parsePayload() Tests")
    class ParsePayloadTests {

        @Test
        @DisplayName("Should successfully parse valid JSON to target class")
        void shouldSuccessfullyParseValidJsonToTargetClass() {
            // Given
            String jsonPayload = """
                    {
                        "type": "view_submission",
                        "team": {"id": "T12345", "domain": "company"},
                        "user": {"id": "U12345", "name": "John Doe"},
                        "api_app_id": "A12345"
                    }
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When - Using a simple Map class for testing
            var result = SlackRequestParser.parsePayload(requestBody, java.util.Map.class);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when JSON is malformed")
        void shouldThrowExceptionWhenJsonIsMalformed() {
            // Given
            String malformedJson = "{invalid json structure}";
            String requestBody = "payload=" + urlEncode(malformedJson);

            // When & Then
            assertThatThrownBy(() -> SlackRequestParser.parsePayload(requestBody, java.util.Map.class))
                    .isInstanceOf(SlackPayloadParseException.class)
                    .hasMessageContaining("Failed to parse payload");
        }

        @Test
        @DisplayName("Should throw exception when target class doesn't match JSON structure")
        void shouldThrowExceptionWhenClassDoesntMatchJsonStructure() {
            // Given - JSON doesn't have required fields for a class like SlackCommandRequest
            String jsonPayload = """
                    {"type":"view_submission","invalid":"field"}
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When & Then
            assertThatThrownBy(() -> SlackRequestParser.parsePayload(requestBody, SlackCommandRequest.class))
                    .isInstanceOf(SlackPayloadParseException.class);
        }

        @Test
        @DisplayName("Should propagate exception from extractPayloadJson when payload is missing")
        void shouldPropagateExceptionWhenPayloadIsMissing() {
            // Given - Request body without "payload" parameter
            String requestBody = "team_id=T12345&user_id=U12345";

            // When & Then - Exception is wrapped with parsePayload's message
            assertThatThrownBy(() -> SlackRequestParser.parsePayload(requestBody, java.util.Map.class))
                    .isInstanceOf(SlackPayloadParseException.class)
                    .hasMessageContaining("Failed to parse payload");
        }

        @Test
        @DisplayName("Should successfully parse JSON with nested objects")
        void shouldSuccessfullyParseJsonWithNestedObjects() {
            // Given - JSON with deeply nested structure
            String jsonPayload = """
                    {
                        "type": "view_submission",
                        "team": {
                            "id": "T12345",
                            "domain": "company",
                            "enterprise": {
                                "id": "E12345",
                                "name": "Enterprise"
                            }
                        },
                        "user": {
                            "id": "U12345",
                            "profile": {
                                "name": "John Doe",
                                "email": "john@example.com"
                            }
                        }
                    }
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When
            var result = SlackRequestParser.parsePayload(requestBody, java.util.Map.class);

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("extractType() Tests")
    class ExtractTypeTests {

        @Test
        @DisplayName("Should successfully extract 'view_submission' type from payload")
        void shouldExtractViewSubmissionType() {
            // Given
            String jsonPayload = """
                    {
                        "type": "view_submission",
                        "team": {"id": "T12345"},
                        "user": {"id": "U12345"}
                    }
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When
            String type = SlackRequestParser.extractType(requestBody);

            // Then
            assertThat(type).isEqualTo("view_submission");
        }

        @Test
        @DisplayName("Should successfully extract 'view_closed' type from payload")
        void shouldExtractViewClosedType() {
            // Given
            String jsonPayload = """
                    {
                        "type": "view_closed",
                        "team": {"id": "T12345"},
                        "view": {"id": "V12345"}
                    }
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When
            String type = SlackRequestParser.extractType(requestBody);

            // Then
            assertThat(type).isEqualTo("view_closed");
        }

        @Test
        @DisplayName("Should successfully extract 'block_actions' type from payload")
        void shouldExtractBlockActionsType() {
            // Given
            String jsonPayload = """
                    {
                        "type": "block_actions",
                        "team": {"id": "T12345"},
                        "actions": [{"action_id": "action1"}]
                    }
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When
            String type = SlackRequestParser.extractType(requestBody);

            // Then
            assertThat(type).isEqualTo("block_actions");
        }

        @Test
        @DisplayName("Should throw exception when 'type' field is missing from payload")
        void shouldThrowExceptionWhenTypeFieldIsMissing() {
            // Given - JSON without "type" field
            String jsonPayload = """
                    {
                        "team": {"id": "T12345"},
                        "user": {"id": "U12345"}
                    }
                    """;
            String requestBody = "payload=" + urlEncode(jsonPayload);

            // When & Then
            assertThatThrownBy(() -> SlackRequestParser.extractType(requestBody))
                    .isInstanceOf(SlackPayloadParseException.class)
                    .hasMessageContaining("Missing 'type' field");
        }
    }

    @Nested
    @DisplayName("parseCommandPayload() Tests")
    class ParseCommandPayloadTests {

        @Test
        @DisplayName("Should successfully parse slash command with all fields to SlackCommandRequest")
        void shouldParseSlashCommandWithAllFields() {
            // Given
            String requestBody = "command=%2Fleave" +
                    "&text=annual+leave" +
                    "&trigger_id=T12345.67890" +
                    "&user_id=U12345" +
                    "&user_name=john.doe" +
                    "&channel_id=C12345" +
                    "&channel_name=general" +
                    "&team_id=T12345" +
                    "&team_domain=company" +
                    "&response_url=https%3A%2F%2Fhooks.slack.com" +
                    "&api_app_id=A12345";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request).isNotNull();
            assertThat(request.getCommand()).isEqualTo("/leave");
            assertThat(request.getText()).isEqualTo("annual leave");
            assertThat(request.getTriggerId()).isEqualTo("T12345.67890");
            assertThat(request.getUserId()).isEqualTo("U12345");
            assertThat(request.getUserName()).isEqualTo("john.doe");
            assertThat(request.getChannelId()).isEqualTo("C12345");
            assertThat(request.getChannelName()).isEqualTo("general");
            assertThat(request.getTeamId()).isEqualTo("T12345");
            assertThat(request.getTeamDomain()).isEqualTo("company");
            assertThat(request.getResponseUrl()).isEqualTo("https://hooks.slack.com");
            assertThat(request.getApiAppId()).isEqualTo("A12345");
        }

        @Test
        @DisplayName("Should successfully parse slash command with minimal required fields")
        void shouldParseSlashCommandWithMinimalFields() {
            // Given - Only required fields
            String requestBody = "command=%2Fleave" +
                    "&user_id=U12345" +
                    "&channel_id=C12345";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request).isNotNull();
            assertThat(request.getCommand()).isEqualTo("/leave");
            assertThat(request.getUserId()).isEqualTo("U12345");
            assertThat(request.getChannelId()).isEqualTo("C12345");
            assertThat(request.getText()).isNull();
            assertThat(request.getTriggerId()).isNull();
        }

        @Test
        @DisplayName("Should successfully parse slash command with URL-encoded values")
        void shouldParseSlashCommandWithUrlEncodedValues() {
            // Given - Text with spaces and special characters
            String requestBody = "command=%2Fleave" +
                    "&text=sick+leave+%2B+medical+appointment" +
                    "&user_id=U12345" +
                    "&channel_id=C12345" +
                    "&user_name=John+Doe+Jr.";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request).isNotNull();
            assertThat(request.getText()).isEqualTo("sick leave + medical appointment");
            assertThat(request.getUserName()).isEqualTo("John Doe Jr.");
        }

        @Test
        @DisplayName("Should successfully parse slash command with empty text field")
        void shouldParseSlashCommandWithEmptyText() {
            // Given
            String requestBody = "command=%2Fleave" +
                    "&text=" +
                    "&user_id=U12345" +
                    "&channel_id=C12345";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request).isNotNull();
            assertThat(request.getText()).isEmpty();
        }

        @Test
        @DisplayName("Should handle malformed form encoding gracefully")
        void shouldHandleMalformedFormEncoding() {
            // Given - Malformed form encoding (parameters without = are filtered out)
            String requestBody = "invalid&format&command=%2Fleave";

            // When - parseCommandPayload should still work with valid fields
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then - Should successfully parse the valid command field
            assertThat(request).isNotNull();
            assertThat(request.getCommand()).isEqualTo("/leave");
        }

        @Test
        @DisplayName("Should verify command field is correctly extracted")
        void shouldVerifyCommandFieldIsCorrectlyExtracted() {
            // Given
            String requestBody = "command=%2Fleave&user_id=U12345&channel_id=C12345";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request.getCommand()).isEqualTo("/leave");
        }

        @Test
        @DisplayName("Should verify userId, channelId, teamId are correctly extracted")
        void shouldVerifyIdsAreCorrectlyExtracted() {
            // Given
            String requestBody = "command=%2Fleave" +
                    "&user_id=U12345" +
                    "&channel_id=C12345" +
                    "&team_id=T12345";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request.getUserId()).isEqualTo("U12345");
            assertThat(request.getChannelId()).isEqualTo("C12345");
            assertThat(request.getTeamId()).isEqualTo("T12345");
        }

        @Test
        @DisplayName("Should verify responseUrl is correctly extracted")
        void shouldVerifyResponseUrlIsCorrectlyExtracted() {
            // Given
            String requestBody = "command=%2Fleave" +
                    "&user_id=U12345" +
                    "&response_url=https%3A%2F%2Fhooks.slack.com%2Fcommands%2F12345%2F67890";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request.getResponseUrl()).isEqualTo("https://hooks.slack.com/commands/12345/67890");
        }

        @Test
        @DisplayName("Should verify special characters in user_name are URL-decoded correctly")
        void shouldVerifySpecialCharactersInUserNameAreDecoded() {
            // Given - Username with special characters
            String requestBody = "command=%2Fleave" +
                    "&user_name=john.o%27neill" +
                    "&user_id=U12345" +
                    "&channel_id=C12345";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request.getUserName()).isEqualTo("john.o'neill");
        }

        @Test
        @DisplayName("Should verify apiAppId is correctly extracted")
        void shouldVerifyApiAppIdIsCorrectlyExtracted() {
            // Given
            String requestBody = "command=%2Fleave" +
                    "&api_app_id=A12345" +
                    "&user_id=U12345" +
                    "&channel_id=C12345";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(requestBody, SlackCommandRequest.class);

            // Then
            assertThat(request.getApiAppId()).isEqualTo("A12345");
        }
    }

    @Nested
    @DisplayName("parseFormPayload() Tests (through public methods)")
    class ParseFormPayloadTests {

        @Test
        @DisplayName("Should successfully parse simple key=value pairs")
        void shouldParseSimpleKeyValuePairs() {
            // Given
            String requestBody = "key1=value1&key2=value2";

            // When - Extract through a public method that uses parseFormPayload
            String result = SlackRequestParser.extractPayloadJson(requestBody + "&payload=" + urlEncode("{\"test\":\"data\"}"));

            // Then - If we got here without exception, parseFormPayload worked
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should successfully parse multiple key=value pairs separated by &")
        void shouldParseMultipleKeyValuePairs() {
            // Given
            String requestBody = "team_id=T12345&user_id=U12345&channel_id=C12345&api_app_id=A12345";

            // When - Parse through parseCommandPayload
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(
                    requestBody + "&command=%2Fleave",
                    SlackCommandRequest.class
            );

            // Then - Verify all fields were parsed correctly
            assertThat(request.getTeamId()).isEqualTo("T12345");
            assertThat(request.getUserId()).isEqualTo("U12345");
            assertThat(request.getChannelId()).isEqualTo("C12345");
            assertThat(request.getApiAppId()).isEqualTo("A12345");
        }

        @Test
        @DisplayName("Should successfully handle URL-encoded values in form data")
        void shouldHandleUrlEncodedValuesInFormData() {
            // Given - Form data with URL-encoded values
            String requestBody = "text=Hello+World+%26+Goodbye&user_name=John+Doe";

            // When
            SlackCommandRequest request = SlackRequestParser.parseCommandPayload(
                    requestBody + "&command=%2Fleave&user_id=U12345&channel_id=C12345",
                    SlackCommandRequest.class
            );

            // Then
            assertThat(request.getText()).isEqualTo("Hello World & Goodbye");
            assertThat(request.getUserName()).isEqualTo("John Doe");
        }
    }

    /**
     * Helper method to URL-encode a string (simulates what Slack does)
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL-encode: " + value, e);
        }
    }
}
