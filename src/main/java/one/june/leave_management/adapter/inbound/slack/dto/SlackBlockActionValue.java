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
 * Represents the value of a block action in a Slack view submission
 * <p>
 * This is the actual value object for a specific action_id within a block.
 * Different action types have different value structures:
 * - radio_buttons: has "selected_option" with text and value
 * - datepicker: has "selected_date" with ISO date string
 * - plain_text_input: has "value" with text string
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/interaction-payloads/views">...</a>
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackBlockActionValue {

    /**
     * The type of action element (e.g., "radio_buttons", "datepicker", "plain_text_input")
     */
    @JsonProperty("type")
    private String type;

    /**
     * The selected option (for radio_buttons and static_select)
     * Contains the text object and value of the selected option
     */
    @JsonProperty("selected_option")
    private SlackSelectedOption selectedOption;

    /**
     * The selected date (for datepicker actions)
     * Format: YYYY-MM-DD (ISO 8601 date string)
     */
    @JsonProperty("selected_date")
    private String selectedDate;

    /**
     * The entered text value (for plain_text_input actions)
     */
    @JsonProperty("value")
    private String value;

    /**
     * Represents a selected option from radio_buttons or static_select
     */
    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackSelectedOption {

        /**
         * The text object for the option label
         */
        @JsonProperty("text")
        private SlackTextObject text;

        /**
         * The value of the selected option
         * This is what you define as the "value" when building the option
         */
        @JsonProperty("value")
        private String value;
    }

    /**
     * Represents a text object in Slack
     * Used for option labels, titles, etc.
     */
    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackTextObject {

        /**
         * The type of text (usually "plain_text")
         */
        @JsonProperty("type")
        private String type;

        /**
         * The actual text content
         */
        @JsonProperty("text")
        private String text;

        /**
         * Whether to use emoji in the text
         */
        @JsonProperty("emoji")
        private Boolean emoji;

        /**
         * Whether to truncate the text if it's too long
         */
        @JsonProperty("truncate")
        private Boolean truncate;
    }
}
