package one.june.leave_management.adapter.inbound.slack;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.util.SlackRequestParser;
import one.june.leave_management.adapter.inbound.slack.util.SlackRequestSignatureVerifier;
import one.june.leave_management.common.annotation.Auditable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * REST controller for handling Slack interactions
 * <p>
 * Handles modal submissions (view_submission) and cancellations (view_closed).
 * <p>
 * This controller is intentionally thin and only handles:
 * <ul>
 *   <li>Signature verification for security</li>
 *   <li>Request type extraction and routing</li>
 *   <li>Returning appropriate HTTP responses</li>
 * </ul>
 * <p>
 * All business logic is delegated to the {@link SlackLeaveOrchestrator}.
 */
@Slf4j
@RestController
@RequestMapping("/integrations/slack")
@ConditionalOnProperty(name = "slack.enabled", havingValue = "true", matchIfMissing = true)
public class SlackViewSubmissionController {

    private final SlackRequestSignatureVerifier signatureVerifier;
    private final SlackLeaveOrchestrator slackLeaveOrchestrator;

    public SlackViewSubmissionController(
            SlackRequestSignatureVerifier signatureVerifier,
            SlackLeaveOrchestrator slackLeaveOrchestrator
    ) {
        this.signatureVerifier = signatureVerifier;
        this.slackLeaveOrchestrator = slackLeaveOrchestrator;
    }

    /**
     * Handles Slack interactions from modal submits and cancellations
     * <p>
     * Routes requests based on the type field in the payload:
     * <ul>
     *   <li>"view_submission": User submitted the leave form → triggers async processing</li>
     *   <li>"view_closed": User closed the modal without submitting → posts cancellation message</li>
     * </ul>
     * <p>
     * Process:
     * <ol>
     *   <li>Read request body</li>
     *   <li>Verify Slack signature to ensure request is from Slack</li>
     *   <li>Extract type field from payload</li>
     *   <li>Route to appropriate handler in orchestrator based on type</li>
     *   <li>Return empty response (Slack closes modal automatically)</li>
     * </ol>
     * <p>
     * All exceptions are handled by the global exception handler, which returns
     * 200 OK to prevent Slack from retrying failed requests.
     *
     * @param request The HTTP request from Slack
     * @param rawBody The raw request body for signature verification
     * @return ResponseEntity with empty body (Slack expects empty response for view_submission)
     */
    @PostMapping("/interactions")
    @Auditable("Slack view submission endpoint")
    public ResponseEntity<?> handleInteraction(
            HttpServletRequest request,
            @RequestBody byte[] rawBody
    ) {
        String requestBody = new String(rawBody, StandardCharsets.UTF_8);
        log.info("Received Slack interaction");

        // Step 1: Verify signature to ensure request is from Slack
        String signature = request.getHeader("X-Slack-Signature");
        String timestamp = request.getHeader("X-Slack-Request-Timestamp");
        signatureVerifier.verify(signature, timestamp, requestBody);

        // Step 2: Extract interaction type from payload
        String type = SlackRequestParser.extractType(requestBody);
        log.info("Interaction type: {}", type);

        // Step 3: Route to appropriate handler based on type
        switch (type) {
            case "view_submission" -> {
                log.debug("Routing view_submission to orchestrator");
                slackLeaveOrchestrator.handleViewSubmission(requestBody);
            }
            case "view_closed" -> {
                log.debug("Routing view_closed to orchestrator");
                slackLeaveOrchestrator.handleViewClosed(requestBody);
            }
            default -> {
                log.warn("Unknown interaction type: {}", type);
            }
        }

        // Step 4: Return empty response - Slack closes the modal automatically
        return ResponseEntity.ok().build();
    }
}
