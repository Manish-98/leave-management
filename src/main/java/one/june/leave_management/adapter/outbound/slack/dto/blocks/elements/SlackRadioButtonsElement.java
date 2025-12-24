package one.june.leave_management.adapter.outbound.slack.dto.blocks.elements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Radio buttons element for Slack input blocks
 * <p>
 * Allows users to select one option from a list of radio buttons
 * <p>
 * Important: Field order matters! The 'options' field must come before 'initial_option'
 * to ensure Slack API correctly parses the initial selection.
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/block-kit/block-elements#radio_buttons">...</a>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "action_id", "options", "initial_option"})
public class SlackRadioButtonsElement {

    /**
     * The type of element - always "radio_buttons"
     * Marked as final since this should never change
     */
    @JsonProperty("type")
    private final String type = "radio_buttons";

    /**
     * An identifier for this action
     * This will be returned when the user interacts with the radio buttons
     */
    @JsonProperty("action_id")
    private String actionId;

    /**
     * The list of options for the radio buttons
     * Must contain at least 1 option and no more than 10 options
     */
    @JsonProperty("options")
    private List<SlackOption> options;

    /**
     * The option that should be selected by default
     * Must be one of the options in the options list
     */
    @JsonProperty("initial_option")
    private SlackOption initialOption;
}
