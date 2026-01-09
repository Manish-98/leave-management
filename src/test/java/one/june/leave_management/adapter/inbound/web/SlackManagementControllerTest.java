package one.june.leave_management.adapter.inbound.web;

import one.june.leave_management.application.slack.dto.SlackUserDto;
import one.june.leave_management.application.slack.dto.SlackUserListResponse;
import one.june.leave_management.application.slack.service.SlackUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SlackManagementController.
 * Tests the REST API layer for Slack management operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SlackManagementController Tests")
class SlackManagementControllerTest {

    @Mock
    private SlackUserService slackUserService;

    private SlackManagementController controller;

    private List<SlackUserDto> testUsers;

    @BeforeEach
    void setUp() {
        controller = new SlackManagementController(slackUserService);

        // Create test users
        testUsers = new ArrayList<>();
        testUsers.add(SlackUserDto.builder()
                .slackId("U12345")
                .name("John Doe")
                .displayName("john.doe")
                .email("john.doe@example.com")
                .teamId("T12345")
                .isActive(true)
                .isBot(false)
                .deleted(false)
                .build());

        testUsers.add(SlackUserDto.builder()
                .slackId("U67890")
                .name("Jane Smith")
                .displayName("jane.smith")
                .email("jane.smith@example.com")
                .teamId("T12345")
                .isActive(true)
                .isBot(false)
                .deleted(false)
                .build());
    }

    // ==================== GET /api/admin/slack/users Tests ====================

    @Nested
    @DisplayName("GET /api/admin/slack/users - Fetch Workspace Users Tests")
    class FetchWorkspaceUsersTests {

        @Test
        @DisplayName("Should return 200 with users list when fetch is successful")
        void shouldReturnUsersSuccessfully() {
            // Given
            SlackUserListResponse expectedResponse = SlackUserListResponse.builder()
                    .users(testUsers)
                    .totalCount(testUsers.size())
                    .build();

            when(slackUserService.fetchAllWorkspaceUsers()).thenReturn(expectedResponse);

            // When
            ResponseEntity<SlackUserListResponse> response = controller.fetchWorkspaceUsers();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTotalCount()).isEqualTo(2);
            assertThat(response.getBody().getUsers()).hasSize(2);
            assertThat(response.getBody().getUsers().get(0).getSlackId()).isEqualTo("U12345");
            assertThat(response.getBody().getUsers().get(0).getName()).isEqualTo("John Doe");
            assertThat(response.getBody().getUsers().get(1).getSlackId()).isEqualTo("U67890");

            verify(slackUserService).fetchAllWorkspaceUsers();
        }

        @Test
        @DisplayName("Should return 200 with empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsers() {
            // Given
            SlackUserListResponse emptyResponse = SlackUserListResponse.builder()
                    .users(new ArrayList<>())
                    .totalCount(0)
                    .build();

            when(slackUserService.fetchAllWorkspaceUsers()).thenReturn(emptyResponse);

            // When
            ResponseEntity<SlackUserListResponse> response = controller.fetchWorkspaceUsers();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTotalCount()).isEqualTo(0);
            assertThat(response.getBody().getUsers()).isEmpty();

            verify(slackUserService).fetchAllWorkspaceUsers();
        }

        @Test
        @DisplayName("Should return 200 with mixed user types (regular, bot, deleted)")
        void shouldHandleMixedUserTypes() {
            // Given
            List<SlackUserDto> mixedUsers = new ArrayList<>();
            mixedUsers.add(SlackUserDto.builder()
                    .slackId("U12345")
                    .name("Regular User")
                    .displayName("regular.user")
                    .email("regular@example.com")
                    .teamId("T12345")
                    .isActive(true)
                    .isBot(false)
                    .deleted(false)
                    .build());

            mixedUsers.add(SlackUserDto.builder()
                    .slackId("UBOT01")
                    .name("Bot User")
                    .displayName("bot")
                    .email(null)
                    .teamId("T12345")
                    .isActive(false)
                    .isBot(true)
                    .deleted(false)
                    .build());

            mixedUsers.add(SlackUserDto.builder()
                    .slackId("UDEL01")
                    .name("Deleted User")
                    .displayName("deleted.user")
                    .email("deleted@example.com")
                    .teamId("T12345")
                    .isActive(false)
                    .isBot(false)
                    .deleted(true)
                    .build());

            SlackUserListResponse expectedResponse = SlackUserListResponse.builder()
                    .users(mixedUsers)
                    .totalCount(3)
                    .build();

            when(slackUserService.fetchAllWorkspaceUsers()).thenReturn(expectedResponse);

            // When
            ResponseEntity<SlackUserListResponse> response = controller.fetchWorkspaceUsers();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTotalCount()).isEqualTo(3);

            // Verify user types
            assertThat(response.getBody().getUsers().get(0).getIsBot()).isFalse();
            assertThat(response.getBody().getUsers().get(0).getDeleted()).isFalse();
            assertThat(response.getBody().getUsers().get(0).getIsActive()).isTrue();

            assertThat(response.getBody().getUsers().get(1).getIsBot()).isTrue();
            assertThat(response.getBody().getUsers().get(2).getDeleted()).isTrue();

            verify(slackUserService).fetchAllWorkspaceUsers();
        }

        @Test
        @DisplayName("Should propagate exception when Slack API fails")
        void shouldPropagateExceptionOnApiFailure() {
            // Given
            RuntimeException expectedException = new RuntimeException("Slack API error: invalid_auth");

            when(slackUserService.fetchAllWorkspaceUsers()).thenThrow(expectedException);

            // When & Then
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                controller.fetchWorkspaceUsers();
            });

            verify(slackUserService).fetchAllWorkspaceUsers();
        }

        @Test
        @DisplayName("Should handle users with null email")
        void shouldHandleUsersWithNullEmail() {
            // Given
            List<SlackUserDto> usersWithNullEmail = new ArrayList<>();
            usersWithNullEmail.add(SlackUserDto.builder()
                    .slackId("U12345")
                    .name("User Without Email")
                    .displayName("noemail.user")
                    .email(null)
                    .teamId("T12345")
                    .isActive(true)
                    .isBot(false)
                    .deleted(false)
                    .build());

            SlackUserListResponse expectedResponse = SlackUserListResponse.builder()
                    .users(usersWithNullEmail)
                    .totalCount(1)
                    .build();

            when(slackUserService.fetchAllWorkspaceUsers()).thenReturn(expectedResponse);

            // When
            ResponseEntity<SlackUserListResponse> response = controller.fetchWorkspaceUsers();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUsers().get(0).getEmail()).isNull();

            verify(slackUserService).fetchAllWorkspaceUsers();
        }
    }
}
