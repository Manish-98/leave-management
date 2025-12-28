package one.june.leave_management.adapter.outbound.slack.dto.blocks.elements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;

import java.util.List;

/**
 * Static select menu element for Slack modals.
 * Displays a dropdown with predefined options.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "action_id", "options", "initial_option", "placeholder", "dispatch_actions"})
public class SlackStaticSelectElement {

    @JsonProperty("type")
    private final String type = "static_select";

    @JsonProperty("action_id")
    private String actionId; // Required - Identifier for the select element

    @JsonProperty("options")
    private List<SlackOption> options; // Required - List of available options (max 100)

    @JsonProperty("initial_option")
    private SlackOption initialOption; // Optional - Initially selected option

    @JsonProperty("placeholder")
    private SlackText placeholder; // Optional - Placeholder text when no option is selected

    @JsonProperty("dispatch_actions")
    private Boolean dispatchActions; // Optional - Enables block_actions on change

    /**
     * Confirmation dialog (optional).
     * Not included in JSON when null.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("confirm")
    private SlackConfirmationDialog confirm;

    /**
     * Nested class for confirmation dialog (optional).
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SlackConfirmationDialog {
        @JsonProperty("title")
        private SlackText title;

        @JsonProperty("text")
        private SlackText text;

        @JsonProperty("confirm")
        private String confirm; // "Confirm" button text

        @JsonProperty("deny")
        private String deny; // "Cancel" button text

        @JsonProperty("style")
        private String style; // "primary" or "danger"
    }
}
