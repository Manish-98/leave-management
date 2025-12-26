package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlackModalTriggerResponse}
 * <p>
 * Tests JSON serialization/deserialization, builder pattern, static factory method, and edge cases
 * for Slack modal trigger responses.
 */
@DisplayName("SlackModalTriggerResponse Unit Tests")
class SlackModalTriggerResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should serialize and deserialize with all fields populated")
    void shouldSerializeAndDeserializeWithAllFieldsPopulated() throws JsonProcessingException {
        // Given
        SlackModalTriggerResponse original = SlackModalTriggerResponse.builder()
                .triggerModal(true)
                .triggerId("T12345")
                .message("Modal triggered successfully")
                .build();

        // When
        String json = objectMapper.writeValueAsString(original);
        SlackModalTriggerResponse deserialized = objectMapper.readValue(json, SlackModalTriggerResponse.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.isTriggerModal()).isTrue();
        assertThat(deserialized.getTriggerId()).isEqualTo("T12345");
        assertThat(deserialized.getMessage()).isEqualTo("Modal triggered successfully");
    }

    @Test
    @DisplayName("Should create instance using builder pattern")
    void shouldCreateInstanceUsingBuilderPattern() {
        // Given
        SlackModalTriggerResponse response = SlackModalTriggerResponse.builder()
                .triggerModal(false)
                .triggerId("T67890")
                .message("No modal")
                .build();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isTriggerModal()).isFalse();
        assertThat(response.getTriggerId()).isEqualTo("T67890");
        assertThat(response.getMessage()).isEqualTo("No modal");
    }

    @Test
    @DisplayName("Should create response via static factory method with valid triggerId")
    void shouldCreateResponseViaStaticFactoryMethodWithValidTriggerId() {
        // When
        SlackModalTriggerResponse response = SlackModalTriggerResponse.forModal("T12345");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isTriggerModal()).isTrue();
        assertThat(response.getTriggerId()).isEqualTo("T12345");
    }

    @Test
    @DisplayName("Should create response via static factory method with null triggerId")
    void shouldCreateResponseViaStaticFactoryMethodWithNullTriggerId() {
        // When
        SlackModalTriggerResponse response = SlackModalTriggerResponse.forModal(null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isTriggerModal()).isTrue();
        assertThat(response.getTriggerId()).isNull();
    }

    @Test
    @DisplayName("Should set triggerModal to false when using builder without explicit value")
    void shouldSetTriggerModalToFalseWhenUsingBuilderWithoutExplicitValue() {
        // When
        SlackModalTriggerResponse response = SlackModalTriggerResponse.builder()
                .triggerId("T12345")
                .build();

        // Then - boolean default is false
        assertThat(response.isTriggerModal()).isFalse();
        assertThat(response.getTriggerId()).isEqualTo("T12345");
    }

    @Test
    @DisplayName("Should handle null and empty fields")
    void shouldHandleNullAndEmptyFields() throws JsonProcessingException {
        // Given
        SlackModalTriggerResponse response = SlackModalTriggerResponse.builder()
                .triggerModal(true)
                .triggerId(null)
                .message("")
                .build();

        String json = objectMapper.writeValueAsString(response);

        // When
        SlackModalTriggerResponse deserialized = objectMapper.readValue(json, SlackModalTriggerResponse.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.isTriggerModal()).isTrue();
        assertThat(deserialized.getTriggerId()).isNull();
        assertThat(deserialized.getMessage()).isEmpty();
    }

    @Test
    @DisplayName("Should serialize to correct JSON format")
    void shouldSerializeToCorrectJsonFormat() throws JsonProcessingException {
        // Given
        SlackModalTriggerResponse response = SlackModalTriggerResponse.builder()
                .triggerModal(true)
                .triggerId("T12345")
                .message("Test")
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).contains("\"triggerModal\":true");
        assertThat(json).contains("\"triggerId\":\"T12345\"");
        assertThat(json).contains("\"message\":\"Test\"");
    }
}
