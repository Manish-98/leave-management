package one.june.leave_management.adapter.inbound.slack.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.common.exception.SlackPayloadParseException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic utility for parsing Slack request payloads
 * <p>
 * This utility handles form-encoded payloads from Slack, extracting and parsing JSON data.
 * <ul>
 *   <li>Slash commands: Direct form fields (no payload wrapper)</li>
 *   <li>Interactions: application/x-www-form-urlencoded with JSON in "payload" parameter</li>
 * </ul>
 * <p>
 * This class provides type-safe parsing using Java generics and Jackson ObjectMapper.
 */
@Slf4j
public class SlackRequestParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extracts the JSON payload string from a form-encoded request body
     * <p>
     * Slack sends payloads in the format: payload={json_string}
     * This method extracts and decodes the JSON string.
     *
     * @param requestBody The raw form-encoded request body
     * @return The decoded JSON payload string
     * @throws SlackPayloadParseException if the payload parameter is missing or empty
     */
    public static String extractPayloadJson(String requestBody) {
        try {
            Map<String, String> formPayload = parseFormPayload(requestBody);
            String payloadJson = formPayload.get("payload");

            if (payloadJson == null || payloadJson.isEmpty()) {
                throw new SlackPayloadParseException("Missing 'payload' parameter in request body");
            }

            log.debug("Extracted payload JSON: {}", payloadJson);
            return payloadJson;

        } catch (Exception e) {
            throw new SlackPayloadParseException("Failed to extract payload from request body", e);
        }
    }

    /**
     * Parses the form-encoded payload to a specific type
     * <p>
     * This method extracts the JSON payload and deserializes it to the specified target class
     * using Jackson's ObjectMapper.
     *
     * @param requestBody   The raw form-encoded request body
     * @param targetClass   The class to parse the payload into
     * @param <T>           The type of the target class
     * @return An instance of the target class populated with data from the payload
     * @throws SlackPayloadParseException if parsing fails
     */
    public static <T> T parsePayload(String requestBody, Class<T> targetClass) {
        try {
            String payloadJson = extractPayloadJson(requestBody);
            return objectMapper.readValue(payloadJson, targetClass);

        } catch (Exception e) {
            throw new SlackPayloadParseException(
                    "Failed to parse payload to type: " + targetClass.getName(), e);
        }
    }

    /**
     * Extracts the "type" field from the payload
     * <p>
     * This is used to route Slack interactions to the appropriate handler.
     * Common types include: "view_submission", "view_closed", "block_actions", etc.
     *
     * @param requestBody The raw form-encoded request body
     * @return The type field value (e.g., "view_submission")
     * @throws SlackPayloadParseException if the type field is missing
     */
    public static String extractType(String requestBody) {
        try {
            String payloadJson = extractPayloadJson(requestBody);
            var rootNode = objectMapper.readTree(payloadJson);
            String type = rootNode.path("type").asText();

            if (type == null || type.isEmpty()) {
                throw new SlackPayloadParseException("Missing 'type' field in payload");
            }

            log.debug("Extracted interaction type: {}", type);
            return type;

        } catch (SlackPayloadParseException e) {
            throw e;
        } catch (Exception e) {
            throw new SlackPayloadParseException("Failed to extract type from payload", e);
        }
    }

    /**
     * Parses a slash command request from form-encoded payload
     * <p>
     * Slash commands from Slack are sent as direct form fields (no "payload" wrapper).
     * This method parses the form data and maps it to a SlackCommandRequest object.
     * <p>
     * Expected form fields:
     * <ul>
     *   <li>command: The slash command that was used (e.g., "/leave")</li>
     *   <li>text: Any text after the command</li>
     *   <li>trigger_id: Short-lived ID for opening modals</li>
     *   <li>user_id: The user who invoked the command</li>
     *   <li>user_name: Username of the invoker</li>
     *   <li>channel_id: Channel where command was invoked</li>
     *   <li>channel_name: Channel name</li>
     *   <li>team_id: Workspace ID</li>
     *   <li>team_domain: Workspace domain</li>
     *   <li>response_url: URL for sending delayed responses</li>
     *   <li>api_app_id: Your app's ID</li>
     * </ul>
     *
     * @param requestBody   The raw form-encoded request body
     * @param targetClass   The class to parse the command into (SlackCommandRequest.class)
     * @param <T>           The type of the target class
     * @return A populated instance of the target class
     * @throws SlackPayloadParseException if parsing fails or required fields are missing
     */
    public static <T> T parseCommandPayload(String requestBody, Class<T> targetClass) {
        try {
            Map<String, String> formPayload = parseFormPayload(requestBody);

            // Check if target class is SlackCommandRequest for specialized handling
            if (targetClass.getName().contains("SlackCommandRequest")) {
                return parseSlackCommandRequest(formPayload, targetClass);
            }

            // Use Jackson to populate the target object from the map
            return objectMapper.convertValue(formPayload, targetClass);

        } catch (Exception e) {
            throw new SlackPayloadParseException(
                    "Failed to parse slash command payload to type: " + targetClass.getName(), e);
        }
    }

    /**
     * Specialized parser for SlackCommandRequest that handles underscore to camelCase conversion
     *
     * @param formPayload The parsed form data map
     * @param targetClass The target class type
     * @param <T>         The type parameter
     * @return A populated SlackCommandRequest instance
     */
    @SuppressWarnings("unchecked")
    private static <T> T parseSlackCommandRequest(Map<String, String> formPayload, Class<T> targetClass) {
        // Use the builder pattern directly
        SlackCommandRequest request = SlackCommandRequest.builder()
                .command(formPayload.get("command"))
                .text(formPayload.get("text"))
                .triggerId(formPayload.get("trigger_id"))
                .userId(formPayload.get("user_id"))
                .userName(formPayload.get("user_name"))
                .channelId(formPayload.get("channel_id"))
                .channelName(formPayload.get("channel_name"))
                .teamId(formPayload.get("team_id"))
                .teamDomain(formPayload.get("team_domain"))
                .responseUrl(formPayload.get("response_url"))
                .apiAppId(formPayload.get("api_app_id"))
                .build();

        return (T) request;
    }

    /**
     * Parses a form-encoded request body into a map of string keys and values
     * <p>
     * Form encoding format: key1=value1&key2=value2
     * Values are URL-decoded.
     *
     * @param requestBody The raw form-encoded request body
     * @return A map of parameter names to decoded values
     */
    private static Map<String, String> parseFormPayload(String requestBody) {
        return Arrays.stream(requestBody.split("&"))
                .map(param -> param.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                ));
    }
}
