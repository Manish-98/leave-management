package one.june.leave_management.adapter.outbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request DTO for opening a Slack modal view using the views.open API
 * <p>
 * Slack API reference: https://api.slack.com/methods/views.open
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SlackViewOpenRequest {

    /**
     * The trigger ID that was received from the slash command
     * This is required to open a modal in response to a user interaction
     */
    @JsonProperty("trigger_id")
    private String triggerId;

    /**
     * The view payload to be displayed in the modal
     * Contains the structure of the modal form
     */
    @JsonProperty("view")
    private SlackModalView view;
}
