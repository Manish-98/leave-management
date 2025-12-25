package one.june.leave_management.adapter.outbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from Slack chat.postMessage API
 * <p>
 * Contains the message timestamp (ts) which is used as thread_ts
 * for posting threaded replies.
 * <p>
 * Slack API reference: <a href="https://api.slack.com/methods/chat.postMessage">...</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackMessageResponse {

    /**
     * Indicates if the API call was successful
     */
    @JsonProperty("ok")
    private boolean ok;

    /**
     * Error message if ok is false
     */
    @JsonProperty("error")
    private String error;

    /**
     * The channel where the message was posted
     */
    @JsonProperty("channel")
    private String channel;

    /**
     * The message timestamp (used as thread_ts for threaded replies)
     * This is the critical field for creating threaded conversations
     */
    @JsonProperty("ts")
    private String ts;

    /**
     * The message object that was posted
     */
    @JsonProperty("message")
    private SlackMessage message;

    /**
     * Nested message object
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackMessage {

        @JsonProperty("ts")
        private String ts;

        @JsonProperty("text")
        private String text;
    }
}
