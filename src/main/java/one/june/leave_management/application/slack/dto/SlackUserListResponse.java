package one.june.leave_management.application.slack.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response wrapper for Slack user list")
public class SlackUserListResponse {

    @Schema(description = "List of Slack users", required = true)
    private List<SlackUserDto> users;

    @Schema(description = "Total count of users", example = "25", required = true)
    private Integer totalCount;
}
