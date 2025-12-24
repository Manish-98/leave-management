package one.june.leave_management.adapter.outbound.slack.dto.blocks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;

/**
 * Input block for Slack modals
 * <p>
 * A block that collects information from users
 * It contains a label and an interactive element (text input, radio buttons, etc.)
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/block-kit/blocks#input">...</a>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "block_id", "label", "element", "hint", "optional", "dispatch_action"})
public class SlackInputBlock {

    /**
     * The type of block - always "input"
     * Marked as final since this should never change
     */
    @JsonProperty("type")
    private final String type = "input";

    /**
     * A unique identifier for this block
     * Maximum 255 characters
     */
    @JsonProperty("block_id")
    private String blockId;

    /**
     * A label that appears above the input element
     * Maximum 2000 characters
     */
    @JsonProperty("label")
    private SlackText label;

    /**
     * An interactive element that appears inside the block
     * Can be: plain_text_input, radio_buttons, static_select, etc.
     */
    @JsonProperty("element")
    private Object element;

    /**
     * A helpful text that appears underneath the input element
     * Maximum 2000 characters
     */
    @JsonProperty("hint")
    private SlackText hint;

    /**
     * A boolean indicating whether this input is optional or required
     * If false (required), the user must provide a value before submitting
     * Defaults to false (required)
     */
    @JsonProperty("optional")
    private Boolean optional;

    /**
     * A boolean that triggers the modal to submit when the user changes the input
     * Defaults to false
     */
    @JsonProperty("dispatch_action")
    private Boolean dispatchAction;
}
