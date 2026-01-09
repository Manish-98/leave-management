package one.june.leave_management.application.slack.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data Transfer Object for Slack user information")
public class SlackUserDto {

    @Schema(description = "Slack user ID", example = "U12345678", required = true)
    private String slackId;

    @Schema(description = "Full name of the user", example = "John Doe", required = true)
    private String name;

    @Schema(description = "Display name in Slack", example = "john.doe")
    private String displayName;

    @Schema(description = "Email address of the user", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Slack team ID", example = "T12345678")
    private String teamId;

    @Schema(description = "Whether the user is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Whether the user is a bot", example = "false")
    private Boolean isBot;

    @Schema(description = "Whether the user has been deleted", example = "false")
    private Boolean deleted;
}
