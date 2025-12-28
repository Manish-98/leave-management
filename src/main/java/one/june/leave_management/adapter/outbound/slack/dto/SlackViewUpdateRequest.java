package one.june.leave_management.adapter.outbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.SlackInputBlock;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Request for updating a Slack modal view.
 * Used with the views.update API endpoint.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SlackViewUpdateRequest {

    @JsonProperty("view_id")
    private String viewId; // The ID of the view to update

    @JsonProperty("view")
    private SlackModalViewUpdate view; // The updated view definition

    @JsonProperty("hash")
    private String hash; // Hash from the interaction to prevent race conditions

    /**
     * Nested class for the view object in update requests.
     * Contains fields that are valid for views.update API.
     * Note: external_id is only valid for views.open and cannot be changed after creation.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SlackModalViewUpdate {
        @JsonProperty("type")
        private String type; // "modal"

        @JsonProperty("title")
        private SlackText title; // Plain text (max 24 chars)

        @JsonProperty("blocks")
        private List<Object> blocks; // List of block objects

        @JsonProperty("private_metadata")
        private String privateMetadata; // Metadata preserved across updates

        @JsonProperty("callback_id")
        private String callbackId; // Identifier for the view

        @JsonProperty("submit")
        private SlackText submit; // Submit button text

        @JsonProperty("close")
        private SlackText close; // Close button text

        @JsonProperty("notify_on_close")
        private Boolean notifyOnClose; // Whether to notify on close

        @JsonProperty("clear_on_close")
        private Boolean clearOnClose; // Whether to clear on close
    }
}
