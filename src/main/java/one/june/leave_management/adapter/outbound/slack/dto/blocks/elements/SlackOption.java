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
 * Option object for Slack interactive elements
 * <p>
 * Used in radio_buttons, static_select, and other select elements
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/block-kit/composition-objects#option">...</a>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"text", "value", "description"})
public class SlackOption {

    /**
     * The text that appears in the option
     */
    @JsonProperty("text")
    private SlackText text;

    /**
     * The value that is sent to your app when this option is selected
     */
    @JsonProperty("value")
    private String value;

    /**
     * Additional information about the option (optional)
     */
    @JsonProperty("description")
    private SlackText description;

    /**
     * Creates a simple option with plain text label
     * <p>
     * Factory method for the most common use case
     *
     * @param label The text label to display
     * @param value The value to send when selected
     * @return A new SlackOption instance with emoji enabled
     */
    public static SlackOption of(String label, String value) {
        return SlackOption.builder()
                .text(SlackText.plainText(label, true))
                .value(value)
                .build();
    }

    /**
     * Creates an option with a text object (for more control)
     *
     * @param text The text object for the label
     * @param value The value to send when selected
     * @return A new SlackOption instance
     */
    public static SlackOption of(SlackText text, String value) {
        return SlackOption.builder()
                .text(text)
                .value(value)
                .build();
    }
}
