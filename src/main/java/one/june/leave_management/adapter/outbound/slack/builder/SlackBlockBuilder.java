package one.june.leave_management.adapter.outbound.slack.builder;

import one.june.leave_management.adapter.outbound.slack.dto.blocks.SlackInputBlock;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackDatePickerElement;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackPlainTextInputElement;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackRadioButtonsElement;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;

import java.util.List;

/**
 * Fluent builder for creating Slack blocks
 * <p>
 * Provides convenient static methods for creating common block patterns
 * Simplifies the construction of input blocks with various element types
 */
public class SlackBlockBuilder {

    /**
     * Creates an input block with radio buttons element
     * <p>
     * This is a convenience method for creating a radio buttons input block
     * with all the necessary components properly configured
     *
     * @param blockId      The unique identifier for this block
     * @param actionId     The action identifier for the radio buttons element
     * @param labelText    The label text to display above the radio buttons
     * @param options      The list of options to display
     * @param initialValue The value of the option that should be selected by default
     * @return A configured SlackInputBlock with radio buttons element
     */
    public static SlackInputBlock radioButtonsInput(
            String blockId,
            String actionId,
            String labelText,
            List<SlackOption> options,
            String initialValue) {

        // Find the initial option from the list
        SlackOption initialOption = options.stream()
                .filter(opt -> opt.getValue().equals(initialValue))
                .findFirst()
                .orElse(null);

        return SlackInputBlock.builder()
                .blockId(blockId)
                .label(SlackText.plainText(labelText, true))
                .element(SlackRadioButtonsElement.builder()
                        .actionId(actionId)
                        .options(options)
                        .initialOption(initialOption)
                        .build())
                .build();
    }

    /**
     * Creates an input block with date picker element
     * <p>
     * This is a convenience method for creating a date input block
     *
     * @param blockId         The unique identifier for this block
     * @param actionId        The action identifier for the date picker element
     * @param labelText       The label text to display above the date picker
     * @param placeholderText The placeholder text to show when no date is selected
     * @param optional        Whether this field is optional (true) or required (false)
     * @return A configured SlackInputBlock with date picker element
     */
    public static SlackInputBlock dateInput(
            String blockId,
            String actionId,
            String labelText,
            String placeholderText,
            boolean optional) {

        return SlackInputBlock.builder()
                .blockId(blockId)
                .label(SlackText.plainText(labelText, true))
                .element(SlackDatePickerElement.builder()
                        .actionId(actionId)
                        .placeholder(SlackText.plainText(placeholderText, true))
                        .build())
                .optional(optional)
                .build();
    }

    /**
     * Creates an input block with plain text input element
     * <p>
     * This is a convenience method for creating a text input block
     *
     * @param blockId         The unique identifier for this block
     * @param actionId        The action identifier for the text input element
     * @param labelText       The label text to display above the text input
     * @param placeholderText The placeholder text to show in the input field
     * @param multiline       Whether to allow multi-line input
     * @param optional        Whether this field is optional (true) or required (false)
     * @return A configured SlackInputBlock with plain text input element
     */
    public static SlackInputBlock plainTextInput(
            String blockId,
            String actionId,
            String labelText,
            String placeholderText,
            boolean multiline,
            boolean optional) {

        return SlackInputBlock.builder()
                .blockId(blockId)
                .label(SlackText.plainText(labelText, true))
                .element(SlackPlainTextInputElement.builder()
                        .actionId(actionId)
                        .placeholder(SlackText.plainText(placeholderText, true))
                        .multiline(multiline)
                        .build())
                .optional(optional)
                .build();
    }
}
