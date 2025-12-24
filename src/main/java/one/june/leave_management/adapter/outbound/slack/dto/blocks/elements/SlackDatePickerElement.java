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
 * Date picker element for Slack input blocks
 * <p>
 * Allows users to select a date from a calendar picker
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/block-kit/block-elements#datepicker">...</a>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "action_id", "placeholder", "initial_date"})
public class SlackDatePickerElement {

    /**
     * The type of element - always "datepicker"
     * Marked as final since this should never change
     */
    @JsonProperty("type")
    private final String type = "datepicker";

    /**
     * An identifier for this action
     * This will be returned when the user selects a date
     */
    @JsonProperty("action_id")
    private String actionId;

    /**
     * A plain_text object that defines the placeholder text shown on the date picker
     * Maximum length is 150 characters
     */
    @JsonProperty("placeholder")
    private SlackText placeholder;

    /**
     * The initial date that is selected when the element is loaded
     * Format should be YYYY-MM-DD
     */
    @JsonProperty("initial_date")
    private String initialDate;
}
