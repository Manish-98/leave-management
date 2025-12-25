package one.june.leave_management.adapter.outbound.slack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending error messages to Slack via response_url
 * Used when view submission processing fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackErrorMessageRequest {

    /**
     * The error message text to display to the user
     */
    private String text;

    /**
     * Response type - "ephemeral" means only the user sees the message
     */
    private String responseType;
}
