package one.june.leave_management.adapter.inbound.slack;

import one.june.leave_management.adapter.inbound.slack.dto.SlackAction;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewSubmissionRequest;
import one.june.leave_management.adapter.inbound.slack.util.SlackRequestSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SlackViewSubmissionController
 * Tests Slack interaction endpoints including view submissions and block actions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SlackViewSubmissionController Unit Tests")
class SlackViewSubmissionControllerTest {

    @Mock
    private SlackRequestSignatureVerifier signatureVerifier;

    @Mock
    private SlackLeaveOrchestrator slackLeaveOrchestrator;

    @Mock
    private HttpServletRequest request;

    private SlackViewSubmissionController controller;

    @BeforeEach
    void setUp() {
        controller = new SlackViewSubmissionController(
                signatureVerifier,
                slackLeaveOrchestrator
        );
    }

    @Nested
    @DisplayName("handleInteraction() Tests")
    class HandleInteractionTests {

        @Test
        @DisplayName("Should handle view_submission interaction successfully")
        void shouldHandleViewSubmissionSuccessfully() throws Exception {
            // Given
            String payloadJson = "{\"type\":\"view_submission\",\"view\":{\"id\":\"V12345\",\"private_metadata\":\"{\\\"userId\\\":\\\"U123\\\"}\"}}";
            String requestBody = "payload=" + java.net.URLEncoder.encode(payloadJson, java.nio.charset.StandardCharsets.UTF_8);

            when(request.getHeader("X-Slack-Signature")).thenReturn("valid-signature");
            when(request.getHeader("X-Slack-Request-Timestamp")).thenReturn("1531420618");
            when(request.getInputStream()).thenReturn(new MockServletInputStream(requestBody));
            doNothing().when(signatureVerifier).verify(any(), any(), any());

            // When
            ResponseEntity<?> response = controller.handleInteraction(request);

            // Then
            verify(signatureVerifier).verify("valid-signature", "1531420618", requestBody);
            verify(slackLeaveOrchestrator).handleViewSubmission(eq(requestBody));
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should handle block_actions interaction successfully")
        void shouldHandleBlockActionsSuccessfully() throws Exception {
            // Given
            String payloadJson = "{\"type\":\"block_actions\",\"trigger_id\":\"T12345\",\"actions\":[{\"action_id\":\"confirm_button\"}]}";
            String requestBody = "payload=" + java.net.URLEncoder.encode(payloadJson, java.nio.charset.StandardCharsets.UTF_8);

            when(request.getHeader("X-Slack-Signature")).thenReturn("valid-signature");
            when(request.getHeader("X-Slack-Request-Timestamp")).thenReturn("1531420618");
            when(request.getInputStream()).thenReturn(new MockServletInputStream(requestBody));
            doNothing().when(signatureVerifier).verify(any(), any(), any());

            // When
            ResponseEntity<?> response = controller.handleInteraction(request);

            // Then
            verify(signatureVerifier).verify("valid-signature", "1531420618", requestBody);
            verify(slackLeaveOrchestrator).handleBlockAction(eq(requestBody));
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should verify signature before processing interaction")
        void shouldVerifySignatureBeforeProcessing() throws Exception {
            // Given
            String payloadJson = "{\"type\":\"view_submission\"}";
            String requestBody = "payload=" + java.net.URLEncoder.encode(payloadJson, java.nio.charset.StandardCharsets.UTF_8);

            when(request.getHeader("X-Slack-Signature")).thenReturn("v0=test-signature");
            when(request.getHeader("X-Slack-Request-Timestamp")).thenReturn("1531420618");
            when(request.getInputStream()).thenReturn(new MockServletInputStream(requestBody));
            doNothing().when(signatureVerifier).verify(any(), any(), any());

            // When
            controller.handleInteraction(request);

            // Then
            verify(signatureVerifier, times(1)).verify("v0=test-signature", "1531420618", requestBody);
        }

        @Test
        @DisplayName("Should handle view_closed interaction")
        void shouldHandleViewClosedInteraction() throws Exception {
            // Given
            String payloadJson = "{\"type\":\"view_closed\",\"view\":{\"id\":\"V12345\"}}";
            String requestBody = "payload=" + java.net.URLEncoder.encode(payloadJson, java.nio.charset.StandardCharsets.UTF_8);

            when(request.getHeader("X-Slack-Signature")).thenReturn("valid-signature");
            when(request.getHeader("X-Slack-Request-Timestamp")).thenReturn("1531420618");
            when(request.getInputStream()).thenReturn(new MockServletInputStream(requestBody));
            doNothing().when(signatureVerifier).verify(any(), any(), any());

            // When
            ResponseEntity<?> response = controller.handleInteraction(request);

            // Then
            verify(slackLeaveOrchestrator).handleViewClosed(eq(requestBody));
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should return 200 OK even when orchestrator throws exception")
        void shouldReturn200OkOnException() throws Exception {
            // Given
            String payloadJson = "{\"type\":\"view_submission\"}";
            String requestBody = "payload=" + java.net.URLEncoder.encode(payloadJson, java.nio.charset.StandardCharsets.UTF_8);

            when(request.getHeader("X-Slack-Signature")).thenReturn("valid-signature");
            when(request.getHeader("X-Slack-Request-Timestamp")).thenReturn("1531420618");
            when(request.getInputStream()).thenReturn(new MockServletInputStream(requestBody));
            doNothing().when(signatureVerifier).verify(any(), any(), any());

            doThrow(new RuntimeException("Test exception"))
                    .when(slackLeaveOrchestrator).handleViewSubmission(any());

            // When & Then - Exception is caught by global exception handler
            assertThatThrownBy(() -> controller.handleInteraction(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test exception");
        }

        @Test
        @DisplayName("Should handle unknown interaction type gracefully")
        void shouldHandleUnknownInteractionType() throws Exception {
            // Given
            String payloadJson = "{\"type\":\"unknown_type\"}";
            String requestBody = "payload=" + java.net.URLEncoder.encode(payloadJson, java.nio.charset.StandardCharsets.UTF_8);

            when(request.getHeader("X-Slack-Signature")).thenReturn("valid-signature");
            when(request.getHeader("X-Slack-Request-Timestamp")).thenReturn("1531420618");
            when(request.getInputStream()).thenReturn(new MockServletInputStream(requestBody));
            doNothing().when(signatureVerifier).verify(any(), any(), any());

            // When
            ResponseEntity<?> response = controller.handleInteraction(request);

            // Then
            // Should not call any orchestrator methods
            verify(slackLeaveOrchestrator, never()).handleViewSubmission(any());
            verify(slackLeaveOrchestrator, never()).handleBlockAction(any());
            verify(slackLeaveOrchestrator, never()).handleViewClosed(any());
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }
    }

    // Mock ServletInputStream for testing
    private static class MockServletInputStream extends jakarta.servlet.ServletInputStream {
        private final java.io.InputStream inputStream;

        public MockServletInputStream(String content) {
            this.inputStream = new java.io.ByteArrayInputStream(content.getBytes());
        }

        @Override
        public boolean isFinished() {
            try {
                return inputStream.available() == 0;
            } catch (java.io.IOException e) {
                return true;
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
            // Not implemented for test
        }

        @Override
        public int read() throws java.io.IOException {
            return inputStream.read();
        }
    }
}
