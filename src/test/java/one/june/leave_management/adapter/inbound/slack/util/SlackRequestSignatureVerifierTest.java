package one.june.leave_management.adapter.inbound.slack.util;

import one.june.leave_management.common.exception.SlackSignatureVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SlackRequestSignatureVerifier
 * Tests signature generation and verification logic
 */
class SlackRequestSignatureVerifierTest {

    private static final String TEST_SIGNING_SECRET = "test-secret-key-12345";
    private SlackRequestSignatureVerifier signatureVerifier;

    @BeforeEach
    void setUp() {
        signatureVerifier = new SlackRequestSignatureVerifier(TEST_SIGNING_SECRET);
    }

    @Test
    @DisplayName("Should verify valid signature successfully")
    void shouldVerifyValidSignature() throws SlackSignatureVerificationException {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "{\"text\":\"hello\"}";
        String signature = generateTestSignature(timestamp, requestBody);

        // When & Then - should not throw
        signatureVerifier.verify(signature, timestamp, requestBody);
    }

    @Test
    @DisplayName("Should reject signature with missing timestamp")
    void shouldRejectMissingTimestamp() {
        // Given
        String requestBody = "{\"text\":\"hello\"}";
        String signature = generateTestSignature("1234567890", requestBody);

        // When & Then
        assertThatThrownBy(() -> signatureVerifier.verify(signature, null, requestBody))
                .isInstanceOf(SlackSignatureVerificationException.class)
                .hasMessageContaining("Missing timestamp parameter");
    }

    @Test
    @DisplayName("Should reject signature with missing signature")
    void shouldRejectMissingSignature() {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "{\"text\":\"hello\"}";

        // When & Then
        assertThatThrownBy(() -> signatureVerifier.verify(null, timestamp, requestBody))
                .isInstanceOf(SlackSignatureVerificationException.class)
                .hasMessageContaining("Missing signature parameter");
    }

    @Test
    @DisplayName("Should reject empty timestamp")
    void shouldRejectEmptyTimestamp() {
        // Given
        String requestBody = "{\"text\":\"hello\"}";
        String signature = generateTestSignature("1234567890", requestBody);

        // When & Then
        assertThatThrownBy(() -> signatureVerifier.verify(signature, "", requestBody))
                .isInstanceOf(SlackSignatureVerificationException.class)
                .hasMessageContaining("Missing timestamp parameter");
    }

    @Test
    @DisplayName("Should reject empty signature")
    void shouldRejectEmptySignature() {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "{\"text\":\"hello\"}";

        // When & Then
        assertThatThrownBy(() -> signatureVerifier.verify("", timestamp, requestBody))
                .isInstanceOf(SlackSignatureVerificationException.class)
                .hasMessageContaining("Missing signature parameter");
    }

    @Test
    @DisplayName("Should reject invalid timestamp format")
    void shouldRejectInvalidTimestampFormat() {
        // Given
        String requestBody = "{\"text\":\"hello\"}";
        String signature = generateTestSignature("1234567890", requestBody);

        // When & Then
        assertThatThrownBy(() -> signatureVerifier.verify(signature, "invalid-timestamp", requestBody))
                .isInstanceOf(SlackSignatureVerificationException.class)
                .hasMessageContaining("Invalid timestamp format");
    }

    @Test
    @DisplayName("Should reject old timestamp (more than 5 minutes)")
    void shouldRejectOldTimestamp() {
        // Given
        long oldTimestamp = Instant.now().minusSeconds(6 * 60).getEpochSecond(); // 6 minutes ago
        String requestBody = "{\"text\":\"hello\"}";
        String signature = generateTestSignature(String.valueOf(oldTimestamp), requestBody);

        // When & Then
        assertThatThrownBy(() -> signatureVerifier.verify(signature, String.valueOf(oldTimestamp), requestBody))
                .isInstanceOf(SlackSignatureVerificationException.class)
                .hasMessageContaining("Request timestamp is too old");
    }

