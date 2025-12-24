package one.june.leave_management.adapter.outbound.slack.client;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.SlackViewOpenRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackViewOpenResponse;
import one.june.leave_management.config.SlackProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Outbound adapter for communicating with Slack API
 * Handles HTTP calls to Slack endpoints for modal operations
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "slack.enabled", havingValue = "true", matchIfMissing = true)
public class SlackApiClient {

    private final RestTemplate restTemplate;
    private final SlackProperties slackProperties;

    /**
     * Creates a new SlackApiClient
     *
     * @param restTemplate The configured RestTemplate for making HTTP requests
     * @param slackProperties The Slack configuration properties
     */
    public SlackApiClient(RestTemplate restTemplate, SlackProperties slackProperties) {
        this.restTemplate = restTemplate;
        this.slackProperties = slackProperties;
    }

    /**
     * Opens a modal view in Slack using the views.open API
     * <p>
     * This method calls the Slack API to display a modal to a user.
     * It uses the trigger_id from a slash command to open the modal.
     * <p>
     * Slack API reference: <a href="https://api.slack.com/methods/views.open">...</a>
     *
     * @param triggerId The trigger ID received from the slash command
     * @param view      The modal view to display
     * @return The response from Slack API
     * @throws RuntimeException if the API call fails
     */
    public SlackViewOpenResponse openModal(String triggerId, SlackModalView view) {
        log.info("Opening modal in Slack with trigger_id: {}", triggerId);

        String botToken = slackProperties.getBotToken();

        // Validate inputs
        if (botToken == null || botToken.trim().isEmpty()) {
            log.error("Slack bot token is null or empty. Please configure slack.bot-token property.");
            throw new RuntimeException("Slack bot token is not configured");
        }

        if (triggerId == null || triggerId.trim().isEmpty()) {
            log.error("Trigger ID is null or empty");
            throw new RuntimeException("Trigger ID cannot be null or empty");
        }

        SlackViewOpenRequest request = SlackViewOpenRequest.builder()
                .triggerId(triggerId)
                .view(view)
                .build();

        String fullApiUrl = slackProperties.getApiBaseUrl() + slackProperties.getViewsOpenEndpoint();

        try {
            // Set up headers with authorization and content type
            // Must include charset explicitly to avoid "missing_charset" warning
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/json; charset=UTF-8"));
            headers.setBearerAuth(botToken);

            // Create the HTTP entity with headers and body
            HttpEntity<SlackViewOpenRequest> entity = new HttpEntity<>(request, headers);

            log.debug("Sending request to Slack API...");

            // Make the API call
            ResponseEntity<SlackViewOpenResponse> responseEntity = restTemplate.exchange(
                    fullApiUrl,
                    HttpMethod.POST,
                    entity,
                    SlackViewOpenResponse.class
            );

            log.debug("Received response from Slack API. Status: {}", responseEntity.getStatusCode());

            SlackViewOpenResponse response = responseEntity.getBody();

            if (response == null) {
                log.error("Received null response body from Slack views.open API");
                throw new RuntimeException("Null response from Slack API");
            }

            if (!response.isOk()) {
                log.error("Slack views.open API returned error: {}. Full response: {}",
                        response.getError(), response);
                throw new RuntimeException("Slack API error: " + response.getError());
            }

            log.info("Successfully opened modal. View ID: {}", response.getView() != null ? response.getView().getId() : "N/A");
            return response;

        } catch (RestClientException e) {
            log.error("HTTP error calling Slack views.open API. Type: {}, Message: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("HTTP error calling Slack API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling Slack views.open API. Type: {}, Message: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Unexpected error calling Slack API: " + e.getMessage(), e);
        }
    }
}
