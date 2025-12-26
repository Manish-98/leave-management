package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlackViewClosedRequest}
 * <p>
 * Tests JSON deserialization with nested objects, type validation, and edge cases
 * for Slack view_closed payloads.
 */
@DisplayName("SlackViewClosedRequest Unit Tests")
class SlackViewClosedRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should deserialize full payload with all nested objects")
    void shouldDeserializeFullPayloadWithAllNestedObjects() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "view_closed",
                    "team": {
                        "id": "T12345",
                        "domain": "company"
                    },
                    "user": {
                        "id": "U12345",
                        "username": "john.doe",
                        "name": "John Doe",
                        "team_id": "T12345"
                    },
                    "api_app_id": "A12345",
                    "view": {
                        "id": "V12345",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "private_metadata": "{\\"userId\\": \\"U12345\\", \\"channelId\\": \\"C12345\\"}"
                    }
                }
                """;

        // When
        SlackViewClosedRequest request = objectMapper.readValue(json, SlackViewClosedRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getType()).isEqualTo("view_closed");

        assertThat(request.getTeam()).isNotNull();
        assertThat(request.getTeam().getId()).isEqualTo("T12345");
        assertThat(request.getTeam().getDomain()).isEqualTo("company");

        assertThat(request.getUser()).isNotNull();
        assertThat(request.getUser().getId()).isEqualTo("U12345");
        assertThat(request.getUser().getUsername()).isEqualTo("john.doe");
        assertThat(request.getUser().getName()).isEqualTo("John Doe");
        assertThat(request.getUser().getTeamId()).isEqualTo("T12345");

        assertThat(request.getApiAppId()).isEqualTo("A12345");

        assertThat(request.getView()).isNotNull();
        assertThat(request.getView().getId()).isEqualTo("V12345");
        assertThat(request.getView().getTeamId()).isEqualTo("T12345");
        assertThat(request.getView().getType()).isEqualTo("modal");
        assertThat(request.getView().getCallbackId()).isEqualTo("leave_application_submit");
        assertThat(request.getView().getPrivateMetadata()).isNotNull();
    }

    @Test
    @DisplayName("Should validate type field as view_closed")
    void shouldValidateTypeFieldAsViewClosed() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "view_closed",
                    "team": {"id": "T12345", "domain": "company"},
                    "user": {"id": "U12345", "username": "john.doe", "name": "John Doe", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "view": {"id": "V12345"}
                }
                """;

        // When
        SlackViewClosedRequest request = objectMapper.readValue(json, SlackViewClosedRequest.class);

        // Then
        assertThat(request.getType()).isEqualTo("view_closed");
    }

    @Test
    @DisplayName("Should deserialize with null optional nested objects")
    void shouldDeserializeWithNullOptionalNestedObjects() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "view_closed",
                    "team": null,
                    "user": null,
                    "api_app_id": "A12345",
                    "view": null
                }
                """;

        // When
        SlackViewClosedRequest request = objectMapper.readValue(json, SlackViewClosedRequest.class);

        // Then
        assertThat(request.getType()).isEqualTo("view_closed");
        assertThat(request.getApiAppId()).isEqualTo("A12345");
        assertThat(request.getTeam()).isNull();
        assertThat(request.getUser()).isNull();
        assertThat(request.getView()).isNull();
    }

    @Test
    @DisplayName("Should build using builder pattern with all nested objects")
    void shouldBuildUsingBuilderPatternWithAllNestedObjects() {
        // Given
        SlackViewClosedRequest.SlackTeam team = SlackViewClosedRequest.SlackTeam.builder()
                .id("T12345")
                .domain("company")
                .build();

        SlackViewClosedRequest.SlackUser user = SlackViewClosedRequest.SlackUser.builder()
                .id("U12345")
                .username("john.doe")
                .name("John Doe")
                .teamId("T12345")
                .build();

        SlackViewClosedRequest.SlackView view = SlackViewClosedRequest.SlackView.builder()
                .id("V12345")
                .teamId("T12345")
                .type("modal")
                .callbackId("leave_application_submit")
                .privateMetadata("{\"userId\": \"U12345\"}")
                .build();

        // When
        SlackViewClosedRequest request = SlackViewClosedRequest.builder()
                .type("view_closed")
                .team(team)
                .user(user)
                .apiAppId("A12345")
                .view(view)
                .build();

        // Then
        assertThat(request.getType()).isEqualTo("view_closed");
        assertThat(request.getTeam()).isEqualTo(team);
        assertThat(request.getUser()).isEqualTo(user);
        assertThat(request.getApiAppId()).isEqualTo("A12345");
        assertThat(request.getView()).isEqualTo(view);
    }

    @Test
    @DisplayName("Should extract private metadata from view")
    void shouldExtractPrivateMetadataFromView() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "view_closed",
                    "team": {"id": "T12345", "domain": "company"},
                    "user": {"id": "U12345", "username": "john.doe", "name": "John Doe", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "view": {
                        "id": "V12345",
                        "private_metadata": "{\\"channel\\": \\"general\\", \\"threadTs\\": \\"1234567890.123456\\"}"
                    }
                }
                """;

        // When
        SlackViewClosedRequest request = objectMapper.readValue(json, SlackViewClosedRequest.class);

        // Then
        assertThat(request.getView()).isNotNull();
        assertThat(request.getView().getPrivateMetadata()).contains("channel");
        assertThat(request.getView().getPrivateMetadata()).contains("general");
        assertThat(request.getView().getPrivateMetadata()).contains("threadTs");
    }

    @Test
    @DisplayName("Should ignore unknown properties with @JsonIgnoreProperties")
    void shouldIgnoreUnknownPropertiesWithJsonIgnoreProperties() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "view_closed",
                    "team": {"id": "T12345", "domain": "company"},
                    "user": {"id": "U12345", "username": "john.doe", "name": "John Doe", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "view": {"id": "V12345"},
                    "unknown_field": "some_value",
                    "another_unknown": 123
                }
                """;

        // When
        SlackViewClosedRequest request = objectMapper.readValue(json, SlackViewClosedRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getType()).isEqualTo("view_closed");
    }

    @Test
    @DisplayName("Should handle empty nested objects")
    void shouldHandleEmptyNestedObjects() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "view_closed",
                    "team": {},
                    "user": {},
                    "api_app_id": "A12345",
                    "view": {}
                }
                """;

        // When
        SlackViewClosedRequest request = objectMapper.readValue(json, SlackViewClosedRequest.class);

        // Then
        assertThat(request.getTeam()).isNotNull();
        assertThat(request.getUser()).isNotNull();
        assertThat(request.getView()).isNotNull();
        assertThat(request.getTeam().getId()).isNull();
        assertThat(request.getUser().getId()).isNull();
        assertThat(request.getView().getId()).isNull();
    }
}
