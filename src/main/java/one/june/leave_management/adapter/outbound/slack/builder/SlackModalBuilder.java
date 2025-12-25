package one.june.leave_management.adapter.outbound.slack.builder;

import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating Slack modals
 * <p>
 * Simplifies modal construction with sensible defaults and a clean API
 * <p>
 * Example usage:
 * <pre>
 * SlackModalView modal = SlackModalBuilder.create("Apply for Leave", "leave_application_submit")
 *     .withBlocks(blocks)
 *     .withPrivateMetadata(userId)
 *     .build();
 * </pre>
 */
public class SlackModalBuilder {

    private final String title;
    private final String callbackId;
    private final List<Object> blocks = new ArrayList<>();
    private String submitText = "Submit";
    private String closeText = "Cancel";
    private String privateMetadata;
    private boolean clearOnClose = true;

    /**
     * Private constructor - use create() factory method
     *
     * @param title     The modal title
     * @param callbackId The callback identifier for modal submission
     */
    private SlackModalBuilder(String title, String callbackId) {
        this.title = title;
        this.callbackId = callbackId;
    }

    /**
     * Creates a new modal builder with the given title and callback ID
     *
     * @param title     The title to display at the top of the modal
     * @param callbackId The callback ID to identify this modal when submitted
     * @return A new SlackModalBuilder instance
     */
    public static SlackModalBuilder create(String title, String callbackId) {
        return new SlackModalBuilder(title, callbackId);
    }

    /**
     * Adds a list of blocks to the modal
     *
     * @param blocks The list of blocks to add
     * @return This builder instance for method chaining
     */
    public SlackModalBuilder withBlocks(List<Object> blocks) {
        this.blocks.addAll(blocks);
        return this;
    }

    /**
     * Adds a single block to the modal
     *
     * @param block The block to add
     * @return This builder instance for method chaining
     */
    public SlackModalBuilder withBlock(Object block) {
        this.blocks.add(block);
        return this;
    }

    /**
     * Sets the text for the submit button
     *
     * @param text The submit button text
     * @return This builder instance for method chaining
     */
    public SlackModalBuilder withSubmitText(String text) {
        this.submitText = text;
        return this;
    }

    /**
     * Sets the text for the close button
     *
     * @param text The close button text
     * @return This builder instance for method chaining
     */
    public SlackModalBuilder withCloseText(String text) {
        this.closeText = text;
        return this;
    }

    /**
     * Sets private metadata for the modal
     * <p>
     * This metadata is sent back when the modal is submitted
     * Useful for storing context like user ID
     *
     * @param metadata The metadata string
     * @return This builder instance for method chaining
     */
    public SlackModalBuilder withPrivateMetadata(String metadata) {
        this.privateMetadata = metadata;
        return this;
    }

    /**
     * Sets whether the modal should clear when closed
     *
     * @param clearOnClose true to clear, false to keep
     * @return This builder instance for method chaining
     */
    public SlackModalBuilder withClearOnClose(boolean clearOnClose) {
        this.clearOnClose = clearOnClose;
        return this;
    }

    /**
     * Builds and returns the SlackModalView
     *
     * @return A configured SlackModalView ready to be sent to Slack API
     */
    public SlackModalView build() {
        return SlackModalView.builder()
                .type("modal")
                .title(SlackText.plainText(title, true))
                .notifyOnClose(true)
                .blocks(blocks)
                .submit(SlackText.plainText(submitText, true))
                .close(SlackText.plainText(closeText, true))
                .callbackId(callbackId)
                .privateMetadata(privateMetadata)
                .clearOnClose(clearOnClose)
                .externalId(generateExternalId())
                .build();
    }

    /**
     * Generates a unique external ID for this modal
     *
     * @return A unique external ID
     */
    private String generateExternalId() {
        return callbackId + "_" + System.currentTimeMillis();
    }
}
