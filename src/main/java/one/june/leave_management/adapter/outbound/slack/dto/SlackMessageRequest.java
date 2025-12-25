package one.june.leave_management.adapter.outbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for posting messages to Slack using chat.postMessage API
 * <p>
 * Used for posting thread messages and updates to Slack channels.
 * Supports rich block-based formatting for professional messages.
 * <p>
 * Slack API reference: <a href="https://api.slack.com/methods/chat.postMessage">...</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlackMessageRequest {

    /**
     * The channel where the message will be posted
     * Can be a public channel, private group, or DM channel ID
     */
    @JsonProperty("channel")
    private String channel;

    /**
     * A collection of blocks that define the message content
     * Blocks provide rich formatting with sections, fields, dividers, etc.
     */
    @JsonProperty("blocks")
    private List<Object> blocks;

    /**
     * Optional fallback text for notifications, search, and screen readers
     * Required if blocks are provided
     */
    @JsonProperty("text")
    private String text;

    /**
     * Parent message timestamp to post this message in a thread
     * If provided, posts as a threaded reply to the parent message
     */
    @JsonProperty("thread_ts")
    private String threadTs;

    /**
     * Message type (deprecated but sometimes required for compatibility)
     */
    @JsonProperty("mrkdwn")
    private Boolean mrkdwn;
}
