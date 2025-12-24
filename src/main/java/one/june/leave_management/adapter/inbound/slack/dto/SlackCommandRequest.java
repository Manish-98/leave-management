package one.june.leave_management.adapter.inbound.slack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request DTO representing a Slack slash command payload
 * Slack sends form data with these fields when a slash command is invoked
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SlackCommandRequest {
    private String command;
    private String text;
    private String triggerId;
    private String userId;
    private String userName;
    private String channelId;
    private String channelName;
    private String teamId;
    private String teamDomain;
    private String responseUrl;
    private String apiAppId;
}
