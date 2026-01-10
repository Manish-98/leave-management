package one.june.leave_management.adapter.genai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for Groq Chat Completions API.
 * Follows the OpenAI-compatible format used by Groq.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroqChatRequest {

    /**
     * ID of the model to use (e.g., "llama-3.1-8b-instant", "mixtral-8x7b-32768")
     */
    private String model;

    /**
     * Messages in the conversation
     */
    private List<Message> messages;

    /**
     * Temperature for response randomness (0.0 to 2.0)
     */
    @Builder.Default
    private double temperature = 0.7;

    /**
     * Maximum tokens in the response
     */
    @JsonProperty("max_tokens")
    @Builder.Default
    private int maxTokens = 1024;

    /**
     * Response format - can be set to JSON mode
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    /**
     * Message format compatible with Groq/OpenAI API
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    /**
     * Response format configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        private String type; // "json_object" for structured output
    }
}
