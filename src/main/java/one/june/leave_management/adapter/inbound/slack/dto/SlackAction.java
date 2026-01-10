package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a Slack action within a block_actions interaction.
 * Contains information about which block/action was triggered.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackAction {

    @JsonProperty("action_id")
    private String actionId; // The action_id of the interactive element

    @JsonProperty("block_id")
    private String blockId; // The block_id containing the element

    @JsonProperty("type")
    private String type; // "radio_buttons", "static_select", etc.

    @JsonProperty("selected_option")
    private SlackSelectedOption selectedOption; // For radio_buttons and selects

    @JsonProperty("initial_option")
    private SlackSelectedOption initialOption; // For static_select elements

    @JsonProperty("placeholder")
    private SlackSelectedOption.SlackOptionText placeholder; // For select elements

    @JsonProperty("value")
    private String value; // For buttons and some other elements

    @JsonProperty("text")
    private SlackSelectedOption.SlackOptionText text; // Button text object

    @JsonProperty("action_ts")
    private String actionTs; // Timestamp when action was triggered

    /**
     * Represents the selected option from radio_buttons or select elements.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SlackSelectedOption {
        @JsonProperty("text")
        private SlackOptionText text;

        @JsonProperty("value")
        private String value; // The actual value sent to the app

        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @ToString
        public static class SlackOptionText {
            @JsonProperty("type")
            private String type; // "plain_text", "mrkdwn"

            @JsonProperty("text")
            private String text; // The display text

            @JsonProperty("emoji")
            private Boolean emoji; // Whether to enable emoji
        }
    }
}
