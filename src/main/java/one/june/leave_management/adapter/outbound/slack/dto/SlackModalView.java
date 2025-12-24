package one.june.leave_management.adapter.outbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;

import java.util.List;

/**
 * DTO representing a Slack modal view
 * <p>
 * Slack API reference: <a href="https://api.slack.com/reference/surfaces/views">...</a>
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlackModalView {

    /**
     * The type of view - must be "modal" for modals
     */
    @JsonProperty("type")
    private String type;

    /**
     * The title displayed at the top of the modal
     */
    @JsonProperty("title")
    private SlackText title;

    /**
     * A collection of blocks that define the modal content
     */
    @JsonProperty("blocks")
    private List<Object> blocks;

    /**
     * An optional identifier for this view
     * Can be used to update the view later
     */
    @JsonProperty("external_id")
    private String externalId;

    /**
     * The label for the submit button
     */
    @JsonProperty("submit")
    private SlackText submit;

    /**
     * A callback ID to identify this modal when submitted
     */
    @JsonProperty("callback_id")
    private String callbackId;

    /**
     * Optional close button text
     */
    @JsonProperty("close")
    private SlackText close;

    /**
     * Indicates if the modal should be cleared when closed
     */
    @JsonProperty("clear_on_close")
    private Boolean clearOnClose;

    /**
     * Indicates if the modal should be submitted when closed outside
     * Note: Only include this field if set to true, don't send null
     */
    @JsonProperty("submit_on_close")
    private Boolean submitOnClose;

    /**
     * Private metadata for your app
     */
    @JsonProperty("private_metadata")
    private String privateMetadata;
}
