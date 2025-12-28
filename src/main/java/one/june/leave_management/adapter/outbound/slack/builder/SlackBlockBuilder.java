package one.june.leave_management.adapter.outbound.slack.builder;

import one.june.leave_management.adapter.outbound.slack.dto.blocks.SlackInputBlock;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.SlackSectionBlock;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackDatePickerElement;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackPlainTextInputElement;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackRadioButtonsElement;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackStaticSelectElement;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @param blockId        The unique identifier for this block
     * @param actionId       The action identifier for the radio buttons element
     * @param labelText       The label text to display above the radio buttons
     * @param options         The list of options to display
     * @param initialValue    The value of the option that should be selected by default
     * @param dispatchAction  Whether to trigger block_actions on change (optional, for dynamic modals)
     * @return A configured SlackInputBlock with radio buttons element
     */
    public static SlackInputBlock radioButtonsInput(
            String blockId,
            String actionId,
            String labelText,
            List<SlackOption> options,
            String initialValue,
            Boolean dispatchAction) {

        // Find the initial option from the list
        SlackOption initialOption = options.stream()
                .filter(opt -> opt.getValue().equals(initialValue))
                .findFirst()
                .orElse(null);

        SlackRadioButtonsElement.SlackRadioButtonsElementBuilder builder =
                SlackRadioButtonsElement.builder()
                        .actionId(actionId)
                        .options(options)
                        .initialOption(initialOption);

        // Set dispatch_action if provided (enables dynamic modal updates)
        if (dispatchAction != null && dispatchAction) {
            builder.dispatchActions(true);
        }

        return SlackInputBlock.builder()
                .blockId(blockId)
                .label(SlackText.plainText(labelText, true))
                .element(builder.build())
                .build();
    }

    /**
     * Creates an input block with radio buttons element (without dispatch_action)
     * <p>
     * This is a convenience method for creating a radio buttons input block
     * with all the necessary components properly configured.
     * Does not enable dispatch_action (modal won't update on selection change).
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
        return radioButtonsInput(blockId, actionId, labelText, options, initialValue, null);
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

    /**
     * Creates an input block with static select dropdown element
     * <p>
     * This is a convenience method for creating a dropdown select input block.
     * Users can select one option from a predefined list.
     * <p>
     * Slack API reference: <a href="https://api.slack.com/reference/block-kit/block-elements#static_select">...</a>
     *
     * @param blockId         The unique identifier for this block
     * @param actionId        The action identifier for the select element
     * @param labelText       The label text to display above the dropdown
     * @param options         The list of options to display in the dropdown
     * @param placeholderText The placeholder text to show when no option is selected (optional)
     * @param initialValue    The value of the option that should be selected by default (optional)
     * @return A configured SlackInputBlock with static select element
     */
    public static SlackInputBlock staticSelectInput(
            String blockId,
            String actionId,
            String labelText,
            List<SlackOption> options,
            String placeholderText,
            String initialValue) {

        // Build the select element
        SlackStaticSelectElement.SlackStaticSelectElementBuilder builder =
                SlackStaticSelectElement.builder()
                        .actionId(actionId)
                        .options(options);

        // Set placeholder if provided
        if (placeholderText != null && !placeholderText.trim().isEmpty()) {
            builder.placeholder(SlackText.plainText(placeholderText, true));
        }

        // Set initial option if provided
        if (initialValue != null && !initialValue.trim().isEmpty()) {
            SlackOption initialOption = options.stream()
                    .filter(opt -> opt.getValue().equals(initialValue))
                    .findFirst()
                    .orElse(null);
            if (initialOption != null) {
                builder.initialOption(initialOption);
            }
        }

        return SlackInputBlock.builder()
                .blockId(blockId)
                .label(SlackText.plainText(labelText, true))
                .element(builder.build())
                .optional(false) // Select is required by default
                .build();
    }

    // ========== Message Block Methods ==========

    /**
     * Creates a header block for messages
     * <p>
     * Header blocks display text in a bold, larger format.
     * Used for titles and section headers in messages.
     *
     * @param text  The header text content
     * @param emoji Whether to allow emoji in the text
     * @return A map representing the header block structure
     */
    public static Map<String, Object> headerBlock(String text, boolean emoji) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "header");

        Map<String, Object> textObj = new HashMap<>();
        textObj.put("type", "plain_text");
        textObj.put("text", text);
        textObj.put("emoji", emoji);

        block.put("text", textObj);
        return block;
    }

    /**
     * Creates a section block with markdown text
     * <p>
     * Section blocks are the most common block type, used to display
     * text content with markdown formatting.
     *
     * @param markdownText The markdown-formatted text content
     * @return A map representing the section block structure
     */
    public static Map<String, Object> sectionBlock(String markdownText) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "section");

        Map<String, String> textObj = new HashMap<>();
        textObj.put("type", "mrkdwn");
        textObj.put("text", markdownText);

        block.put("text", textObj);
        return block;
    }

    /**
     * Creates a section block with a label and value
     * <p>
     * Convenience method for creating key-value pairs in markdown format.
     *
     * @param label The field label (will be bold)
     * @param value The field value
     * @return A map representing the section block structure
     */
    public static Map<String, Object> sectionBlock(String label, String value) {
        return sectionBlock(String.format("*%s:* %s", label, value));
    }

    /**
     * Creates a section block with multiple fields
     * <p>
     * Fields are displayed side-by-side in columns (up to 5 fields).
     * Each field is a key-value pair.
     *
     * @param fields Map of field labels to values
     * @return A map representing the section block structure
     */
    public static Map<String, Object> fieldsBlock(Map<String, String> fields) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "section");

        List<Map<String, String>> fieldList = fields.entrySet().stream()
                .map(entry -> {
                    Map<String, String> field = new HashMap<>();
                    field.put("type", "mrkdwn");
                    field.put("text", String.format("*%s:* %s", entry.getKey(), entry.getValue()));
                    return field;
                })
                .toList();

        block.put("fields", fieldList);
        return block;
    }

    /**
     * Creates a divider block
     * <p>
     * Divider blocks display a visual separator line between sections.
     *
     * @return A map representing the divider block structure
     */
    public static Map<String, Object> dividerBlock() {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "divider");
        return block;
    }

    /**
     * Creates a context block with markdown text
     * <p>
     * Context blocks display auxiliary information in a smaller font,
     * typically used for metadata, timestamps, or status indicators.
     *
     * @param markdownText The markdown-formatted text content
     * @return A map representing the context block structure
     */
    public static Map<String, Object> contextBlock(String markdownText) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "context");

        Map<String, String> textObj = new HashMap<>();
        textObj.put("type", "mrkdwn");
        textObj.put("text", markdownText);

        block.put("elements", List.of(textObj));
        return block;
    }

    // ========== Section Block Methods for Modals ==========

    /**
     * Creates a section block with a radio buttons accessory
     * <p>
     * This creates a section block that displays text and includes radio buttons
     * as an interactive accessory. Unlike input blocks, section blocks with
     * accessories support dispatch_action in modals, enabling dynamic updates.
     * <p>
     * Use this for leave type selection, where you want the modal to update
     * immediately when the user changes their selection.
     *
     * @param blockId        The unique identifier for this block
     * @param labelMarkdown  The label text (markdown formatted, e.g., "*Leave Type*")
     * @param radioButtons   The radio buttons element to include as accessory
     * @return A configured SlackSectionBlock
     */
    public static SlackSectionBlock sectionWithRadioButtons(
            String blockId,
            String labelMarkdown,
            SlackRadioButtonsElement radioButtons) {
        return SlackSectionBlock.builder()
                .blockId(blockId)
                .text(SlackText.markdown(labelMarkdown))
                .accessory(radioButtons)
                .build();
    }

    /**
     * Creates a section block with a static select accessory
     * <p>
     * This creates a section block that displays text and includes a dropdown
     * menu as an interactive accessory. Supports dispatch_action in modals.
     * <p>
     * Use this for selections where you want dynamic modal updates.
     *
     * @param blockId      The unique identifier for this block
     * @param labelMarkdown The label text (markdown formatted)
     * @param selectElement The static select element to include as accessory
     * @return A configured SlackSectionBlock
     */
    public static SlackSectionBlock sectionWithStaticSelect(
            String blockId,
            String labelMarkdown,
            SlackStaticSelectElement selectElement) {
        return SlackSectionBlock.builder()
                .blockId(blockId)
                .text(SlackText.markdown(labelMarkdown))
                .accessory(selectElement)
                .build();
    }
}
