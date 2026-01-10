package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SlackAction
 * Tests deserialization and structure of Slack action objects from block interactions
 */
@DisplayName("SlackAction Unit Tests")
class SlackActionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("Deserialization Tests")
    class DeserializationTests {

        @Test
        @DisplayName("Should deserialize radio button action with selected option")
        void shouldDeserializeRadioButtonAction() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"leave_type_category\","
                    + "\"block_id\":\"leave_type_category_block\","
                    + "\"type\":\"radio_buttons\","
                    + "\"selected_option\":{"
                    + "  \"text\":{"
                    + "    \"type\":\"plain_text\","
                    + "    \"text\":\"Annual Leave\","
                    + "    \"emoji\":true"
                    + "  },"
                    + "  \"value\":\"ANNUAL_LEAVE\""
                    + "},"
                    + "\"action_ts\":\"1234567890.123456\""
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action).isNotNull();
            assertThat(action.getActionId()).isEqualTo("leave_type_category");
            assertThat(action.getBlockId()).isEqualTo("leave_type_category_block");
            assertThat(action.getType()).isEqualTo("radio_buttons");
            assertThat(action.getSelectedOption()).isNotNull();
            assertThat(action.getSelectedOption().getText().getType()).isEqualTo("plain_text");
            assertThat(action.getSelectedOption().getText().getText()).isEqualTo("Annual Leave");
            assertThat(action.getSelectedOption().getText().getEmoji()).isTrue();
            assertThat(action.getSelectedOption().getValue()).isEqualTo("ANNUAL_LEAVE");
            assertThat(action.getActionTs()).isEqualTo("1234567890.123456");
        }

        @Test
        @DisplayName("Should deserialize static select action with selected option")
        void shouldDeserializeStaticSelectAction() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"duration_select\","
                    + "\"block_id\":\"duration_block\","
                    + "\"type\":\"static_select\","
                    + "\"selected_option\":{"
                    + "  \"text\":{"
                    + "    \"type\":\"plain_text\","
                    + "    \"text\":\"Full Day\""
                    + "  },"
                    + "  \"value\":\"FULL_DAY\""
                    + "},"
                    + "\"placeholder\":{"
                    + "  \"type\":\"plain_text\","
                    + "  \"text\":\"Select duration\""
                    + "},"
                    + "\"action_ts\":\"1234567890.123456\""
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action).isNotNull();
            assertThat(action.getActionId()).isEqualTo("duration_select");
            assertThat(action.getType()).isEqualTo("static_select");
            assertThat(action.getSelectedOption().getValue()).isEqualTo("FULL_DAY");
            assertThat(action.getPlaceholder()).isNotNull();
            assertThat(action.getPlaceholder().getType()).isEqualTo("plain_text");
            assertThat(action.getPlaceholder().getText()).isEqualTo("Select duration");
        }

        @Test
        @DisplayName("Should deserialize button action with value and text")
        void shouldDeserializeButtonAction() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"confirm_button\","
                    + "\"block_id\":\"action_block\","
                    + "\"type\":\"button\","
                    + "\"value\":\"confirm\","
                    + "\"text\":{"
                    + "  \"type\":\"plain_text\","
                    + "  \"text\":\"Confirm\","
                    + "  \"emoji\":true"
                    + "},"
                    + "\"action_ts\":\"1234567890.123456\""
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action).isNotNull();
            assertThat(action.getActionId()).isEqualTo("confirm_button");
            assertThat(action.getType()).isEqualTo("button");
            assertThat(action.getValue()).isEqualTo("confirm");
            assertThat(action.getText()).isNotNull();
            assertThat(action.getText().getType()).isEqualTo("plain_text");
            assertThat(action.getText().getText()).isEqualTo("Confirm");
            assertThat(action.getText().getEmoji()).isTrue();
        }

        @Test
        @DisplayName("Should deserialize action with initial option")
        void shouldDeserializeActionWithInitialOption() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"leave_type\","
                    + "\"block_id\":\"leave_type_block\","
                    + "\"type\":\"static_select\","
                    + "\"initial_option\":{"
                    + "  \"text\":{"
                    + "    \"type\":\"plain_text\","
                    + "    \"text\":\"Annual Leave\""
                    + "  },"
                    + "  \"value\":\"ANNUAL_LEAVE\""
                    + "}"
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action).isNotNull();
            assertThat(action.getInitialOption()).isNotNull();
            assertThat(action.getInitialOption().getText().getText()).isEqualTo("Annual Leave");
            assertThat(action.getInitialOption().getValue()).isEqualTo("ANNUAL_LEAVE");
        }

        @Test
        @DisplayName("Should deserialize minimal action")
        void shouldDeserializeMinimalAction() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"action1\","
                    + "\"type\":\"button\""
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action).isNotNull();
            assertThat(action.getActionId()).isEqualTo("action1");
            assertThat(action.getType()).isEqualTo("button");
        }

        @Test
        @DisplayName("Should deserialize action with mrkdwn text type")
        void shouldDeserializeActionWithMrkdwnText() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"action1\","
                    + "\"type\":\"radio_buttons\","
                    + "\"selected_option\":{"
                    + "  \"text\":{"
                    + "    \"type\":\"mrkdwn\","
                    + "    \"text\":\"*Bold* text\""
                    + "  },"
                    + "  \"value\":\"value1\""
                    + "}"
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action.getSelectedOption().getText().getType()).isEqualTo("mrkdwn");
            assertThat(action.getSelectedOption().getText().getText()).isEqualTo("*Bold* text");
        }

        @Test
        @DisplayName("Should deserialize action without emoji field")
        void shouldDeserializeActionWithoutEmoji() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"action1\","
                    + "\"type\":\"button\","
                    + "\"text\":{"
                    + "  \"type\":\"plain_text\","
                    + "  \"text\":\"Click me\""
                    + "}"
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action.getText()).isNotNull();
            assertThat(action.getText().getEmoji()).isNull(); // emoji field is optional
        }

        @Test
        @DisplayName("Should ignore unknown properties")
        void shouldIgnoreUnknownProperties() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"action1\","
                    + "\"type\":\"button\","
                    + "\"unknown_field\":\"unknown_value\","
                    + "\"another_unknown\":123"
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action).isNotNull();
            assertThat(action.getActionId()).isEqualTo("action1");
        }

        @Test
        @DisplayName("Should deserialize action with null optional fields")
        void shouldDeserializeActionWithNullOptionalFields() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"action1\","
                    + "\"block_id\":null,"
                    + "\"type\":\"button\","
                    + "\"value\":null,"
                    + "\"action_ts\":null"
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action.getActionId()).isEqualTo("action1");
            assertThat(action.getBlockId()).isNull();
            assertThat(action.getValue()).isNull();
            assertThat(action.getActionTs()).isNull();
        }

        @Test
        @DisplayName("Should deserialize action with empty strings")
        void shouldDeserializeActionWithEmptyStrings() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"\","
                    + "\"block_id\":\"\","
                    + "\"type\":\"button\","
                    + "\"value\":\"\""
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action.getActionId()).isEmpty();
            assertThat(action.getBlockId()).isEmpty();
            assertThat(action.getValue()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build complete action using builder")
        void shouldBuildCompleteAction() {
            // Given
            SlackAction.SlackSelectedOption.SlackOptionText text =
                    SlackAction.SlackSelectedOption.SlackOptionText.builder()
                            .type("plain_text")
                            .text("Annual Leave")
                            .emoji(true)
                            .build();

            SlackAction.SlackSelectedOption selectedOption =
                    SlackAction.SlackSelectedOption.builder()
                            .text(text)
                            .value("ANNUAL_LEAVE")
                            .build();

            // When
            SlackAction action = SlackAction.builder()
                    .actionId("leave_type_category")
                    .blockId("leave_type_category_block")
                    .type("radio_buttons")
                    .selectedOption(selectedOption)
                    .actionTs("1234567890.123456")
                    .build();

            // Then
            assertThat(action.getActionId()).isEqualTo("leave_type_category");
            assertThat(action.getBlockId()).isEqualTo("leave_type_category_block");
            assertThat(action.getType()).isEqualTo("radio_buttons");
            assertThat(action.getSelectedOption()).isNotNull();
            assertThat(action.getSelectedOption().getText().getText()).isEqualTo("Annual Leave");
            assertThat(action.getSelectedOption().getValue()).isEqualTo("ANNUAL_LEAVE");
            assertThat(action.getActionTs()).isEqualTo("1234567890.123456");
        }

        @Test
        @DisplayName("Should build button action with text")
        void shouldBuildButtonActionWithText() {
            // Given
            SlackAction.SlackSelectedOption.SlackOptionText text =
                    SlackAction.SlackSelectedOption.SlackOptionText.builder()
                            .type("plain_text")
                            .text("Confirm")
                            .emoji(true)
                            .build();

            // When
            SlackAction action = SlackAction.builder()
                    .actionId("confirm_button")
                    .blockId("action_block")
                    .type("button")
                    .value("confirm")
                    .text(text)
                    .build();

            // Then
            assertThat(action.getActionId()).isEqualTo("confirm_button");
            assertThat(action.getType()).isEqualTo("button");
            assertThat(action.getValue()).isEqualTo("confirm");
            assertThat(action.getText()).isNotNull();
            assertThat(action.getText().getText()).isEqualTo("Confirm");
            assertThat(action.getText().getEmoji()).isTrue();
        }

        @Test
        @DisplayName("Should build action with placeholder")
        void shouldBuildActionWithPlaceholder() {
            // Given
            SlackAction.SlackSelectedOption.SlackOptionText placeholder =
                    SlackAction.SlackSelectedOption.SlackOptionText.builder()
                            .type("plain_text")
                            .text("Select an option")
                            .build();

            // When
            SlackAction action = SlackAction.builder()
                    .actionId("select_action")
                    .type("static_select")
                    .placeholder(placeholder)
                    .build();

            // Then
            assertThat(action.getActionId()).isEqualTo("select_action");
            assertThat(action.getPlaceholder()).isNotNull();
            assertThat(action.getPlaceholder().getText()).isEqualTo("Select an option");
        }

        @Test
        @DisplayName("Should build action with initial option")
        void shouldBuildActionWithInitialOption() {
            // Given
            SlackAction.SlackSelectedOption.SlackOptionText text =
                    SlackAction.SlackSelectedOption.SlackOptionText.builder()
                            .type("plain_text")
                            .text("Option 1")
                            .build();

            SlackAction.SlackSelectedOption initialOption =
                    SlackAction.SlackSelectedOption.builder()
                            .text(text)
                            .value("option1")
                            .build();

            // When
            SlackAction action = SlackAction.builder()
                    .actionId("select_action")
                    .type("static_select")
                    .initialOption(initialOption)
                    .build();

            // Then
            assertThat(action.getInitialOption()).isNotNull();
            assertThat(action.getInitialOption().getValue()).isEqualTo("option1");
        }
    }

    @Nested
    @DisplayName("Nested Classes Tests")
    class NestedClassesTests {

        @Test
        @DisplayName("Should build SlackOptionText with all fields")
        void shouldBuildSlackOptionTextWithAllFields() {
            // When
            SlackAction.SlackSelectedOption.SlackOptionText optionText =
                    SlackAction.SlackSelectedOption.SlackOptionText.builder()
                            .type("plain_text")
                            .text("Display Text")
                            .emoji(true)
                            .build();

            // Then
            assertThat(optionText.getType()).isEqualTo("plain_text");
            assertThat(optionText.getText()).isEqualTo("Display Text");
            assertThat(optionText.getEmoji()).isTrue();
        }

        @Test
        @DisplayName("Should build SlackOptionText without emoji")
        void shouldBuildSlackOptionTextWithoutEmoji() {
            // When
            SlackAction.SlackSelectedOption.SlackOptionText optionText =
                    SlackAction.SlackSelectedOption.SlackOptionText.builder()
                            .type("mrkdwn")
                            .text("Markdown text")
                            .build();

            // Then
            assertThat(optionText.getType()).isEqualTo("mrkdwn");
            assertThat(optionText.getText()).isEqualTo("Markdown text");
            assertThat(optionText.getEmoji()).isNull();
        }

        @Test
        @DisplayName("Should build SlackSelectedOption with text and value")
        void shouldBuildSlackSelectedOption() {
            // Given
            SlackAction.SlackSelectedOption.SlackOptionText text =
                    SlackAction.SlackSelectedOption.SlackOptionText.builder()
                            .type("plain_text")
                            .text("Option")
                            .build();

            // When
            SlackAction.SlackSelectedOption selectedOption =
                    SlackAction.SlackSelectedOption.builder()
                            .text(text)
                            .value("option_value")
                            .build();

            // Then
            assertThat(selectedOption.getText()).isNotNull();
            assertThat(selectedOption.getText().getText()).isEqualTo("Option");
            assertThat(selectedOption.getValue()).isEqualTo("option_value");
        }

        @Test
        @DisplayName("Should build SlackSelectedOption with null text")
        void shouldBuildSlackSelectedOptionWithNullText() {
            // When
            SlackAction.SlackSelectedOption selectedOption =
                    SlackAction.SlackSelectedOption.builder()
                            .text(null)
                            .value("value1")
                            .build();

            // Then
            assertThat(selectedOption.getText()).isNull();
            assertThat(selectedOption.getValue()).isEqualTo("value1");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null selected option")
        void shouldHandleNullSelectedOption() {
            // When
            SlackAction action = SlackAction.builder()
                    .actionId("action1")
                    .type("button")
                    .selectedOption(null)
                    .build();

            // Then
            assertThat(action.getSelectedOption()).isNull();
        }

        @Test
        @DisplayName("Should handle null text object")
        void shouldHandleNullTextObject() {
            // When
            SlackAction action = SlackAction.builder()
                    .actionId("action1")
                    .type("button")
                    .text(null)
                    .build();

            // Then
            assertThat(action.getText()).isNull();
        }

        @Test
        @DisplayName("Should handle null placeholder")
        void shouldHandleNullPlaceholder() {
            // When
            SlackAction action = SlackAction.builder()
                    .actionId("action1")
                    .type("static_select")
                    .placeholder(null)
                    .build();

            // Then
            assertThat(action.getPlaceholder()).isNull();
        }

        @Test
        @DisplayName("Should handle null initial option")
        void shouldHandleNullInitialOption() {
            // When
            SlackAction action = SlackAction.builder()
                    .actionId("action1")
                    .type("static_select")
                    .initialOption(null)
                    .build();

            // Then
            assertThat(action.getInitialOption()).isNull();
        }

        @Test
        @DisplayName("Should build action with all null optional fields")
        void shouldBuildActionWithAllNullOptionalFields() {
            // When
            SlackAction action = SlackAction.builder()
                    .actionId("action1")
                    .blockId(null)
                    .type("button")
                    .selectedOption(null)
                    .initialOption(null)
                    .placeholder(null)
                    .value(null)
                    .text(null)
                    .actionTs(null)
                    .build();

            // Then
            assertThat(action.getActionId()).isEqualTo("action1");
            assertThat(action.getBlockId()).isNull();
            assertThat(action.getSelectedOption()).isNull();
            assertThat(action.getInitialOption()).isNull();
            assertThat(action.getPlaceholder()).isNull();
            assertThat(action.getValue()).isNull();
            assertThat(action.getText()).isNull();
            assertThat(action.getActionTs()).isNull();
        }

        @Test
        @DisplayName("Should handle special characters in text")
        void shouldHandleSpecialCharactersInText() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"action1\","
                    + "\"type\":\"button\","
                    + "\"text\":{"
                    + "  \"type\":\"plain_text\","
                    + "  \"text\":\"Hello <@user> & goodbye!\""
                    + "}"
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action.getText().getText()).isEqualTo("Hello <@user> & goodbye!");
        }

        @Test
        @DisplayName("Should handle unicode characters in text")
        void shouldHandleUnicodeCharactersInText() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"action_id\":\"action1\","
                    + "\"type\":\"button\","
                    + "\"text\":{"
                    + "  \"type\":\"plain_text\","
                    + "  \"text\":\"🎉 Party time! 👍\""
                    + "}"
                    + "}";

            // When
            SlackAction action = objectMapper.readValue(json, SlackAction.class);

            // Then
            assertThat(action.getText().getText()).isEqualTo("🎉 Party time! 👍");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate toString with all fields")
        void shouldGenerateToStringWithAllFields() {
            // Given
            SlackAction action = SlackAction.builder()
                    .actionId("action1")
                    .blockId("block1")
                    .type("button")
                    .value("value1")
                    .build();

            // When
            String str = action.toString();

            // Then
            assertThat(str).contains("actionId=action1");
            assertThat(str).contains("blockId=block1");
            assertThat(str).contains("type=button");
            assertThat(str).contains("value=value1");
        }
    }
}
