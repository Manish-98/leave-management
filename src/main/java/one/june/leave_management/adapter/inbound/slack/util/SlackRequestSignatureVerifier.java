package one.june.leave_management.adapter.inbound.slack.util;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.common.exception.SlackSignatureVerificationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for verifying Slack request signatures
 * This ensures that requests actually come from Slack and not from malicious actors
 */
@Slf4j
public class SlackRequestSignatureVerifier {

    private final String signingSecret;

    public SlackRequestSignatureVerifier(String signingSecret) {
        this.signingSecret = signingSecret;
    }

    /**
     * Verifies the Slack request signature
     *
     * @param signature The signature from X-Slack-Signature header
     * @param timestamp The timestamp from X-Slack-Request-Timestamp header
     * @param requestBody The raw request body string
     * @throws SlackSignatureVerificationException if the signature is invalid
     */
    public void verify(String signature, String timestamp, String requestBody) throws SlackSignatureVerificationException {
        if (timestamp == null || timestamp.isEmpty()) {
            throw new SlackSignatureVerificationException("Missing timestamp parameter");
        }

        if (signature == null || signature.isEmpty()) {
            throw new SlackSignatureVerificationException("Missing signature parameter");
        }

        // Verify the timestamp is not too old (prevent replay attacks)
        try {
            long requestTimestamp = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            long timeDifference = currentTime - requestTimestamp;

            if (timeDifference > 5 * 60) { // 5 minutes
                throw new SlackSignatureVerificationException("Request timestamp is too old");
            }
        } catch (NumberFormatException e) {
            throw new SlackSignatureVerificationException("Invalid timestamp format");
        }

        // Verify the signature
        String expectedSignature = "v0=" + generateSignature(timestamp, requestBody);

        log.debug("Signature: {}, expected: {}", signature, expectedSignature);

        if (!signature.equals(expectedSignature)) {
            throw new SlackSignatureVerificationException("Invalid signature");
        }

        log.debug("Slack signature verified successfully");
    }

    /**
     * Generates a signature for the given timestamp and request body
     *
     * @param timestamp The request timestamp
     * @param requestBody The request body
     * @return The generated signature
     */
    private String generateSignature(String timestamp, String requestBody) {
        try {
            String baseString = "v0:" + timestamp + ":" + requestBody;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
}