    @Test
    @DisplayName("Should accept timestamp exactly 5 minutes old")
    void shouldAcceptExactlyFiveMinuteOldTimestamp() throws SlackSignatureVerificationException {
        // Given
        long fiveMinutesAgo = Instant.now().minusSeconds(5 * 60).getEpochSecond();
        String requestBody = "{\"text\":\"hello\"}";
        String signature = generateTestSignature(String.valueOf(fiveMinutesAgo), requestBody);

        // When & Then - should not throw
        signatureVerifier.verify(signature, String.valueOf(fiveMinutesAgo), requestBody);
    }

    @Test
    @DisplayName("Should reject incorrect signature")
    void shouldRejectIncorrectSignature() throws SlackSignatureVerificationException {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "{\"text\":\"hello\"}";
        String incorrectSignature = "v0=incorrect_signature";

        // When & Then
        assertThatThrownBy(() -> signatureVerifier.verify(incorrectSignature, timestamp, requestBody))
                .isInstanceOf(SlackSignatureVerificationException.class)
                .hasMessageContaining("Invalid signature");
    }

    @Test
    @DisplayName("Should reject signature without v0= prefix")
    void shouldRejectSignatureWithoutPrefix() throws SlackSignatureVerificationException {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "{\"text\":\"hello\"}";
        String signatureWithoutPrefix = generateTestSignature(timestamp, requestBody).substring(3); // Remove "v0="

        // When & Then
        assertThatThrownBy(() -> signatureVerifier.verify(signatureWithoutPrefix, timestamp, requestBody))
                .isInstanceOf(SlackSignatureVerificationException.class)
                .hasMessageContaining("Invalid signature");
    }

    @Test
    @DisplayName("Should handle empty request body")
    void shouldHandleEmptyRequestBody() throws SlackSignatureVerificationException {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "";
        String signature = generateTestSignature(timestamp, requestBody);

        // When & Then - should not throw
        signatureVerifier.verify(signature, timestamp, requestBody);
    }

    @Test
    @DisplayName("Should handle special characters in request body")
    void shouldHandleSpecialCharactersInRequestBody() throws SlackSignatureVerificationException {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "{\"text\":\"hello @user #channel &more!\"}";
        String signature = generateTestSignature(timestamp, requestBody);

        // When & Then - should not throw
        signatureVerifier.verify(signature, timestamp, requestBody);
    }

    @Test
    @DisplayName("Should handle unicode characters in request body")
    void shouldHandleUnicodeCharactersInRequestBody() throws SlackSignatureVerificationException {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "{\"text\":\"Hello 世界 🌍\"}";
        String signature = generateTestSignature(timestamp, requestBody);

        // When & Then - should not throw
        signatureVerifier.verify(signature, timestamp, requestBody);
    }

    @Test
    @DisplayName("Should handle large request body")
    void shouldHandleLargeRequestBody() throws SlackSignatureVerificationException {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String largeText = "a".repeat(10000);
        String requestBody = "{\"text\":\"" + largeText + "\"}";
        String signature = generateTestSignature(timestamp, requestBody);

        // When & Then - should not throw
        signatureVerifier.verify(signature, timestamp, requestBody);
    }

    @Test
    @DisplayName("Should handle newlines in request body")
    void shouldHandleNewlinesInRequestBody() throws SlackSignatureVerificationException {
        // Given
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = "{\n\"text\": \"hello\",\n\"channel\": \"test\"\n}";
        String signature = generateTestSignature(timestamp, requestBody);

        // When & Then - should not throw
        signatureVerifier.verify(signature, timestamp, requestBody);
    }

    @Test
    @DisplayName("Should generate correct signature format")
    void shouldGenerateCorrectSignatureFormat() {
        // Given
        String timestamp = "1234567890";
        String requestBody = "{\"text\":\"hello\"}";

        // When
        String signature = generateTestSignature(timestamp, requestBody);

        // Then
        assertThat(signature).startsWith("v0=");
        assertThat(signature.length()).isGreaterThan(10); // Has hash content
    }

    // Helper method to generate test signatures matching Slack's algorithm
    private String generateTestSignature(String timestamp, String requestBody) {
        try {
            String baseString = "v0:" + timestamp + ":" + requestBody;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    TEST_SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return "v0=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test signature", e);
        }
    }
}
