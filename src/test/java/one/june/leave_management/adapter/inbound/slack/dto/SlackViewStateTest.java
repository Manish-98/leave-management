package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlackViewState}
 * <p>
 * Tests complex nested map structure deserialization, empty states,
 * and multiple blocks/actions handling.
 */
@DisplayName("SlackViewState Unit Tests")
class SlackViewStateTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should deserialize with empty values map (default)")
    void shouldDeserializeWithEmptyValuesMap() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "values": {}
                }
                """;

        // When
        SlackViewState state = objectMapper.readValue(json, SlackViewState.class);

        // Then
        assertThat(state).isNotNull();
        assertThat(state.getValues()).isNotNull();
        assertThat(state.getValues()).isEmpty();
    }

    @Test
    @DisplayName("Should deserialize complex nested map structure")
    void shouldDeserializeComplexNestedMapStructure() throws JsonProcessingException {
        // Given
        String json = """
                {
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
                        "start_date_block": {
                            "start_date_action": {
                                "type": "datepicker",
                                "selected_date": "2024-01-15"
                            }
                        }
                    }
                }
                """;

        // When
        SlackViewState state = objectMapper.readValue(json, SlackViewState.class);

        // Then
        assertThat(state).isNotNull();
        assertThat(state.getValues()).isNotNull();
        assertThat(state.getValues()).hasSize(2);

        // Verify first block
        Map<String, SlackBlockActionValue> firstBlock = state.getValues().get("leave_type_category_block");
        assertThat(firstBlock).isNotNull();
        assertThat(firstBlock).hasSize(1);

        SlackBlockActionValue firstAction = firstBlock.get("leave_type_category_action");
        assertThat(firstAction).isNotNull();
        assertThat(firstAction.getType()).isEqualTo("radio_buttons");
        assertThat(firstAction.getSelectedOption()).isNotNull();
        assertThat(firstAction.getSelectedOption().getValue()).isEqualTo("ANNUAL_LEAVE");

        // Verify second block
        Map<String, SlackBlockActionValue> secondBlock = state.getValues().get("start_date_block");
        assertThat(secondBlock).isNotNull();

        SlackBlockActionValue secondAction = secondBlock.get("start_date_action");
        assertThat(secondAction).isNotNull();
        assertThat(secondAction.getType()).isEqualTo("datepicker");
        assertThat(secondAction.getSelectedDate()).isEqualTo("2024-01-15");
    }

    @Test
    @DisplayName("Should build using builder with nested objects")
    void shouldBuildUsingBuilderWithNestedObjects() {
        // Given
        SlackBlockActionValue actionValue = SlackBlockActionValue.builder()
                .type("plain_text_input")
                .value("Sample text")
                .build();

        Map<String, SlackBlockActionValue> actions = Map.of("action_id", actionValue);
        Map<String, Map<String, SlackBlockActionValue>> values = Map.of("block_id", actions);

        // When
        SlackViewState state = SlackViewState.builder()
                .values(values)
                .build();

        // Then
        assertThat(state).isNotNull();
        assertThat(state.getValues()).isNotNull();
        assertThat(state.getValues()).hasSize(1);
        assertThat(state.getValues().get("block_id")).isNotNull();
        assertThat(state.getValues().get("block_id").get("action_id")).isEqualTo(actionValue);
    }

    @Test
    @DisplayName("Should handle null values map")
    void shouldHandleNullValuesMap() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "values": null
                }
                """;

        // When
        SlackViewState state = objectMapper.readValue(json, SlackViewState.class);

        // Then
        assertThat(state).isNotNull();
        assertThat(state.getValues()).isNull();
    }

    @Test
    @DisplayName("Should support multiple actions per block")
    void shouldSupportMultipleActionsPerBlock() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "values": {
                        "block_id": {
                            "action1": {
                                "type": "plain_text_input",
                                "value": "value1"
                            },
                            "action2": {
                                "type": "plain_text_input",
                                "value": "value2"
                            }
                        }
                    }
                }
                """;

        // When
        SlackViewState state = objectMapper.readValue(json, SlackViewState.class);

        // Then
        assertThat(state).isNotNull();
        assertThat(state.getValues()).hasSize(1);

        Map<String, SlackBlockActionValue> block = state.getValues().get("block_id");
        assertThat(block).hasSize(2);
        assertThat(block.get("action1").getValue()).isEqualTo("value1");
        assertThat(block.get("action2").getValue()).isEqualTo("value2");
    }

    @Test
    @DisplayName("Should serialize to correct JSON format")
    void shouldSerializeToCorrectJsonFormat() throws JsonProcessingException {
        // Given
        SlackBlockActionValue actionValue = SlackBlockActionValue.builder()
                .type("datepicker")
                .selectedDate("2024-01-15")
                .build();

        Map<String, SlackBlockActionValue> actions = Map.of("action_id", actionValue);
        Map<String, Map<String, SlackBlockActionValue>> values = Map.of("block_id", actions);

        SlackViewState state = SlackViewState.builder()
                .values(values)
                .build();

        // When
        String json = objectMapper.writeValueAsString(state);

        // Then
        assertThat(json).contains("\"values\"");
        assertThat(json).contains("\"block_id\"");
        assertThat(json).contains("\"action_id\"");
        assertThat(json).contains("\"datepicker\"");
        assertThat(json).contains("\"2024-01-15\"");
    }
}
