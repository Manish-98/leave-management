package one.june.leave_management.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.application.slack.dto.SlackUserListResponse;
import one.june.leave_management.application.slack.service.SlackUserService;
import one.june.leave_management.common.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Slack management operations.
 * Provides endpoints for admins to interact with Slack workspace data.
 */
@RestController
@RequestMapping("/api/admin/slack")
@Tag(name = "Slack Management", description = "Admin APIs for Slack workspace management")
@Slf4j
public class SlackManagementController {

    private final SlackUserService slackUserService;

    public SlackManagementController(SlackUserService slackUserService) {
        this.slackUserService = slackUserService;
    }

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Fetch all users from Slack workspace",
            description = """
                    Retrieves all users from the Slack workspace including:
                    - Basic user information (ID, name, display name)
                    - Email addresses
                    - Team membership
                    - User status (active, deleted, bot)

                    This endpoint fetches real-time data from Slack API and handles
                    pagination automatically for workspaces with many users.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully fetched all Slack users",
                    content = @Content(schema = @Schema(implementation = SlackUserListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error communicating with Slack API",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Slack integration is disabled or not configured",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SlackUserListResponse> fetchWorkspaceUsers() {
        log.info("Received request to fetch all users from Slack workspace");

        SlackUserListResponse response = slackUserService.fetchAllWorkspaceUsers();

        log.info("Successfully fetched {} users from Slack workspace", response.getTotalCount());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
