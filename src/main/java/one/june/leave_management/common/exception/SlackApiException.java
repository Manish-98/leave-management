package one.june.leave_management.common.exception;

/**
 * Exception thrown when Slack API communication fails
 * <p>
 * This exception wraps errors from Slack API calls, providing context about:
 * <ul>
 *   <li>The endpoint that was called</li>
 *   <li>Slack's error code (e.g., "rate_limited", "invalid_auth", "account_inactive")</li>
 *   <li>The error message from Slack</li>
 * </ul>
 * <p>
 * This is a runtime exception that will be caught by the global exception handler.
 */
public class SlackApiException extends SlackCommunicationException {

    private final String endpoint;
    private final String errorCode;
    private final String errorMessage;

    /**
     * Constructs a new SlackApiException with detailed context.
     *
     * @param endpoint    The Slack API endpoint that was called (e.g., "chat.postMessage")
     * @param errorCode   The error code from Slack (e.g., "rate_limited", "invalid_auth")
     * @param errorMessage The error message from Slack
     */
    public SlackApiException(String endpoint, String errorCode, String errorMessage) {
        super(String.format("Slack API error on endpoint '%s': %s - %s", endpoint, errorCode, errorMessage));
        this.endpoint = endpoint;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Constructs a new SlackApiException with detailed context and an underlying cause.
     *
     * @param endpoint    The Slack API endpoint that was called (e.g., "chat.postMessage")
     * @param errorCode   The error code from Slack (e.g., "rate_limited", "invalid_auth")
     * @param errorMessage The error message from Slack
     * @param cause       The underlying cause of the API error
     */
    public SlackApiException(String endpoint, String errorCode, String errorMessage, Throwable cause) {
        super(String.format("Slack API error on endpoint '%s': %s - %s", endpoint, errorCode, errorMessage), cause);
        this.endpoint = endpoint;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Constructs a new SlackApiException with a message and cause (generic constructor).
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public SlackApiException(String message, Throwable cause) {
        super(message, cause);
        this.endpoint = "unknown";
        this.errorCode = "unknown";
        this.errorMessage = message;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
