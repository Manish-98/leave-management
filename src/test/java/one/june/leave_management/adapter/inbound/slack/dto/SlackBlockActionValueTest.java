package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlackBlockActionValue}
 * <p>
 * Tests multiple action types (radio_buttons, datepicker, plain_text_input),
 * nested objects, and edge cases for Slack block action values.
 */
@DisplayName("SlackBlockActionValue Unit Tests")
class SlackBlockActionValueTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should deserialize radio button action with selected option")
    void shouldDeserializeRadioButtonActionWithSelectedOption() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "radio_buttons",
                    "selected_option": {
                        "text": {
                            "type": "plain_text",
                            "text": "Annual Leave",
                            "emoji": true
                        },
                        "value": "ANNUAL_LEAVE"
                    }
                }
                """;

        // When
        SlackBlockActionValue value = objectMapper.readValue(json, SlackBlockActionValue.class);

        // Then
        assertThat(value).isNotNull();
        assertThat(value.getType()).isEqualTo("radio_buttons");
        assertThat(value.getSelectedOption()).isNotNull();
        assertThat(value.getSelectedOption().getText()).isNotNull();
        assertThat(value.getSelectedOption().getText().getType()).isEqualTo("plain_text");
        assertThat(value.getSelectedOption().getText().getText()).isEqualTo("Annual Leave");
        assertThat(value.getSelectedOption().getText().getEmoji()).isTrue();
        assertThat(value.getSelectedOption().getValue()).isEqualTo("ANNUAL_LEAVE");
    }

    @Test
    @DisplayName("Should deserialize date picker action with selected date")
    void shouldDeserializeDatePickerActionWithSelectedDate() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "datepicker",
                    "selected_date": "2024-01-15"
                }
                """;

        // When
        SlackBlockActionValue value = objectMapper.readValue(json, SlackBlockActionValue.class);

        // Then
        assertThat(value).isNotNull();
        assertThat(value.getType()).isEqualTo("datepicker");
        assertThat(value.getSelectedDate()).isEqualTo("2024-01-15");
    }

    @Test
    @DisplayName("Should deserialize plain text input action with value")
    void shouldDeserializePlainTextInputActionWithValue() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "plain_text_input",
                    "value": "This is a sample reason"
                }
                """;

        // When
        SlackBlockActionValue value = objectMapper.readValue(json, SlackBlockActionValue.class);

        // Then
        assertThat(value).isNotNull();
        assertThat(value.getType()).isEqualTo("plain_text_input");
        assertThat(value.getValue()).isEqualTo("This is a sample reason");
    }

    @Test
    @DisplayName("Should serialize all action types to JSON")
    void shouldSerializeAllActionTypesToJSON() throws JsonProcessingException {
        // Given - Radio button action
        SlackBlockActionValue radioButtonAction = SlackBlockActionValue.builder()
                .type("radio_buttons")
                .selectedOption(SlackBlockActionValue.SlackSelectedOption.builder()
                        .text(SlackBlockActionValue.SlackTextObject.builder()
                                .type("plain_text")
                                .text("Option Text")
                                .emoji(true)
                                .build())
                        .value("OPTION_VALUE")
                        .build())
                .build();

        // When
        String json = objectMapper.writeValueAsString(radioButtonAction);

        // Then
        assertThat(json).contains("\"type\":\"radio_buttons\"");
        assertThat(json).contains("\"selected_option\"");
        assertThat(json).contains("\"text\":\"Option Text\"");
    }

    @Test
    @DisplayName("Should build radio button action using builder")
    void shouldBuildRadioButtonActionUsingBuilder() {
        // Given
        SlackBlockActionValue.SlackTextObject text = SlackBlockActionValue.SlackTextObject.builder()
                .type("plain_text")
                .text("Test Option")
                .emoji(true)
                .build();

        SlackBlockActionValue.SlackSelectedOption option = SlackBlockActionValue.SlackSelectedOption.builder()
                .text(text)
                .value("TEST_VALUE")
                .build();

        // When
        SlackBlockActionValue value = SlackBlockActionValue.builder()
                .type("radio_buttons")
                .selectedOption(option)
                .build();

        // Then
        assertThat(value.getType()).isEqualTo("radio_buttons");
        assertThat(value.getSelectedOption()).isEqualTo(option);
        assertThat(value.getSelectedOption().getText()).isEqualTo(text);
        assertThat(value.getSelectedOption().getValue()).isEqualTo("TEST_VALUE");
    }

    @Test
    @DisplayName("Should ignore unknown properties with @JsonIgnoreProperties")
    void shouldIgnoreUnknownPropertiesWithJsonIgnoreProperties() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "radio_buttons",
                    "selected_option": {
                        "text": {"type": "plain_text", "text": "Test"},
                        "value": "TEST"
                    },
                    "unknown_field": "some_value",
                    "another_unknown": 123
                }
                """;

        // When
        SlackBlockActionValue value = objectMapper.readValue(json, SlackBlockActionValue.class);

        // Then
        assertThat(value).isNotNull();
        assertThat(value.getType()).isEqualTo("radio_buttons");
        assertThat(value.getSelectedOption()).isNotNull();
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "type": "plain_text_input",
                    "selected_option": null,
                    "selected_date": null,
                    "value": null
                }
                """;

        // When
        SlackBlockActionValue value = objectMapper.readValue(json, SlackBlockActionValue.class);

        // Then
        assertThat(value.getType()).isEqualTo("plain_text_input");
        assertThat(value.getSelectedOption()).isNull();
        assertThat(value.getSelectedDate()).isNull();
        assertThat(value.getValue()).isNull();
    }

    @Test
    @DisplayName("Should serialize and deserialize nested SlackTextObject with all fields")
    void shouldSerializeAndDeserializeNestedSlackTextObjectWithAllFields() throws JsonProcessingException {
        // Given
        SlackBlockActionValue.SlackTextObject textObject = SlackBlockActionValue.SlackTextObject.builder()
                .type("plain_text")
                .text("Test Text")
                .emoji(true)
                .truncate(false)
                .build();

        // When
        String json = objectMapper.writeValueAsString(textObject);
        SlackBlockActionValue.SlackTextObject deserialized = objectMapper.readValue(json, SlackBlockActionValue.SlackTextObject.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getType()).isEqualTo("plain_text");
        assertThat(deserialized.getText()).isEqualTo("Test Text");
        assertThat(deserialized.getEmoji()).isTrue();
        assertThat(deserialized.getTruncate()).isFalse();
    }
}
