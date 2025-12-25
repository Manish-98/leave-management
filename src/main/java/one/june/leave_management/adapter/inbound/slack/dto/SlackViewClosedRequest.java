package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a view_closed payload from Slack
 * <p>
 * When a user closes a modal without submitting (clicks X or Cancel button),
 * Slack sends a POST request with this payload.
 * We use this to update the thread with a cancellation message.
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/interaction-payloads/views">...</a>
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackViewClosedRequest {

    /**
     * The type of payload (always "view_closed" for modal cancellations)
     */
    @JsonProperty("type")
    private String type;

    /**
     * Information about the team where the interaction occurred
     */
    @JsonProperty("team")
    private SlackTeam team;

    /**
     * Information about the user who closed the modal
     */
    @JsonProperty("user")
    private SlackUser user;

    /**
     * The API app ID (identifies your Slack app)
     */
    @JsonProperty("api_app_id")
    private String apiAppId;

    /**
     * The view object containing metadata we stored
     */
    @JsonProperty("view")
    private SlackView view;

    /**
     * Represents team information
     */
    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackTeam {

        @JsonProperty("id")
        private String id;

        @JsonProperty("domain")
        private String domain;
    }

    /**
     * Represents user information
     */
    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackUser {

        @JsonProperty("id")
        private String id;

        @JsonProperty("username")
        private String username;

        @JsonProperty("name")
        private String name;

        @JsonProperty("team_id")
        private String teamId;
    }

    /**
     * Represents the view object in the closed request
     */
    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackView {

        /**
         * Unique identifier for this view
         */
        @JsonProperty("id")
        private String id;

        /**
         * The team ID that owns this view
         */
        @JsonProperty("team_id")
        private String teamId;

        /**
         * The type of view (usually "modal")
         */
        @JsonProperty("type")
        private String type;

        /**
         * The callback ID specified when opening the modal
         */
        @JsonProperty("callback_id")
        private String callbackId;

        /**
         * Private metadata passed when opening the modal
         * Contains our JSON with userId, channelId, threadTs, channelName
         */
        @JsonProperty("private_metadata")
        private String privateMetadata;
    }
}
