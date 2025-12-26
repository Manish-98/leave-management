package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlackCommandResponse}
 * <p>
 * Tests JSON serialization/deserialization, builder pattern, and edge cases
 * for Slack command response payloads.
 */
@DisplayName("SlackCommandResponse Unit Tests")
class SlackCommandResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should serialize and deserialize with both fields populated")
    void shouldSerializeAndDeserializeWithBothFieldsPopulated() throws JsonProcessingException {
        // Given
        SlackCommandResponse original = SlackCommandResponse.builder()
                .responseType("in_channel")
                .text("Leave request submitted successfully")
                .build();

        // When
        String json = objectMapper.writeValueAsString(original);
        SlackCommandResponse deserialized = objectMapper.readValue(json, SlackCommandResponse.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getResponseType()).isEqualTo("in_channel");
        assertThat(deserialized.getText()).isEqualTo("Leave request submitted successfully");
    }

    @Test
    @DisplayName("Should create instance using builder pattern")
    void shouldCreateInstanceUsingBuilderPattern() {
        // Given
        SlackCommandResponse response = SlackCommandResponse.builder()
                .responseType("ephemeral")
                .text("Private message")
                .build();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseType()).isEqualTo("ephemeral");
        assertThat(response.getText()).isEqualTo("Private message");
    }

    @Test
    @DisplayName("Should handle null and empty fields")
    void shouldHandleNullAndEmptyFields() throws JsonProcessingException {
        // Given
        SlackCommandResponse response = SlackCommandResponse.builder()
                .responseType(null)
                .text("")
                .build();

        String json = objectMapper.writeValueAsString(response);

        // When
        SlackCommandResponse deserialized = objectMapper.readValue(json, SlackCommandResponse.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getResponseType()).isNull();
        assertThat(deserialized.getText()).isEmpty();
    }

    @Test
    @DisplayName("Should serialize to correct JSON format")
    void shouldSerializeToCorrectJsonFormat() throws JsonProcessingException {
        // Given
        SlackCommandResponse response = SlackCommandResponse.builder()
                .responseType("in_channel")
                .text("Test message")
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).contains("\"responseType\":\"in_channel\"");
        assertThat(json).contains("\"text\":\"Test message\"");
    }
}
