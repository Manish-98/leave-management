package one.june.leave_management.common.exception;

/**
 * Exception thrown when Slack signature verification fails
 * <p>
 * This exception is thrown when the X-Slack-Signature header verification fails,
 * indicating that the request did not come from Slack or was tampered with.
 * <p>
 * This is a runtime exception that will be caught by the global exception handler.
 */
public class SlackSignatureVerificationException extends SlackCommunicationException {

    /**
     * Constructs a new SlackSignatureVerificationException with the specified detail message.
     *
     * @param message the detail message explaining the verification failure
     */
    public SlackSignatureVerificationException(String message) {
        super(message);
    }

    /**
     * Constructs a new SlackSignatureVerificationException with the specified detail message and cause.
     *
     * @param message the detail message explaining the verification failure
     * @param cause   the underlying cause of the verification failure
     */
    public SlackSignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
