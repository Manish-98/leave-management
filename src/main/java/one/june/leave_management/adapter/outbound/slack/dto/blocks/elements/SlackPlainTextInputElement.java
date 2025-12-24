package one.june.leave_management.adapter.outbound.slack.dto.blocks.elements;

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
 * Plain text input element for Slack input blocks
 * <p>
 * Allows users to enter plain text (single-line or multi-line)
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/block-kit/block-elements#input">...</a>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "action_id", "placeholder", "initial_value", "multiline", "min_length", "max_length"})
public class SlackPlainTextInputElement {

    /**
     * The type of element - always "plain_text_input"
     * Marked as final since this should never change
     */
    @JsonProperty("type")
    private final String type = "plain_text_input";

    /**
     * An identifier for this action
     * This will be returned when the user submits the modal
     */
    @JsonProperty("action_id")
    private String actionId;

    /**
     * A plain_text object that defines the placeholder text shown on the text input
     * Maximum length is 150 characters
     */
    @JsonProperty("placeholder")
    private SlackText placeholder;

    /**
     * The initial value in the plain-text input when it is loaded
     * Maximum length is 3000 characters
     */
    @JsonProperty("initial_value")
    private String initialValue;

    /**
     * Indicates whether the input will be a multi-line text input
     * Defaults to false (single-line)
     */
    @JsonProperty("multiline")
    private Boolean multiline;

    /**
     * The minimum length of the input (optional)
     */
    @JsonProperty("min_length")
    private Integer minLength;

    /**
     * The maximum length of the input (optional)
     * Maximum is 3000 characters
     */
    @JsonProperty("max_length")
    private Integer maxLength;
}
