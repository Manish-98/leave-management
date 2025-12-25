package one.june.leave_management.common.exception;

/**
 * Exception thrown when parsing Slack payload fails
 * <p>
 * This exception is used when there are issues parsing form-encoded payloads
 * from Slack requests, such as missing required fields, malformed JSON,
 * or invalid payload structure.
 */
public class SlackPayloadParseException extends RuntimeException {

    /**
     * Constructs a new SlackPayloadParseException with the specified detail message.
     *
     * @param message the detail message explaining the parsing failure
     */
    public SlackPayloadParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new SlackPayloadParseException with the specified detail message and cause.
     *
     * @param message the detail message explaining the parsing failure
     * @param cause   the underlying cause of the parsing failure
     */
    public SlackPayloadParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
