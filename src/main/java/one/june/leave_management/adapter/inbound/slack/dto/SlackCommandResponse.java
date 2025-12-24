package one.june.leave_management.adapter.inbound.slack.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SlackCommandResponse {
    private String responseType;
    private String text;
}
