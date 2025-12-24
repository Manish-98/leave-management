package one.june.leave_management.adapter.outbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response DTO from Slack views.open API call
 * <p>
 * Slack API reference: https://api.slack.com/methods/views.open
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SlackViewOpenResponse {

    /**
     * Indicates if the API call was successful
     */
    @JsonProperty("ok")
    private boolean ok;

    /**
     * Error message if the call was not successful
     */
    @JsonProperty("error")
    private String error;

    /**
     * The view that was opened
     * Contains the external ID and view ID
     */
    @JsonProperty("view")
    private SlackViewResponse view;

    /**
     * Response warnings if any
     */
    @JsonProperty("response_metadata")
    private ResponseMetadata responseMetadata;

    /**
     * Nested class for view information in the response
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackViewResponse {
        /**
         * The ID of the opened view
         */
        @JsonProperty("id")
        private String id;

        /**
         * The team ID
         */
        @JsonProperty("team_id")
        private String teamId;

        /**
         * The external ID of the view
         */
        @JsonProperty("external_id")
        private String externalId;

        /**
         * The bot ID
         */
        @JsonProperty("bot_id")
        private String botId;
    }

    /**
     * Nested class for response metadata
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ResponseMetadata {
        /**
         * Warnings from the API
         */
        @JsonProperty("warnings")
        private String[] warnings;
    }
}
