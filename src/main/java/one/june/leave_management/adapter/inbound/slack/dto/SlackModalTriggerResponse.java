package one.june.leave_management.adapter.inbound.slack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response DTO for triggering a Slack modal
 * This response tells Slack to open a modal using the provided trigger_id
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SlackModalTriggerResponse {
    private boolean triggerModal;
    private String triggerId;
    private String message;

    public static SlackModalTriggerResponse forModal(String triggerId) {
        return SlackModalTriggerResponse.builder()
                .triggerModal(true)
                .triggerId(triggerId)
                .build();
    }
}
