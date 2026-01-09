package one.june.leave_management.application.slack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import one.june.leave_management.adapter.outbound.slack.client.SlackApiClient;
import one.june.leave_management.application.slack.dto.SlackUserDto;
import one.june.leave_management.application.slack.dto.SlackUserListResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SlackUserService.
 * Tests the application service layer for Slack user operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SlackUserService Tests")
class SlackUserServiceTest {

    @Mock
    private SlackApiClient slackApiClient;

    @InjectMocks
    private SlackUserService slackUserService;

    private List<SlackUserDto> testUsers;

    @BeforeEach
    void setUp() {
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

    @Test
    @DisplayName("Should fetch all workspace users successfully")
    void testFetchAllWorkspaceUsers_Success() {
        // Arrange
        when(slackApiClient.fetchWorkspaceUsers()).thenReturn(testUsers);

        // Act
        SlackUserListResponse result = slackUserService.fetchAllWorkspaceUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getUsers().size());
        assertEquals("U12345", result.getUsers().get(0).getSlackId());
        assertEquals("John Doe", result.getUsers().get(0).getName());
        assertEquals("john.doe@example.com", result.getUsers().get(0).getEmail());

        verify(slackApiClient).fetchWorkspaceUsers();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void testFetchAllWorkspaceUsers_EmptyList() {
        // Arrange
        List<SlackUserDto> emptyList = new ArrayList<>();
        when(slackApiClient.fetchWorkspaceUsers()).thenReturn(emptyList);

        // Act
        SlackUserListResponse result = slackUserService.fetchAllWorkspaceUsers();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertTrue(result.getUsers().isEmpty());

        verify(slackApiClient).fetchWorkspaceUsers();
    }

    @Test
    @DisplayName("Should handle users with null email")
    void testFetchAllWorkspaceUsers_WithNullEmail() {
        // Arrange
        List<SlackUserDto> usersWithNullEmail = new ArrayList<>();
        usersWithNullEmail.add(SlackUserDto.builder()
                .slackId("U12345")
                .name("John Doe")
                .displayName("john.doe")
                .email(null)
                .teamId("T12345")
                .isActive(true)
                .isBot(false)
                .deleted(false)
                .build());

        when(slackApiClient.fetchWorkspaceUsers()).thenReturn(usersWithNullEmail);

        // Act
        SlackUserListResponse result = slackUserService.fetchAllWorkspaceUsers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertNull(result.getUsers().get(0).getEmail());

        verify(slackApiClient).fetchWorkspaceUsers();
    }

    @Test
    @DisplayName("Should handle bot and deleted users")
    void testFetchAllWorkspaceUsers_WithBotAndDeletedUsers() {
        // Arrange
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
                .displayName("bot.user")
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

        when(slackApiClient.fetchWorkspaceUsers()).thenReturn(mixedUsers);

        // Act
        SlackUserListResponse result = slackUserService.fetchAllWorkspaceUsers();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalCount());

        // Verify first user is regular
        assertTrue(result.getUsers().get(0).getIsActive());
        assertFalse(result.getUsers().get(0).getIsBot());
        assertFalse(result.getUsers().get(0).getDeleted());

        // Verify second user is bot
        assertFalse(result.getUsers().get(1).getIsActive());
        assertTrue(result.getUsers().get(1).getIsBot());

        // Verify third user is deleted
        assertFalse(result.getUsers().get(2).getIsActive());
        assertTrue(result.getUsers().get(2).getDeleted());

        verify(slackApiClient).fetchWorkspaceUsers();
    }

    @Test
    @DisplayName("Should propagate exception from SlackApiClient")
    void testFetchAllWorkspaceUsers_ApiException() {
        // Arrange
        RuntimeException expectedException = new RuntimeException("Slack API error: invalid_auth");
        when(slackApiClient.fetchWorkspaceUsers()).thenThrow(expectedException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            slackUserService.fetchAllWorkspaceUsers();
        });

        assertEquals("Slack API error: invalid_auth", thrown.getMessage());
        verify(slackApiClient).fetchWorkspaceUsers();
    }
}
