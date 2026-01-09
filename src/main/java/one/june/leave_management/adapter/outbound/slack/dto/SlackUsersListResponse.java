package one.june.leave_management.adapter.outbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Response DTO from Slack users.list API call
 * <p>
 * Slack API reference: https://api.slack.com/methods/users.list
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SlackUsersListResponse {

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
     * List of users in the workspace
     */
    @JsonProperty("members")
    private List<SlackUser> members;

    /**
     * Metadata for pagination
     */
    @JsonProperty("response_metadata")
    private ResponseMetadata responseMetadata;

    /**
     * Nested class for user information from Slack
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackUser {
        /**
         * User's unique ID
         */
        @JsonProperty("id")
        private String id;

        /**
         * Team ID
         */
        @JsonProperty("team_id")
        private String teamId;

        /**
         * Username
         */
        @JsonProperty("name")
        private String name;

        /**
         * Whether the user has been deleted
         */
        @JsonProperty("deleted")
        private boolean deleted;

        /**
         * Whether this is a bot user
         */
        @JsonProperty("is_bot")
        private boolean isBot;

        /**
         * Whether this is an app user
         */
        @JsonProperty("is_app_user")
        private boolean isAppUser;

        /**
         * User's profile information
         */
        @JsonProperty("profile")
        private Profile profile;

        /**
         * User's presence (active, away)
         */
        @JsonProperty("presence")
        private String presence;
    }

    /**
     * Nested class for user profile information
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        /**
         * User's real name
         */
        @JsonProperty("real_name")
        private String realName;

        /**
         * Display name
         */
        @JsonProperty("display_name")
        private String displayName;

        /**
         * Email address
         */
        @JsonProperty("email")
        private String email;

        /**
         * Title/position
         */
        @JsonProperty("title")
        private String title;

        /**
         * Phone number
         */
        @JsonProperty("phone")
        private String phone;

        /**
         * Skype username
         */
        @JsonProperty("skype")
        private String skype;
    }

    /**
     * Nested class for response metadata (pagination)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ResponseMetadata {
        /**
         * Next cursor for pagination
         */
        @JsonProperty("next_cursor")
        private String nextCursor;
    }
}
