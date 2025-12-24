package one.june.leave_management.adapter.outbound.slack.builder;

import one.june.leave_management.adapter.outbound.slack.dto.blocks.SlackInputBlock;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackRadioButtonsElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SlackBlockBuilder
 */
class SlackBlockBuilderTest {

    @Test
    void testRadioButtonsInputCreation() {
        List<SlackOption> options = List.of(
                SlackOption.of("Option 1", "opt1"),
                SlackOption.of("Option 2", "opt2")
        );

        SlackInputBlock block = SlackBlockBuilder.radioButtonsInput(
                "test_block",
                "test_action",
                "Test Label",
                options,
                "opt1"
        );

        assertThat(block.getBlockId()).isEqualTo("test_block");
        assertThat(block.getLabel().getText()).isEqualTo("Test Label");
        assertThat(block.getLabel().getEmoji()).isTrue();
        assertThat(block.getElement()).isInstanceOf(SlackRadioButtonsElement.class);

        SlackRadioButtonsElement element = (SlackRadioButtonsElement) block.getElement();
        assertThat(element.getActionId()).isEqualTo("test_action");
        assertThat(element.getOptions()).hasSize(2);
        assertThat(element.getInitialOption()).isNotNull();
        assertThat(element.getInitialOption().getValue()).isEqualTo("opt1");
    }

    @Test
    void testRadioButtonsInputWithoutInitialValue() {
        List<SlackOption> options = List.of(
                SlackOption.of("Option 1", "opt1")
        );

        SlackInputBlock block = SlackBlockBuilder.radioButtonsInput(
                "test_block",
                "test_action",
                "Test Label",
                options,
                "nonexistent"
        );

        SlackRadioButtonsElement element = (SlackRadioButtonsElement) block.getElement();
        assertThat(element.getInitialOption()).isNull();
    }

    @Test
    void testDateInputCreation() {
        SlackInputBlock block = SlackBlockBuilder.dateInput(
                "date_block",
                "date_action",
                "Select Date",
                "Choose a date...",
                false
        );

        assertThat(block.getBlockId()).isEqualTo("date_block");
        assertThat(block.getLabel().getText()).isEqualTo("Select Date");
        assertThat(block.getOptional()).isFalse();

        assertThat(block.getLabel().getEmoji()).isTrue();
    }

    @Test
    void testDateInputOptional() {
        SlackInputBlock block = SlackBlockBuilder.dateInput(
                "date_block",
                "date_action",
                "End Date",
                "Select...",
                true
        );

        assertThat(block.getOptional()).isTrue();
    }

    @Test
    void testPlainTextInputCreation() {
        SlackInputBlock block = SlackBlockBuilder.plainTextInput(
                "text_block",
                "text_action",
                "Enter Text",
                "Type here...",
                false,
                false
        );

        assertThat(block.getBlockId()).isEqualTo("text_block");
        assertThat(block.getLabel().getText()).isEqualTo("Enter Text");
        assertThat(block.getOptional()).isFalse();
    }

    @Test
    void testPlainTextInputMultiline() {
        SlackInputBlock block = SlackBlockBuilder.plainTextInput(
                "text_block",
                "text_action",
                "Reason",
                "Optional...",
                true,
                true
        );

        assertThat(block.getOptional()).isTrue();
        assertThat(block.getLabel().getEmoji()).isTrue();
    }

    @Test
    void testAllInputBlocksHaveCorrectType() {
        List<SlackOption> options = List.of(SlackOption.of("Test", "val"));

        SlackInputBlock radioBlock = SlackBlockBuilder.radioButtonsInput("b", "a", "L", options, "val");
        SlackInputBlock dateBlock = SlackBlockBuilder.dateInput("b", "a", "L", "P", false);
        SlackInputBlock textBlock = SlackBlockBuilder.plainTextInput("b", "a", "L", "P", false, false);

        assertThat(radioBlock.getType()).isEqualTo("input");
        assertThat(dateBlock.getType()).isEqualTo("input");
        assertThat(textBlock.getType()).isEqualTo("input");
    }
}
