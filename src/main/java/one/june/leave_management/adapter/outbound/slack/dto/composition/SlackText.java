package one.june.leave_management.adapter.outbound.slack.dto.composition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Text object for Slack Block Kit
 * <p>
 * Supports both plain_text and mrkdwn text types
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/messaging/composition-objects#text">...</a>
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "text", "emoji", "verbatim"})
public class SlackText {

    /**
     * The type of text - either "plain_text" or "mrkdwn"
     */
    @JsonProperty("type")
    private final String type;

    /**
     * The actual text content
     */
    @JsonProperty("text")
    private String text;

    /**
     * Whether to use emoji in the text
     * Only applies to plain_text type
     */
    @JsonProperty("emoji")
    private Boolean emoji;

    /**
     * Whether to use verbatim mode (no markdown parsing)
     * Only applies to mrkdwn type
     */
    @JsonProperty("verbatim")
    private Boolean verbatim;

    /**
     * Creates a plain_text object without emoji
     *
     * @param text The text content
     * @return A new SlackText instance
     */
    public static SlackText plainText(String text) {
        return SlackText.builder()
                .type("plain_text")
                .text(text)
                .build();
    }

    /**
     * Creates a plain_text object with emoji enabled
     *
     * @param text The text content
     * @param emoji Whether to enable emoji
     * @return A new SlackText instance
     */
    public static SlackText plainText(String text, boolean emoji) {
        return SlackText.builder()
                .type("plain_text")
                .text(text)
                .emoji(emoji)
                .build();
    }

    /**
     * Creates a mrkdwn text object
     *
     * @param text The markdown text content
     * @return A new SlackText instance
     */
    public static SlackText markdown(String text) {
        return SlackText.builder()
                .type("mrkdwn")
                .text(text)
                .build();
    }
}
