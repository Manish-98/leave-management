package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Represents the state object in a Slack view submission
 * <p>
 * The state contains all the values entered by the user in the modal.
 * Structure: state.values is a map where:
 * - Key: block_id (string)
 * - Value: map of action_id to SlackBlockActionValue
 * <p>
 * Example:
 * <pre>
 * {
 *   "values": {
 *     "leave_type_category_block": {
 *       "leave_type_category_action": {
 *         "type": "radio_buttons",
 *         "selected_option": {
 *           "text": {"type": "plain_text", "text": "Annual Leave"},
 *           "value": "ANNUAL_LEAVE"
 *         }
 *       }
 *     },
 *     "start_date_block": {
 *       "start_date_action": {
 *         "type": "datepicker",
 *         "selected_date": "2024-01-15"
 *       }
 *     }
 *   }
 * }
 * </pre>
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
public class SlackViewState {

    /**
     * Map of block_id to map of action_id to action value
     * This structure allows for multiple actions within a single block,
     * though most blocks have only one action.
     */
    @JsonProperty("values")
    @Builder.Default
    private Map<String, Map<String, SlackBlockActionValue>> values = Map.of();
}
