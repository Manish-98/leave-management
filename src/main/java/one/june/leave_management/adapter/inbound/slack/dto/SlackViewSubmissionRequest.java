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
 * Represents a view_submission payload from Slack
 * <p>
 * When a user submits a modal in Slack, Slack sends a POST request to your endpoint
 * with this payload containing all the form data. The payload includes:
 * - Team and user information
 * - The view object with all form values
 * - API app ID and verification token
 * <p>
 * This endpoint needs to:
 * 1. Verify the request signature (same as slash commands)
 * 2. Parse the form values from view.state.values
 * 3. Process the data (create leave in this case)
 * 4. Return an empty response body (Slack closes the modal automatically)
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
public class SlackViewSubmissionRequest {

    /**
     * The type of payload (always "view_submission" for modal submissions)
     */
    @JsonProperty("type")
    private String type;

    /**
     * Information about the team where the interaction occurred
     */
    @JsonProperty("team")
    private SlackTeam team;

    /**
     * Information about the user who triggered the interaction
     */
    @JsonProperty("user")
    private SlackUser user;

    /**
     * The API app ID (identifies your Slack app)
     */
    @JsonProperty("api_app_id")
    private String apiAppId;

    /**
     * The verification token (if verification is enabled)
     * Note: Signature verification is preferred over token verification
     */
    @JsonProperty("token")
    private String token;

    /**
     * The trigger ID from the interaction
     * Can be used to open another modal or display a message
     */
    @JsonProperty("trigger_id")
    private String triggerId;

    /**
     * The view object containing all form data
     * This is the most important field as it contains:
     * - view.id: unique view ID (use as sourceId)
     * - view.state.values: map of form field values
     * - view.private_metadata: metadata we passed when opening the modal
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
     * Represents the view object in the submission
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
         * Use this as the sourceId when creating leaves
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
         * For this implementation, it should be "leave_application_submit"
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
         * We're using this to store the userId
         */
        @JsonProperty("private_metadata")
        private String privateMetadata;

        /**
         * The title of the modal
         */
        @JsonProperty("title")
        private SlackTextObject title;

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
