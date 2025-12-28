package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Represents a Slack block_actions interaction request.
 * Sent when a user interacts with a block element (e.g., radio buttons, selects).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SlackBlockActionRequest {

    @JsonProperty("type")
    private String type; // "block_actions"

    @JsonProperty("team")
    private SlackTeam team;

    @JsonProperty("enterprise")
    private SlackEnterprise enterprise;

    @JsonProperty("is_enterprise_install")
    private Boolean isEnterpriseInstall;

    @JsonProperty("user")
    private SlackUser user;

    @JsonProperty("api_app_id")
    private String apiAppId;

    @JsonProperty("token")
    private String token;

    @JsonProperty("trigger_id")
    private String triggerId; // Used for opening new modals

    @JsonProperty("container")
    private SlackContainer container;

    @JsonProperty("actions")
    private List<SlackAction> actions;

    @JsonProperty("state")
    private SlackViewState state;

    @JsonProperty("view")
    private SlackView view;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SlackTeam {
        @JsonProperty("id")
        private String id;

        @JsonProperty("domain")
        private String domain;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SlackEnterprise {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String domain;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SlackContainer {
        @JsonProperty("type")
        private String type; // "view"

        @JsonProperty("view_id")
        private String viewId;

        @JsonProperty("view_hash")
        private String viewHash;
    }

    /**
     * Represents the view object in the block action
     * <p>
     * Contains the current view state including private_metadata
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
         * The state object containing all form values
         */
        @JsonProperty("state")
        private SlackViewState state;

        /**
         * Private metadata passed when opening the modal
         * Contains userId, channelId, channelName, threadTs
         */
        @JsonProperty("private_metadata")
        private String privateMetadata;

        /**
         * The title of the modal
         */
        @JsonProperty("title")
        private SlackTextObject title;

        /**
         * Developer-defined identifier for the view
         * Must be preserved across updates
         */
        @JsonProperty("external_id")
        private String externalId;

        /**
         * Hash of the current view state
         * Used for validation in views.update API to prevent race conditions
         */
        @JsonProperty("hash")
        private String hash;

        /**
         * Represents a text object in Slack
         */
        @Getter
        @Setter
        @Builder
        @ToString
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SlackTextObject {

            @JsonProperty("type")
            private String type;

            @JsonProperty("text")
            private String text;

            @JsonProperty("emoji")
            private Boolean emoji;
        }
    }
}
