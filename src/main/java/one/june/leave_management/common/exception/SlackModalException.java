package one.june.leave_management.common.exception;

/**
 * Exception thrown when Slack modal operations fail
 * <p>
 * This exception wraps errors related to Slack modal interactions, including:
 * <ul>
 *   <li>Opening modals via trigger_id</li>
 *   <li>Expired or invalid trigger_id values</li>
 *   <li>Invalid modal view definitions</li>
 *   <li>Modal rendering failures</li>
 * </ul>
 * <p>
 * This is a runtime exception that will be caught by the global exception handler.
 */
public class SlackModalException extends SlackCommunicationException {

    private final String triggerId;
    private final String userId;
    private final String reason;

    /**
     * Constructs a new SlackModalException with detailed context.
     *
     * @param triggerId The trigger_id that was used (or null if not available)
     * @param userId    The Slack user ID for whom the modal operation failed
     * @param reason    The reason for the failure (e.g., "expired_trigger_id", "invalid_view")
     */
    public SlackModalException(String triggerId, String userId, String reason) {
        super(String.format("Failed to open modal for user '%s': %s", userId, reason));
        this.triggerId = triggerId;
        this.userId = userId;
        this.reason = reason;
    }

    /**
     * Constructs a new SlackModalException with detailed context and an underlying cause.
     *
     * @param triggerId The trigger_id that was used (or null if not available)
     * @param userId    The Slack user ID for whom the modal operation failed
     * @param reason    The reason for the failure (e.g., "expired_trigger_id", "invalid_view")
     * @param cause     The underlying cause of the modal failure
     */
    public SlackModalException(String triggerId, String userId, String reason, Throwable cause) {
        super(String.format("Failed to open modal for user '%s': %s", userId, reason), cause);
        this.triggerId = triggerId;
        this.userId = userId;
        this.reason = reason;
    }

    /**
     * Constructs a new SlackModalException with a generic message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public SlackModalException(String message, Throwable cause) {
        super(message, cause);
        this.triggerId = "unknown";
        this.userId = "unknown";
        this.reason = message;
    }

    public String getTriggerId() {
        return triggerId;
    }

    public String getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }
}
