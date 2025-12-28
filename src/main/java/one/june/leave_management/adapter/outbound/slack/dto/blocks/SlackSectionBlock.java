package one.june.leave_management.adapter.outbound.slack.dto.blocks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;

import java.util.List;

/**
 * Section block for Slack modals and messages
 * <p>
 * Section blocks are the most common block type, used to display
 * text content with markdown formatting and can include interactive elements.
 * <p>
 * Unlike input blocks, section blocks with accessories support dispatch_action
 * in modals, enabling dynamic modal updates based on user interactions.
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/block-kit/blocks#section">...</a>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "block_id", "text", "fields", "accessory"})
public class SlackSectionBlock {

    /**
     * The type of block - always "section"
     * Using @Builder.Default to ensure proper initialization with Lombok builder
     */
    @Builder.Default
    @JsonProperty("type")
    private String type = "section";

    /**
     * A unique identifier for this block
     * Maximum 255 characters
     */
    @JsonProperty("block_id")
    private String blockId;

    /**
     * The text content for this block (markdown formatted)
     * This is the primary content displayed in the block
     */
    @JsonProperty("text")
    private SlackText text;

    /**
     * An array of text objects
     * When specified, fields are displayed in a compact horizontal arrangement
     * Maximum 5 items
     */
    @JsonProperty("fields")
    private List<SlackText> fields;

    /**
     * An interactive element (radio buttons, select menu, button, etc.)
     * When specified, the element is displayed as an accessory to the block content
     * <p>
     * IMPORTANT: Elements in section accessories CAN use dispatch_action in modals!
     */
    @JsonProperty("accessory")
    private Object accessory;
}
