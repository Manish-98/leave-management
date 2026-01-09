package one.june.leave_management.application.slack.service;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.outbound.slack.client.SlackApiClient;
import one.june.leave_management.application.slack.dto.SlackUserDto;
import one.june.leave_management.application.slack.dto.SlackUserListResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for Slack user operations.
 * Handles fetching and processing Slack user information.
 */
@Service
@Slf4j
public class SlackUserService {

    private final SlackApiClient slackApiClient;

    public SlackUserService(SlackApiClient slackApiClient) {
        this.slackApiClient = slackApiClient;
    }

    /**
     * Fetches all users from the Slack workspace.
     * This method retrieves all users including active, deleted, and bot users.
     * <p>
     * The method handles pagination automatically and returns all users in the workspace.
     *
     * @return Response containing list of all Slack users and total count
     * @throws RuntimeException if Slack API call fails
     */
    public SlackUserListResponse fetchAllWorkspaceUsers() {
        log.info("Fetching all users from Slack workspace");

        try {
            List<SlackUserDto> users = slackApiClient.fetchWorkspaceUsers();

            log.info("Successfully fetched {} users from Slack workspace", users.size());

            return SlackUserListResponse.builder()
                    .users(users)
                    .totalCount(users.size())
                    .build();

        } catch (Exception e) {
            log.error("Error fetching users from Slack workspace: {}", e.getMessage(), e);
            throw e;
        }
    }
}
