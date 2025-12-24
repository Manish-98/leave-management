package one.june.leave_management.adapter.outbound.slack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.june.leave_management.adapter.outbound.slack.builder.SlackBlockBuilder;
import one.june.leave_management.adapter.outbound.slack.builder.SlackModalBuilder;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Slack serialization
 * <p>
 * Verifies that the DTOs serialize to JSON that matches Slack API expectations
 */
class SlackSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSlackTextSerialization() throws JsonProcessingException {
        SlackText text = SlackText.plainText("Hello World", true);

        String json = objectMapper.writeValueAsString(text);

        assertThat(json).isEqualTo("{\"type\":\"plain_text\",\"text\":\"Hello World\",\"emoji\":true}");
    }

    @Test
    void testSlackOptionSerialization() throws JsonProcessingException {
        SlackOption option = SlackOption.of("Full Day", "FULL_DAY");

        String json = objectMapper.writeValueAsString(option);

        assertThat(json).contains("\"text\":{");
        assertThat(json).contains("\"type\":\"plain_text\"");
        assertThat(json).contains("\"text\":\"Full Day\"");
        assertThat(json).contains("\"emoji\":true");
        assertThat(json).contains("\"value\":\"FULL_DAY\"");
    }

    @Test
    void testRadioButtonsInputSerialization() throws JsonProcessingException {
        var options = java.util.List.of(
                SlackOption.of("Option 1", "opt1"),
                SlackOption.of("Option 2", "opt2")
        );

        var block = SlackBlockBuilder.radioButtonsInput("block_id", "action_id", "Label", options, "opt1");

        String json = objectMapper.writeValueAsString(block);

        // Verify structure
        assertThat(json).contains("\"type\":\"input\"");
        assertThat(json).contains("\"block_id\":\"block_id\"");
        assertThat(json).contains("\"label\":{");
        assertThat(json).contains("\"element\":{");
        assertThat(json).contains("\"type\":\"radio_buttons\"");
        assertThat(json).contains("\"action_id\":\"action_id\"");

        // Verify field order: options should come before initial_option
        int optionsIndex = json.indexOf("\"options\"");
        int initialOptionIndex = json.indexOf("\"initial_option\"");
        assertThat(optionsIndex).isLessThan(initialOptionIndex);
    }

    @Test
    void testDateInputSerialization() throws JsonProcessingException {
        var block = SlackBlockBuilder.dateInput("date_block", "date_action", "Select Date", "Choose...", false);

        String json = objectMapper.writeValueAsString(block);

        assertThat(json).contains("\"type\":\"input\"");
        assertThat(json).contains("\"block_id\":\"date_block\"");
        assertThat(json).contains("\"type\":\"datepicker\"");
        assertThat(json).contains("\"action_id\":\"date_action\"");
        assertThat(json).contains("\"placeholder\":{");

        // Optional field should be present (false is not null due to NON_NULL)
        assertThat(json).contains("\"optional\":false");
    }

    @Test
    void testModalSerialization() throws JsonProcessingException {
        var block1 = SlackBlockBuilder.dateInput("b1", "a1", "Date", "Select...", false);
        var block2 = SlackBlockBuilder.plainTextInput("b2", "a2", "Reason", "Enter...", true, true);

        var modal = SlackModalBuilder.create("Test Modal", "test_callback")
                .withBlocks(java.util.List.of(block1, block2))
                .withPrivateMetadata("user123")
                .build();

        String json = objectMapper.writeValueAsString(modal);

        // Verify modal structure
        assertThat(json).contains("\"type\":\"modal\"");
        assertThat(json).contains("\"title\":{");
        assertThat(json).contains("\"text\":\"Test Modal\"");
        assertThat(json).contains("\"callback_id\":\"test_callback\"");
        assertThat(json).contains("\"submit\":{");
        assertThat(json).contains("\"close\":{");
        assertThat(json).contains("\"blocks\":[");

        // Verify blocks are present
        assertThat(json).contains("\"block_id\":\"b1\"");
        assertThat(json).contains("\"block_id\":\"b2\"");

        // Verify private metadata
        assertThat(json).contains("\"private_metadata\":\"user123\"");

        // Verify clear_on_close is present (true is not null)
        assertThat(json).contains("\"clear_on_close\":true");

        // Verify submit_on_close is NOT present (null field)
        assertThat(json).doesNotContain("submit_on_close");
    }

    @Test
    void testLeaveApplicationModalSerialization() throws JsonProcessingException {
        // Build a modal similar to the actual leave application modal
        var leaveTypeOptions = java.util.List.of(
                SlackOption.of("Full Day", "FULL_DAY"),
                SlackOption.of("First Half", "FIRST_HALF"),
                SlackOption.of("Second Half", "SECOND_HALF")
        );

        java.util.List<Object> blocks = java.util.List.of(
                SlackBlockBuilder.radioButtonsInput("leave_type_block", "leave_type_action",
                        "Leave Type", leaveTypeOptions, "FULL_DAY"),
                SlackBlockBuilder.dateInput("start_date_block", "start_date_action",
                        "Start Date", "Select a date", false),
                SlackBlockBuilder.dateInput("end_date_block", "end_date_action",
                        "End Date", "Select a date", true),
                SlackBlockBuilder.plainTextInput("reason_block", "reason_action",
                        "Reason", "Optional: Provide a reason for your leave", true, true)
        );

        var modal = SlackModalBuilder.create("Apply for Leave", "leave_application_submit")
                .withBlocks(blocks)
                .withPrivateMetadata("U0A56D0V59P")
                .build();

        String json = objectMapper.writeValueAsString(modal);

        // Verify key modal properties
        assertThat(json).contains("\"type\":\"modal\"");
        assertThat(json).contains("\"text\":\"Apply for Leave\"");

        // Verify all blocks are present
        assertThat(json).contains("\"block_id\":\"leave_type_block\"");
        assertThat(json).contains("\"block_id\":\"start_date_block\"");
        assertThat(json).contains("\"block_id\":\"end_date_block\"");
        assertThat(json).contains("\"block_id\":\"reason_block\"");

        // Verify radio buttons structure
        assertThat(json).contains("\"type\":\"radio_buttons\"");
        assertThat(json).contains("\"options\":[");

        // Verify date pickers
        assertThat(json).contains("\"type\":\"datepicker\"");

        // Verify plain text input
        assertThat(json).contains("\"type\":\"plain_text_input\"");
        assertThat(json).contains("\"multiline\":true");

        // Verify optional fields are correctly set
        // First two blocks are required (optional not in JSON because false)
        // But we need to check if optional:true appears for end_date and reason
        // Count occurrences of "optional":true - should be 2
        long optionalCount = json.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .reduce("", (a, b) -> a + b)
                .split("\"optional\":true", -1)
                .length - 1;
        assertThat(optionalCount).isEqualTo(2);
    }

    @Test
    void testNullFieldsAreNotSerialized() throws JsonProcessingException {
        var modal = SlackModalBuilder.create("Test", "cb")
                .build();

        String json = objectMapper.writeValueAsString(modal);

        // submit_on_close should not be in JSON (it's null)
        assertThat(json).doesNotContain("submit_on_close");

        // private_metadata should not be in JSON (it's null)
        assertThat(json).doesNotContain("private_metadata");
    }
}
