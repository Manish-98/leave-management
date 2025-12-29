package one.june.leave_management.adapter.inbound.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.common.exception.DomainException;
import one.june.leave_management.common.exception.ErrorResponse;
import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.common.exception.SlackApiException;
import one.june.leave_management.common.exception.SlackCommunicationException;
import one.june.leave_management.common.exception.SlackModalException;
import one.june.leave_management.common.exception.SlackPayloadParseException;
import one.june.leave_management.common.exception.SlackSignatureVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Global Exception Handler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Nested
    @DisplayName("Domain Exception Tests")
    class DomainExceptionTests {

        @Test
        @DisplayName("Should handle OverlappingLeaveException")
        void shouldHandleOverlappingLeaveException() {
            // Given
            String userId = "user-123";
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 5);
            UUID existingLeaveId = UUID.randomUUID();
            OverlappingLeaveException ex = new OverlappingLeaveException(userId, startDate, endDate, existingLeaveId);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleOverlappingLeaveException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Bad Request");
            assertThat(response.getBody().getMessage()).isNotNull();
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should handle DomainException")
        void shouldHandleDomainException() {
            // Given
            String message = "Domain rule violation";
            DomainException ex = new DomainException(message);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleDomainException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Bad Request");
            assertThat(response.getBody().getMessage()).isEqualTo("Domain rule violation");
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException")
        void shouldHandleIllegalArgumentException() {
            // Given
            String message = "Invalid argument provided";
            IllegalArgumentException ex = new IllegalArgumentException(message);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Bad Request");
            assertThat(response.getBody().getMessage()).isEqualTo(message);
        }
    }

    @Nested
    @DisplayName("Not Found Exception Tests")
    class NotFoundExceptionTests {

        @Test
        @DisplayName("Should handle BulkUploadJobNotFoundException")
        void shouldHandleBulkUploadJobNotFoundException() {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJobNotFoundException ex = new BulkUploadJobNotFoundException(jobId);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleBulkUploadJobNotFoundException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getError()).isEqualTo("Not Found");
            assertThat(response.getBody().getMessage()).contains(jobId.toString());
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }
    }

    @Nested
    @DisplayName("Validation Exception Tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException")
        void shouldHandleMethodArgumentNotValidException() {
            // Given
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError1 = new FieldError("request", "userId", null, false, null, null, "User ID is required");
            FieldError fieldError2 = new FieldError("request", "dateRange", "invalid", false, null, null, "Invalid date range");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentNotValidException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid request parameters");
            assertThat(response.getBody().getFieldErrors()).hasSize(2);

            assertThat(response.getBody().getFieldErrors().get(0).getField()).isEqualTo("userId");
            assertThat(response.getBody().getFieldErrors().get(0).getMessage()).isEqualTo("User ID is required");

            assertThat(response.getBody().getFieldErrors().get(1).getField()).isEqualTo("dateRange");
            assertThat(response.getBody().getFieldErrors().get(1).getMessage()).isEqualTo("Invalid date range");
            assertThat(response.getBody().getFieldErrors().get(1).getRejectedValue()).isEqualTo("invalid");
        }

        @Test
        @DisplayName("Should handle MethodArgumentTypeMismatchException")
        void shouldHandleMethodArgumentTypeMismatchException() {
            // Given
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getName()).thenReturn("year");
            when(ex.getValue()).thenReturn("invalid");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatchException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Bad Request");
            assertThat(response.getBody().getMessage()).contains("year");
            assertThat(response.getBody().getMessage()).contains("invalid");
        }
    }

    @Nested
    @DisplayName("Slack Exception Tests")
    class SlackExceptionTests {

        @Test
        @DisplayName("Should handle SlackSignatureVerificationException with 200 OK")
        void shouldHandleSlackSignatureVerificationException() {
            // Given
            SlackSignatureVerificationException ex = new SlackSignatureVerificationException("Invalid signature");

            // When
            ResponseEntity<Void> response = exceptionHandler.handleSlackSignatureVerificationException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("Should handle SlackPayloadParseException with 200 OK")
        void shouldHandleSlackPayloadParseException() {
            // Given
            SlackPayloadParseException ex = new SlackPayloadParseException("Failed to parse payload");

            // When
            ResponseEntity<Void> response = exceptionHandler.handleSlackPayloadParseException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("Should handle SlackApiException with 200 OK")
        void shouldHandleSlackApiException() {
            // Given
            SlackApiException ex = new SlackApiException("/api/chat.postMessage", "channel_not_found", "Channel not found");

            // When
            ResponseEntity<Void> response = exceptionHandler.handleSlackApiException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("Should handle SlackModalException with 200 OK")
        void shouldHandleSlackModalException() {
            // Given
            SlackModalException ex = new SlackModalException("U123456", "trigger-id", "Failed to open modal");

            // When
            ResponseEntity<Void> response = exceptionHandler.handleSlackModalException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("Should handle SlackCommunicationException with 200 OK")
        void shouldHandleSlackCommunicationException() {
            // Given
            SlackCommunicationException ex = new SlackCommunicationException("Communication error");

            // When
            ResponseEntity<Void> response = exceptionHandler.handleSlackCommunicationException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("Generic Exception Tests")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should handle generic Exception with 500 Internal Server Error")
        void shouldHandleGenericException() {
            // Given
            Exception ex = new Exception("Unexpected error occurred");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should handle RuntimeException")
        void shouldHandleRuntimeException() {
            // Given
            RuntimeException ex = new RuntimeException("Runtime error");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().getMessage()).isNotNull();
        }
    }
}
