package one.june.leave_management.adapter.outbound.slack.client;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.outbound.slack.dto.SlackErrorMessageRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageResponse;
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

    /**
     * Posts an error message to Slack using the response_url.
     * <p>
     * This is used to send error messages to users when processing fails.
     * The message appears as an ephemeral message visible only to the user.
     * <p>
     * Slack API reference: <a href="https://api.slack.com/methods/chat.postMessage">...</a>
     *
     * @param responseUrl The response_url from the Slack payload
     * @param errorMessage The error message to display to the user
     */
    public void postErrorMessage(String responseUrl, String errorMessage) {
        log.info("Posting error message to Slack: {}", errorMessage);

        // Validate inputs
        if (responseUrl == null || responseUrl.trim().isEmpty()) {
            log.error("Response URL is null or empty, cannot send error message: {}", errorMessage);
            return;
        }

        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            log.warn("Error message is null or empty, nothing to send");
            return;
        }

        try {
            // Build the message payload
            SlackErrorMessageRequest messageRequest = SlackErrorMessageRequest.builder()
                    .text(errorMessage)
                    .responseType("ephemeral")
                    .build();

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the HTTP entity
            HttpEntity<SlackErrorMessageRequest> entity = new HttpEntity<>(messageRequest, headers);

            log.debug("Sending error message to Slack via response_url...");

            // Make the API call
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    responseUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.debug("Error message sent. Status: {}", responseEntity.getStatusCode());

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                log.warn("Slack returned non-2xx status when sending error message: {}",
                        responseEntity.getStatusCode());
            }

        } catch (RestClientException e) {
            // Log but don't throw - we don't want to fail the request if error notification fails
            log.error("HTTP error posting error message to Slack. Type: {}, Message: {}",
                    e.getClass().getName(), e.getMessage());
        } catch (Exception e) {
            // Log but don't throw
            log.error("Unexpected error posting error message to Slack. Type: {}, Message: {}",
                    e.getClass().getName(), e.getMessage());
        }
    }

    /**
     * Posts a message to a Slack channel using the chat.postMessage API
     * <p>
     * This method is used to create thread anchor messages or post updates.
     * Returns the message timestamp (ts) which can be used as thread_ts for replies.
     * <p>
     * Slack API reference: <a href="https://api.slack.com/methods/chat.postMessage">...</a>
     *
     * @param channelId The channel ID where the message should be posted
     * @param message   The message request containing blocks and other content
     * @return The response from Slack API containing the message timestamp
     * @throws RuntimeException if the API call fails
     */
    public SlackMessageResponse postMessage(String channelId, SlackMessageRequest message) {
        log.info("Posting message to channel: {}", channelId);

        String botToken = slackProperties.getBotToken();

        // Validate inputs
        if (botToken == null || botToken.trim().isEmpty()) {
            log.error("Slack bot token is null or empty. Please configure slack.bot-token property.");
            throw new RuntimeException("Slack bot token is not configured");
        }

        if (channelId == null || channelId.trim().isEmpty()) {
            log.error("Channel ID is null or empty");
            throw new RuntimeException("Channel ID cannot be null or empty");
        }

        if (message == null) {
            log.error("Message request is null");
            throw new RuntimeException("Message request cannot be null");
        }

        // Ensure channel is set in the message
        message.setChannel(channelId);

        String fullApiUrl = slackProperties.getApiBaseUrl() + "/chat.postMessage";

        try {
            // Set up headers with authorization and content type
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/json; charset=UTF-8"));
            headers.setBearerAuth(botToken);

            // Create the HTTP entity with headers and body
            HttpEntity<SlackMessageRequest> entity = new HttpEntity<>(message, headers);

            log.debug("Sending message to Slack API...");

            // Make the API call
            ResponseEntity<SlackMessageResponse> responseEntity = restTemplate.exchange(
                    fullApiUrl,
                    HttpMethod.POST,
                    entity,
                    SlackMessageResponse.class
            );

            log.debug("Received response from Slack API. Status: {}", responseEntity.getStatusCode());

            SlackMessageResponse response = responseEntity.getBody();

            if (response == null) {
                log.error("Received null response body from Slack chat.postMessage API");
                throw new RuntimeException("Null response from Slack API");
            }

            if (!response.isOk()) {
                log.error("Slack chat.postMessage API returned error: {}. Full response: {}",
                        response.getError(), response);
                throw new RuntimeException("Slack API error: " + response.getError());
            }

            log.info("Successfully posted message. Message timestamp (ts): {}", response.getTs());
            return response;

        } catch (RestClientException e) {
            log.error("HTTP error calling Slack chat.postMessage API. Type: {}, Message: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("HTTP error calling Slack API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling Slack chat.postMessage API. Type: {}, Message: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Unexpected error calling Slack API: " + e.getMessage(), e);
        }
    }

    /**
     * Posts a threaded reply to an existing message in a Slack channel
     * <p>
     * This method is used to post success/failure messages as threaded replies
     * to the original thread anchor message.
     * <p>
     * Slack API reference: <a href="https://api.slack.com/methods/chat.postMessage">...</a>
     *
     * @param channelId The channel ID where the thread exists
     * @param threadTs  The thread timestamp (ts) of the parent message
     * @param message   The message request containing blocks and other content
     * @return The response from Slack API
     * @throws RuntimeException if the API call fails
     */
    public SlackMessageResponse postThreadReply(String channelId, String threadTs, SlackMessageRequest message) {
        log.info("Posting thread reply to channel: {}, thread_ts: {}", channelId, threadTs);

        // Validate thread_ts
        if (threadTs == null || threadTs.trim().isEmpty()) {
            log.error("Thread timestamp (threadTs) is null or empty");
            throw new RuntimeException("Thread timestamp cannot be null or empty");
        }

        // Set the thread_ts in the message
        message.setThreadTs(threadTs);

        // Reuse postMessage logic
        return postMessage(channelId, message);
    }
}
