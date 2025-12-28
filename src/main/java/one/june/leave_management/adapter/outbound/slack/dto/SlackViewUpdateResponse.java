package one.june.leave_management.adapter.outbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewState;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;

import java.util.List;

/**
 * Response from Slack views.update API call.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SlackViewUpdateResponse {

    @JsonProperty("ok")
    private Boolean ok;

    @JsonProperty("error")
    private String error;

    @JsonProperty("view")
    private SlackModalViewResponse view;

    @JsonProperty("response_url")
    private String responseUrl;

    /**
     * Nested class for the view returned in the response.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SlackModalViewResponse {
        @JsonProperty("id")
        private String id;

        @JsonProperty("team_id")
        private String teamId;

        @JsonProperty("root_view_id")
        private String rootViewId;

        @JsonProperty("app_id")
        private String appId;

        @JsonProperty("state")
        private SlackViewState state;

        @JsonProperty("title")
        private SlackText title;

        @JsonProperty("blocks")
        private List<Object> blocks; // Simplified for response

        @JsonProperty("private_metadata")
        private String privateMetadata;

        @JsonProperty("callback_id")
        private String callbackId;

        @JsonProperty("external_id")
        private String externalId;
    }
}
