package one.june.leave_management.adapter.genai.client;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.genai.dto.GroqChatRequest;
import one.june.leave_management.adapter.genai.dto.GroqChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Client for interacting with Groq API.
 * Uses RestTemplate for synchronous HTTP calls with retry logic.
 */
@Slf4j
@Component
public class GroqApiClient {

    private final RestTemplate restTemplate;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String model;

    @Value("${groq.api.timeout:30}")
    private int timeoutSeconds;

    @Value("${groq.api.max-retries:2}")
    private int maxRetries;

    public GroqApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Send a chat completion request to Groq API with retry logic.
     *
     * @param systemMessage System prompt defining the AI's behavior
     * @param userMessage User's input message
     * @return Response containing the AI's reply
     * @throws RuntimeException if the API call fails after retries
     */
    public GroqChatResponse chat(String systemMessage, String userMessage) {
        log.debug("Sending chat request to Groq API with model: {}", model);

        GroqChatRequest request = GroqChatRequest.builder()
                .model(model)
                .messages(List.of(
                        GroqChatRequest.Message.builder()
                                .role("system")
                                .content(systemMessage)
                                .build(),
                        GroqChatRequest.Message.builder()
                                .role("user")
                                .content(userMessage)
                                .build()
                ))
                .temperature(0.3) // Lower temperature for more deterministic parsing
                .maxTokens(1024)
                .responseFormat(GroqChatRequest.ResponseFormat.builder()
                        .type("json_object")
                        .build())
                .build();

        return sendRequestWithRetry(request);
    }

    /**
     * Send a simple chat completion request with only a user message.
     *
     * @param userMessage User's input message
     * @return Response containing the AI's reply
     */
    public GroqChatResponse chat(String userMessage) {
        return chat("You are a helpful assistant that parses leave requests.", userMessage);
    }

    /**
     * Send request to Groq API with retry logic.
     *
     * @param request The chat request to send
     * @return The response from Groq API
     * @throws RuntimeException if all retry attempts fail
     */
    private GroqChatResponse sendRequestWithRetry(GroqChatRequest request) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= maxRetries) {
            try {
                log.debug("Attempt {} of {}", attempts + 1, maxRetries + 1);

                // Set up headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                // Create HTTP entity
                HttpEntity<GroqChatRequest> entity = new HttpEntity<>(request, headers);

                // Make the API call
                ResponseEntity<GroqChatResponse> response = restTemplate.exchange(
                        groqApiUrl,
                        HttpMethod.POST,
                        entity,
                        GroqChatResponse.class
                );

                GroqChatResponse responseBody = response.getBody();

                // Log the raw response for debugging
                log.info("Received successful response from Groq API");
                log.debug("Response ID: {}, Model: {}",
                        responseBody != null ? responseBody.getId() : "null",
                        responseBody != null ? responseBody.getModel() : "null");
                if (responseBody != null && responseBody.getChoices() != null && !responseBody.getChoices().isEmpty()) {
                    log.debug("Response content: {}",
                            responseBody.getChoices().get(0).getMessage().getContent());
                }

                return responseBody;

            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {} failed: {}", attempts + 1, e.getMessage());

                // Check if we should retry (rate limit or server error)
                if (shouldRetry(e) && attempts < maxRetries) {
                    attempts++;
                    try {
                        // Wait before retry with exponential backoff
                        Thread.sleep(1000L * (1 << attempts)); // 2s, 4s, 8s...
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry delay", ie);
                    }
                    continue;
                } else {
                    break;
                }
            }
        }

        log.error("Error calling Groq API after {} attempts", attempts + 1);
        throw new RuntimeException("Failed to call Groq API after " + (attempts + 1) + " attempts", lastException);
    }

    /**
     * Determine if an exception is retryable.
     *
     * @param e The exception to check
     * @return true if the exception should trigger a retry
     */
    private boolean shouldRetry(Exception e) {
        // Check for rate limit (429) or server errors (5xx)
        if (e instanceof org.springframework.web.client.HttpStatusCodeException httpEx) {
            int status = httpEx.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }
}
