package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlackCommandRequest}
 * <p>
 * Tests JSON serialization/deserialization, builder pattern, and edge cases
 * for Slack slash command payloads.
 */
@DisplayName("SlackCommandRequest Unit Tests")
class SlackCommandRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should serialize and deserialize with all fields populated")
    void shouldSerializeAndDeserializeWithAllFieldsPopulated() throws JsonProcessingException {
        // Given
        SlackCommandRequest original = SlackCommandRequest.builder()
                .command("/leave")
                .text("annual leave")
                .triggerId("T12345")
                .userId("U12345")
                .userName("john.doe")
                .channelId("C12345")
                .channelName("general")
                .teamId("T12345")
                .teamDomain("company")
                .responseUrl("https://hooks.slack.com/commands/1234/5678")
                .apiAppId("A12345")
                .build();

        // When
        String json = objectMapper.writeValueAsString(original);
        SlackCommandRequest deserialized = objectMapper.readValue(json, SlackCommandRequest.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getCommand()).isEqualTo("/leave");
        assertThat(deserialized.getText()).isEqualTo("annual leave");
        assertThat(deserialized.getTriggerId()).isEqualTo("T12345");
        assertThat(deserialized.getUserId()).isEqualTo("U12345");
        assertThat(deserialized.getUserName()).isEqualTo("john.doe");
        assertThat(deserialized.getChannelId()).isEqualTo("C12345");
        assertThat(deserialized.getChannelName()).isEqualTo("general");
        assertThat(deserialized.getTeamId()).isEqualTo("T12345");
        assertThat(deserialized.getTeamDomain()).isEqualTo("company");
        assertThat(deserialized.getResponseUrl()).isEqualTo("https://hooks.slack.com/commands/1234/5678");
        assertThat(deserialized.getApiAppId()).isEqualTo("A12345");
    }

    @Test
    @DisplayName("Should create instance using builder pattern")
    void shouldCreateInstanceUsingBuilderPattern() {
        // Given
        SlackCommandRequest request = SlackCommandRequest.builder()
                .command("/leave")
                .userId("U12345")
                .channelId("C12345")
                .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getCommand()).isEqualTo("/leave");
        assertThat(request.getUserId()).isEqualTo("U12345");
        assertThat(request.getChannelId()).isEqualTo("C12345");
    }

    @Test
    @DisplayName("Should handle null fields gracefully")
    void shouldHandleNullFieldsGracefully() throws JsonProcessingException {
        // Given
        SlackCommandRequest request = SlackCommandRequest.builder()
                .command("/leave")
                .text(null)
                .triggerId(null)
                .build();

        String json = objectMapper.writeValueAsString(request);

        // When
        SlackCommandRequest deserialized = objectMapper.readValue(json, SlackCommandRequest.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getCommand()).isEqualTo("/leave");
        assertThat(deserialized.getText()).isNull();
        assertThat(deserialized.getTriggerId()).isNull();
    }

    @Test
    @DisplayName("Should handle empty string fields")
    void shouldHandleEmptyStringFields() {
        // Given
        SlackCommandRequest request = SlackCommandRequest.builder()
                .command("")
                .text("")
                .triggerId("")
                .build();

        // Then
        assertThat(request.getCommand()).isEmpty();
        assertThat(request.getText()).isEmpty();
        assertThat(request.getTriggerId()).isEmpty();
    }

    @Test
    @DisplayName("Should support no-args constructor")
    void shouldSupportNoArgsConstructor() {
        // When
        SlackCommandRequest request = new SlackCommandRequest();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getCommand()).isNull();
        assertThat(request.getText()).isNull();
    }

    @Test
    @DisplayName("Should serialize to correct JSON format")
    void shouldSerializeToCorrectJsonFormat() throws JsonProcessingException {
        // Given
        SlackCommandRequest request = SlackCommandRequest.builder()
                .command("/test")
                .text("sample text")
                .build();

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).contains("\"command\":\"/test\"");
        assertThat(json).contains("\"text\":\"sample text\"");
    }

    @Test
    @DisplayName("Should deserialize from JSON with partial fields")
    void shouldDeserializeFromJsonWithPartialFields() throws JsonProcessingException {
        // Given
        String json = "{\"command\":\"/leave\",\"userId\":\"U12345\"}";

        // When
        SlackCommandRequest request = objectMapper.readValue(json, SlackCommandRequest.class);

        // Then
        assertThat(request.getCommand()).isEqualTo("/leave");
        assertThat(request.getUserId()).isEqualTo("U12345");
        assertThat(request.getText()).isNull();
    }
}
