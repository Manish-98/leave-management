package one.june.leave_management.common.exception;

/**
 * Exception thrown when Slack signature verification fails
 */
public class SlackSignatureVerificationException extends Exception {

    public SlackSignatureVerificationException(String message) {
        super(message);
    }

    public SlackSignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
