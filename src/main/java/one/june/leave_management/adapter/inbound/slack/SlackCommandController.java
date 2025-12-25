package one.june.leave_management.adapter.inbound.slack;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandResponse;
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
 * REST controller for handling Slack slash commands
 * <p>
 * Handles incoming slash command requests from Slack.
 * <p>
 * This controller is intentionally thin and only handles:
 * <ul>
 *   <li>Signature verification for security</li>
 *   <li>Request parsing and routing</li>
 *   <li>Returning appropriate HTTP responses</li>
 * </ul>
 * <p>
 * All business logic is delegated to the {@link SlackLeaveOrchestrator}.
 */
@Slf4j
@RestController
@RequestMapping("/integrations/slack/commands")
@ConditionalOnProperty(name = "slack.enabled", havingValue = "true", matchIfMissing = true)
public class SlackCommandController {

    private final SlackRequestSignatureVerifier signatureVerifier;
    private final SlackLeaveOrchestrator slackLeaveOrchestrator;

    public SlackCommandController(
            SlackRequestSignatureVerifier signatureVerifier,
            SlackLeaveOrchestrator slackLeaveOrchestrator
    ) {
        this.signatureVerifier = signatureVerifier;
        this.slackLeaveOrchestrator = slackLeaveOrchestrator;
    }

    /**
     * Handles the /leave slash command from Slack
     * <p>
     * Process:
     * <ol>
     *   <li>Read request body</li>
     *   <li>Verify Slack signature to ensure request is from Slack</li>
     *   <li>Parse form payload to SlackCommandRequest</li>
     *   <li>Route to orchestrator for business logic</li>
     *   <li>ACK immediately with 200 OK</li>
     * </ol>
     * <p>
     * Slack requires a response within 3 seconds, so we ACK immediately
     * and handle all processing asynchronously via the orchestrator.
     * <p>
     * All exceptions are handled by the global exception handler, which returns
     * 200 OK to prevent Slack from retrying failed requests.
     *
     * @param request The HTTP request from Slack
     * @param rawBody The raw request body for signature verification
     * @return ResponseEntity with empty body to ACK the request
     */
    @PostMapping("/leave")
    @Auditable("Slack command endpoint")
    public ResponseEntity<SlackCommandResponse> handleLeaveCommand(
            HttpServletRequest request,
            @RequestBody byte[] rawBody
    ) {
        String requestBody = new String(rawBody, StandardCharsets.UTF_8);
        log.info("Received Slack command request");

        // Step 1: Verify signature to ensure request is from Slack
        String signature = request.getHeader("X-Slack-Signature");
        String timestamp = request.getHeader("X-Slack-Request-Timestamp");
        signatureVerifier.verify(signature, timestamp, requestBody);

        // Step 2: Parse command request from form payload
        SlackCommandRequest slackRequest = SlackRequestParser.parseCommandPayload(
                requestBody,
                SlackCommandRequest.class
        );

        log.info("Parsed /leave command from user: {} in channel: {}",
                slackRequest.getUserId(),
                slackRequest.getChannelName());

        // Step 3: Route to orchestrator for business logic
        slackLeaveOrchestrator.handleSlashCommand(slackRequest);

        // Step 4: ACK immediately with 200 OK
        return ResponseEntity.ok().build();
    }
}
