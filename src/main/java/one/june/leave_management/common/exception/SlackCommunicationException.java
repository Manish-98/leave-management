package one.june.leave_management.common.exception;

/**
 * Base exception for all Slack communication errors
 * <p>
 * This exception is thrown when there are issues communicating with Slack's API,
 * including network errors, API failures, authentication issues, etc.
 * <p>
 * This is a runtime exception that will be caught by the global exception handler,
 * which will return 200 OK to prevent Slack from retrying the request.
 */
public class SlackCommunicationException extends RuntimeException {

    /**
     * Constructs a new SlackCommunicationException with the specified detail message.
     *
     * @param message the detail message explaining the communication failure
     */
    public SlackCommunicationException(String message) {
        super(message);
    }

    /**
     * Constructs a new SlackCommunicationException with the specified detail message and cause.
     *
     * @param message the detail message explaining the communication failure
     * @param cause   the underlying cause of the communication failure
     */
    public SlackCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
