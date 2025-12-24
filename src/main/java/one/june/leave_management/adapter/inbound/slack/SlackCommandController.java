package one.june.leave_management.adapter.inbound.slack;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandResponse;
import one.june.leave_management.adapter.inbound.slack.util.SlackRequestSignatureVerifier;
import one.june.leave_management.common.annotation.Auditable;
import one.june.leave_management.common.exception.SlackSignatureVerificationException;
import one.june.leave_management.application.leave.service.SlackModalService;
import one.june.leave_management.config.SlackProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for handling Slack slash commands
 * Handles incoming slash command requests from Slack
 */
@Slf4j
@RestController
@RequestMapping("/integrations/slack/commands")
@ConditionalOnProperty(name = "slack.enabled", havingValue = "true", matchIfMissing = true)
public class SlackCommandController {

    private final SlackRequestSignatureVerifier signatureVerifier;
    private final SlackProperties slackProperties;
    private final SlackModalService slackModalService;

    public SlackCommandController(
            SlackProperties slackProperties,
            SlackRequestSignatureVerifier signatureVerifier,
            SlackModalService slackModalService
    ) {
        this.slackProperties = slackProperties;
        this.signatureVerifier = signatureVerifier;
        this.slackModalService = slackModalService;
    }

    /**
     * Handles the /leave slash command from Slack
     *
     * Process:
     * 1. Read request body
     * 2. Verify Slack signature to ensure request is from Slack
     * 3. Parse form payload from request body
     * 4. ACK immediately (return 200 OK with empty response)
     * 5. Trigger modal opening asynchronously
     *
     * Slack requires a response within 3 seconds, so we ACK immediately
     * and handle the modal opening asynchronously
     *
     * @param request The HTTP request from Slack
     * @return ResponseEntity with empty body to ACK the request
     */
    @PostMapping("/leave")
    @Auditable("Slack command endpoint")
    public ResponseEntity<SlackCommandResponse> handleLeaveCommand(HttpServletRequest request, @RequestBody byte[] rawBody) {
        try {
            String requestBody = new String(rawBody, StandardCharsets.UTF_8);
            log.info("Slack request: {}", requestBody);

            // Extract headers for signature verification
            String signature = request.getHeader("X-Slack-Signature");
            String timestamp = request.getHeader("X-Slack-Request-Timestamp");
            signatureVerifier.verify(signature, timestamp, requestBody);

            SlackCommandRequest slackRequest = parseFormPayload(requestBody);

            log.info("Received /leave command from user: {} in channel: {}. Request: {}",
                    slackRequest.getUserId(), slackRequest.getChannelName(), slackRequest);

            slackModalService.openLeaveApplicationModalAsync(slackRequest);

            log.debug("Triggered modal opening asynchronously for trigger_id: {}", slackRequest.getTriggerId());

            return ResponseEntity.ok(SlackCommandResponse.builder().responseType("ephemeral").text("Opening leave modal...").build());

        } catch (SlackSignatureVerificationException e) {
            log.error("Invalid Slack signature", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error processing Slack command", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Parses the form payload from the request body
     * Slack sends data as application/x-www-form-urlencoded
     *
     * @param requestBody The raw request body string
     * @return Parsed Slack command request
     */
    private SlackCommandRequest parseFormPayload(String requestBody) {
        Map<String, String> formPayload = Arrays.stream(requestBody.split("&"))
                .map(param -> param.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                ));

        return SlackCommandRequest.builder()
                .command(formPayload.get("command"))
                .text(formPayload.get("text"))
                .triggerId(formPayload.get("trigger_id"))
                .userId(formPayload.get("user_id"))
                .userName(formPayload.get("user_name"))
                .channelId(formPayload.get("channel_id"))
                .channelName(formPayload.get("channel_name"))
                .teamId(formPayload.get("team_id"))
                .teamDomain(formPayload.get("team_domain"))
                .responseUrl(formPayload.get("response_url"))
                .apiAppId(formPayload.get("api_app_id"))
                .build();
    }

}
