package one.june.leave_management.adapter.inbound.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import one.june.leave_management.common.exception.DomainException;
import one.june.leave_management.common.exception.ErrorResponse;
import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.common.exception.SlackApiException;
import one.june.leave_management.common.exception.SlackCommunicationException;
import one.june.leave_management.common.exception.SlackModalException;
import one.june.leave_management.common.exception.SlackPayloadParseException;
import one.june.leave_management.common.exception.SlackSignatureVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OverlappingLeaveException.class)
    public ResponseEntity<ErrorResponse> handleOverlappingLeaveException(
            OverlappingLeaveException ex,
            HttpServletRequest request
    ) {
        logger.warn("Overlapping leave request for user {}: {}", ex.getUserId(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            DomainException ex,
            HttpServletRequest request
    ) {
        logger.warn("Domain exception occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        logger.warn("Illegal argument exception: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        logger.warn("Validation failed for request: {}", request.getRequestURI());

        List<ErrorResponse.ValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> ErrorResponse.ValidationError.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .rejectedValue(fieldError.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request parameters")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        logger.warn("Type mismatch exception for parameter {}: {}", ex.getName(), ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========== Slack Exception Handlers ==========

    /**
     * Handles Slack signature verification failures
     * <p>
     * Returns 200 OK to prevent Slack from retrying the request.
     * Logs the error for monitoring and debugging.
     *
     * @param ex      The signature verification exception
     * @param request The HTTP request
     * @return 200 OK with empty body
     */
    @ExceptionHandler(SlackSignatureVerificationException.class)
    public ResponseEntity<Void> handleSlackSignatureVerificationException(
            SlackSignatureVerificationException ex,
            HttpServletRequest request
    ) {
        logger.error("Slack signature verification failed for {}: {}", request.getRequestURI(), ex.getMessage());

        // Return 200 OK to prevent Slack from retrying
        // The error has been logged, and no response body is needed
        return ResponseEntity.ok().build();
    }

    /**
     * Handles Slack payload parsing failures
     * <p>
     * Returns 200 OK to prevent Slack from retrying the request.
     * Logs the error for monitoring and debugging.
     *
     * @param ex      The payload parse exception
     * @param request The HTTP request
     * @return 200 OK with empty body
     */
    @ExceptionHandler(SlackPayloadParseException.class)
    public ResponseEntity<Void> handleSlackPayloadParseException(
            SlackPayloadParseException ex,
            HttpServletRequest request
    ) {
        logger.error("Slack payload parsing failed for {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        // Return 200 OK to prevent Slack from retrying
        // The error has been logged, and no response body is needed
        return ResponseEntity.ok().build();
    }

    /**
     * Handles Slack API communication failures
     * <p>
     * Returns 200 OK to prevent Slack from retrying the request.
     * Logs detailed error information including endpoint and error code.
     *
     * @param ex      The Slack API exception
     * @param request The HTTP request
     * @return 200 OK with empty body
     */
    @ExceptionHandler(SlackApiException.class)
    public ResponseEntity<Void> handleSlackApiException(
            SlackApiException ex,
            HttpServletRequest request
    ) {
        logger.error("Slack API error on endpoint '{}' (code: {}) for {}: {}",
                ex.getEndpoint(), ex.getErrorCode(), request.getRequestURI(), ex.getMessage());

        // Return 200 OK to prevent Slack from retrying
        // The error has been logged with full context
        // Success/failure will be communicated via thread messages
        return ResponseEntity.ok().build();
    }

    /**
     * Handles Slack modal operation failures
     * <p>
     * Returns 200 OK to prevent Slack from retrying the request.
     * Logs detailed error information including trigger_id and user_id.
     *
     * @param ex      The Slack modal exception
     * @param request The HTTP request
     * @return 200 OK with empty body
     */
    @ExceptionHandler(SlackModalException.class)
    public ResponseEntity<Void> handleSlackModalException(
            SlackModalException ex,
            HttpServletRequest request
    ) {
        logger.error("Slack modal error for user '{}' (trigger_id: {}) on {}: {}",
                ex.getUserId(), ex.getTriggerId(), request.getRequestURI(), ex.getMessage());

        // Return 200 OK to prevent Slack from retrying
        // The error has been logged with full context
        return ResponseEntity.ok().build();
    }

    /**
     * Handles all Slack communication exceptions (catch-all)
     * <p>
     * Returns 200 OK to prevent Slack from retrying the request.
     * Logs the error for monitoring and debugging.
     *
     * @param ex      The Slack communication exception
     * @param request The HTTP request
     * @return 200 OK with empty body
     */
    @ExceptionHandler(SlackCommunicationException.class)
    public ResponseEntity<Void> handleSlackCommunicationException(
            SlackCommunicationException ex,
            HttpServletRequest request
    ) {
        logger.error("Slack communication error for {}: {}", request.getRequestURI(), ex.getMessage());

        // Return 200 OK to prevent Slack from retrying
        // The error has been logged, and no response body is needed
        return ResponseEntity.ok().build();
    }

    // ========== End Slack Exception Handlers ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
