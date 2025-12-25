package one.june.leave_management.adapter.outbound.slack.builder;

import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating Slack messages
 * <p>
 * Provides a convenient, type-safe API for constructing Slack messages with blocks.
 * This builder follows the same pattern as SlackModalBuilder, providing a consistent
 * experience across the codebase.
 * <p>
 * Example usage:
 * <pre>
 * SlackMessageRequest message = SlackMessageBuilder.create("Fallback text")
 *     .withHeader("Title", true)
 *     .withSection("*Label:* value")
 *     .withDivider()
 *     .withFields(Map.of("Field1", "Value1", "Field2", "Value2"))
 *     .toChannel("C12345")
 *     .inThread("1234567890.123456")
 *     .build();
 * </pre>
 */
public class SlackMessageBuilder {

    private final String fallbackText;
    private final List<Object> blocks = new ArrayList<>();
    private String channelId;
    private String threadTs;

    private SlackMessageBuilder(String fallbackText) {
        this.fallbackText = fallbackText;
    }

    /**
     * Creates a new builder instance with the specified fallback text
     * <p>
     * The fallback text is used in notifications and clients that don't support blocks.
     *
     * @param fallbackText The plain text fallback for the message
     * @return A new SlackMessageBuilder instance
     */
    public static SlackMessageBuilder create(String fallbackText) {
        return new SlackMessageBuilder(fallbackText);
    }

    /**
     * Adds a header block to the message
     * <p>
     * Header blocks display text in a bold, larger format, ideal for titles.
     *
     * @param text  The header text content
     * @param emoji Whether to allow emoji in the text
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder withHeader(String text, boolean emoji) {
        blocks.add(SlackBlockBuilder.headerBlock(text, emoji));
        return this;
    }

    /**
     * Adds a section block with markdown text to the message
     * <p>
     * Section blocks are the most common block type for displaying text content.
     *
     * @param markdownText The markdown-formatted text content
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder withSection(String markdownText) {
        blocks.add(SlackBlockBuilder.sectionBlock(markdownText));
        return this;
    }

    /**
     * Adds a section block with label and value to the message
     * <p>
     * Convenience method for key-value pairs.
     *
     * @param label The field label (will be bold)
     * @param value The field value
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder withSection(String label, String value) {
        blocks.add(SlackBlockBuilder.sectionBlock(label, value));
        return this;
    }

    /**
     * Adds a section block with multiple fields to the message
     * <p>
     * Fields are displayed side-by-side in columns (up to 5 fields).
     *
     * @param fields Map of field labels to values
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder withFields(Map<String, String> fields) {
        blocks.add(SlackBlockBuilder.fieldsBlock(fields));
        return this;
    }

    /**
     * Adds a divider block to the message
     * <p>
     * Divider blocks display a visual separator line between sections.
     *
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder withDivider() {
        blocks.add(SlackBlockBuilder.dividerBlock());
        return this;
    }

    /**
     * Adds a context block to the message
     * <p>
     * Context blocks display auxiliary information in a smaller font,
     * typically used for metadata or status indicators.
     *
     * @param markdownText The markdown-formatted text content
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder withContext(String markdownText) {
        blocks.add(SlackBlockBuilder.contextBlock(markdownText));
        return this;
    }

    /**
     * Adds a custom block to the message
     * <p>
     * Allows adding pre-built blocks or custom block structures.
     *
     * @param block The block to add
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder withBlock(Map<String, Object> block) {
        blocks.add(block);
        return this;
    }

    /**
     * Sets the target channel for the message
     * <p>
     * This will override any channel set in the SlackMessageRequest.
     *
     * @param channelId The channel ID where the message will be posted
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder toChannel(String channelId) {
        this.channelId = channelId;
        return this;
    }

    /**
     * Sets the thread timestamp for posting as a threaded reply
     * <p>
     * When set, the message will be posted as a reply in the specified thread.
     *
     * @param threadTs The thread timestamp of the parent message
     * @return This builder instance for method chaining
     */
    public SlackMessageBuilder inThread(String threadTs) {
        this.threadTs = threadTs;
        return this;
    }

    /**
     * Builds the SlackMessageRequest from the configured builder
     * <p>
     * The returned message can be passed directly to SlackApiClient methods.
     *
     * @return A configured SlackMessageRequest instance
     */
    public SlackMessageRequest build() {
        return SlackMessageRequest.builder()
                .channel(channelId)
                .text(fallbackText)
                .blocks(blocks)
                .threadTs(threadTs)
                .build();
    }
}
