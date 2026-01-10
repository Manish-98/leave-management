package one.june.leave_management.adapter.outbound.slack.client;

import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageResponse;
import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.SlackUsersListResponse;
import one.june.leave_management.adapter.outbound.slack.dto.SlackViewOpenResponse;
import one.june.leave_management.adapter.outbound.slack.dto.SlackViewUpdateRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackViewUpdateResponse;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;
import one.june.leave_management.application.slack.dto.SlackUserDto;
import one.june.leave_management.config.SlackProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link SlackApiClient}
 * <p>
 * Tests all HTTP interactions with the Slack API including:
 * - Modal opening (views.open API)
 * - Error message posting (response_url)
 * - Message posting (chat.postMessage API)
 * - Thread replies (chat.postMessage with thread_ts)
 */
@ExtendWith(MockitoExtension.class)
class SlackApiClientTest {

    private SlackApiClient slackApiClient;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SlackProperties slackProperties;

    @BeforeEach
    void setUp() {
        // Configure default Slack properties
        lenient().when(slackProperties.getBotToken()).thenReturn("xoxb-test-token");
        lenient().when(slackProperties.getApiBaseUrl()).thenReturn("https://slack.com/api");
        lenient().when(slackProperties.getViewsOpenEndpoint()).thenReturn("/views.open");
        lenient().when(slackProperties.getViewsUpdateEndpoint()).thenReturn("/views.update");

        // Manually create the SlackApiClient with mocked dependencies
        slackApiClient = new SlackApiClient(restTemplate, slackProperties);
    }

    @Nested
    @DisplayName("openModal() Tests")
    class OpenModalTests {

        private SlackModalView createTestModalView() {
            return SlackModalView.builder()
                    .type("modal")
                    .title(SlackText.plainText("Test Modal", false))
                    .blocks(java.util.List.of())
                    .build();
        }

        private SlackViewOpenResponse createSuccessResponse() {
            return SlackViewOpenResponse.builder()
                    .ok(true)
                    .view(SlackViewOpenResponse.SlackViewResponse.builder()
                            .id("V12345")
                            .externalId("external-id-123")
                            .build())
                    .build();
        }

        @Test
        @DisplayName("Should open modal with valid inputs")
        void shouldOpenModalWithValidInputs() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();
            SlackViewOpenResponse expectedResponse = createSuccessResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SlackViewOpenResponse actualResponse = slackApiClient.openModal(triggerId, view);

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.isOk()).isTrue();
            assertThat(actualResponse.getView()).isNotNull();
            assertThat(actualResponse.getView().getId()).isEqualTo("V12345");

            verify(restTemplate).exchange(
                    eq("https://slack.com/api/views.open"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when botToken is null")
        void shouldThrowExceptionWhenBotTokenIsNull() {
            // Given
            when(slackProperties.getBotToken()).thenReturn(null);
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Slack bot token is not configured");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when botToken is empty")
        void shouldThrowExceptionWhenBotTokenIsEmpty() {
            // Given
            when(slackProperties.getBotToken()).thenReturn("   ");
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Slack bot token is not configured");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when triggerId is null")
        void shouldThrowExceptionWhenTriggerIdIsNull() {
            // Given
            String triggerId = null;
            SlackModalView view = createTestModalView();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Trigger ID cannot be null or empty");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when triggerId is empty")
        void shouldThrowExceptionWhenTriggerIdIsEmpty() {
            // Given
            String triggerId = "   ";
            SlackModalView view = createTestModalView();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Trigger ID cannot be null or empty");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when view is null")
        void shouldThrowExceptionWhenViewIsNull() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = null;

            // When & Then - should throw RuntimeException wrapping NullPointerException
            // The builder accepts null, but the API call fails
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API");

            // Note: restTemplate.exchange IS called with null view, then fails
            verify(restTemplate).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            );
        }

        @Test
        @DisplayName("Should handle null response body from Slack")
        void shouldHandleNullResponseBodyFromSlack() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            )).thenReturn(ResponseEntity.ok(null));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API")
                    .hasMessageContaining("Null response from Slack API");
        }

        @Test
        @DisplayName("Should handle non-200 HTTP status codes")
        void shouldHandleNon200HttpStatusCodes() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();
            SlackViewOpenResponse errorResponse = SlackViewOpenResponse.builder()
                    .ok(false)
                    .error("account_inactive")
                    .build();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            )).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API")
                    .hasMessageContaining("account_inactive");
        }

        @Test
        @DisplayName("Should handle Slack API error responses")
        void shouldHandleSlackApiErrorResponses() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();
            SlackViewOpenResponse errorResponse = SlackViewOpenResponse.builder()
                    .ok(false)
                    .error("trigger_expired")
                    .build();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            )).thenReturn(ResponseEntity.ok(errorResponse));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API")
                    .hasMessageContaining("trigger_expired");
        }

        @Test
        @DisplayName("Should propagate Slack error messages")
        void shouldPropagateSlackErrorMessages() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();
            String errorMessage = "invalid_auth";
            SlackViewOpenResponse errorResponse = SlackViewOpenResponse.builder()
                    .ok(false)
                    .error(errorMessage)
                    .build();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            )).thenReturn(ResponseEntity.ok(errorResponse));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API")
                    .hasMessageContaining(errorMessage);
        }

        @Test
        @DisplayName("Should handle RestClientException")
        void shouldHandleRestClientException() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            )).thenThrow(new RestClientException("Network error"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API")
                    .hasCauseExactlyInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("Should handle HttpClientErrorException")
        void shouldHandleHttpClientErrorException() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            )).thenThrow(new RestClientException("401 Unauthorized"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API");
        }

        @Test
        @DisplayName("Should handle HttpServerErrorException")
        void shouldHandleHttpServerErrorException() {
            // Given
            String triggerId = "trigger-id-123";
            SlackModalView view = createTestModalView();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewOpenResponse.class)
            )).thenThrow(new RestClientException("500 Internal Server Error"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.openModal(triggerId, view))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API");
        }
    }

    @Nested
    @DisplayName("postErrorMessage() Tests")
    class PostErrorMessageTests {

        @Test
        @DisplayName("Should post error message with valid inputs")
        void shouldPostErrorMessageWithValidInputs() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            String errorMessage = "An error occurred";

            when(restTemplate.exchange(
                    eq(responseUrl),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok("ok"));

            // When
            slackApiClient.postErrorMessage(responseUrl, errorMessage);

            // Then
            verify(restTemplate).exchange(
                    eq(responseUrl),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("Should skip posting when responseUrl is null")
        void shouldSkipPostingWhenResponseUrlIsNull() {
            // Given
            String responseUrl = null;
            String errorMessage = "An error occurred";

            // When
            slackApiClient.postErrorMessage(responseUrl, errorMessage);

            // Then
            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should skip posting when responseUrl is empty")
        void shouldSkipPostingWhenResponseUrlIsEmpty() {
            // Given
            String responseUrl = "   ";
            String errorMessage = "An error occurred";

            // When
            slackApiClient.postErrorMessage(responseUrl, errorMessage);

            // Then
            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should skip posting when errorMessage is null")
        void shouldSkipPostingWhenErrorMessageIsNull() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            String errorMessage = null;

            // When
            slackApiClient.postErrorMessage(responseUrl, errorMessage);

            // Then
            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should skip posting when errorMessage is empty")
        void shouldSkipPostingWhenErrorMessageIsEmpty() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            String errorMessage = "   ";

            // When
            slackApiClient.postErrorMessage(responseUrl, errorMessage);

            // Then
            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should handle RestClientException gracefully")
        void shouldHandleRestClientExceptionGracefully() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            String errorMessage = "An error occurred";

            when(restTemplate.exchange(
                    eq(responseUrl),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Network error"));

            // When & Then - should not throw exception
            slackApiClient.postErrorMessage(responseUrl, errorMessage);

            verify(restTemplate).exchange(
                    eq(responseUrl),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("Should handle generic exceptions gracefully")
        void shouldHandleGenericExceptionsGracefully() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            String errorMessage = "An error occurred";

            when(restTemplate.exchange(
                    eq(responseUrl),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RuntimeException("Unexpected error"));

            // When & Then - should not throw exception
            slackApiClient.postErrorMessage(responseUrl, errorMessage);

            verify(restTemplate).exchange(
                    eq(responseUrl),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("postMessage() Tests")
    class PostMessageTests {

        private SlackMessageRequest createTestMessageRequest() {
            return SlackMessageRequest.builder()
                    .text("Test message")
                    .build();
        }

        private SlackMessageResponse createSuccessMessageResponse() {
            return SlackMessageResponse.builder()
                    .ok(true)
                    .channel("C12345")
                    .ts("1234567890.123456")
                    .build();
        }

        @Test
        @DisplayName("Should post message to channel successfully")
        void shouldPostMessageToChannelSuccessfully() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();
            SlackMessageResponse expectedResponse = createSuccessMessageResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SlackMessageResponse actualResponse = slackApiClient.postMessage(channelId, message);

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.isOk()).isTrue();
            assertThat(actualResponse.getChannel()).isEqualTo("C12345");
            assertThat(actualResponse.getTs()).isEqualTo("1234567890.123456");

            verify(restTemplate).exchange(
                    eq("https://slack.com/api/chat.postMessage"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when botToken is null")
        void shouldThrowExceptionWhenBotTokenIsNull() {
            // Given
            when(slackProperties.getBotToken()).thenReturn(null);
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Slack bot token is not configured");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when botToken is empty")
        void shouldThrowExceptionWhenBotTokenIsEmpty() {
            // Given
            when(slackProperties.getBotToken()).thenReturn("   ");
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Slack bot token is not configured");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when channelId is null")
        void shouldThrowExceptionWhenChannelIdIsNull() {
            // Given
            String channelId = null;
            SlackMessageRequest message = createTestMessageRequest();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Channel ID cannot be null or empty");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when channelId is empty")
        void shouldThrowExceptionWhenChannelIdIsEmpty() {
            // Given
            String channelId = "   ";
            SlackMessageRequest message = createTestMessageRequest();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Channel ID cannot be null or empty");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when message is null")
        void shouldThrowExceptionWhenMessageIsNull() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = null;

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Message request cannot be null");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should handle null response body")
        void shouldHandleNullResponseBody() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenReturn(ResponseEntity.ok(null));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API")
                    .hasMessageContaining("Null response from Slack API");
        }

        @Test
        @DisplayName("Should handle non-200 status codes")
        void shouldHandleNon200StatusCodes() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();
            SlackMessageResponse errorResponse = SlackMessageResponse.builder()
                    .ok(false)
                    .error("channel_not_found")
                    .build();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API")
                    .hasMessageContaining("channel_not_found");
        }

        @Test
        @DisplayName("Should handle Slack API error responses")
        void shouldHandleSlackApiErrorResponses() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();
            SlackMessageResponse errorResponse = SlackMessageResponse.builder()
                    .ok(false)
                    .error("not_in_channel")
                    .build();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenReturn(ResponseEntity.ok(errorResponse));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API")
                    .hasMessageContaining("not_in_channel");
        }

        @Test
        @DisplayName("Should propagate Slack error messages")
        void shouldPropagateSlackErrorMessages() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();
            String errorMessage = "message_too_long";
            SlackMessageResponse errorResponse = SlackMessageResponse.builder()
                    .ok(false)
                    .error(errorMessage)
                    .build();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenReturn(ResponseEntity.ok(errorResponse));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API")
                    .hasMessageContaining(errorMessage);
        }

        @Test
        @DisplayName("Should handle RestClientException")
        void shouldHandleRestClientException() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenThrow(new RestClientException("Network error"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API")
                    .hasCauseExactlyInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("Should handle HTTP client errors")
        void shouldHandleHttpClientErrors() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenThrow(new RestClientException("403 Forbidden"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API");
        }

        @Test
        @DisplayName("Should handle server errors")
        void shouldHandleServerErrors() {
            // Given
            String channelId = "C12345";
            SlackMessageRequest message = createTestMessageRequest();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenThrow(new RestClientException("503 Service Unavailable"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postMessage(channelId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API");
        }
    }

    @Nested
    @DisplayName("postThreadReply() Tests")
    class PostThreadReplyTests {

        private SlackMessageRequest createTestMessageRequest() {
            return SlackMessageRequest.builder()
                    .text("Test reply")
                    .build();
        }

        private SlackMessageResponse createSuccessMessageResponse() {
            return SlackMessageResponse.builder()
                    .ok(true)
                    .channel("C12345")
                    .ts("1234567890.123456")
                    .build();
        }

        @Test
        @DisplayName("Should delegate to postMessage method")
        void shouldDelegateToPostMessageMethod() {
            // Given
            String channelId = "C12345";
            String threadTs = "1234567890.123456";
            SlackMessageRequest message = createTestMessageRequest();
            SlackMessageResponse expectedResponse = createSuccessMessageResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SlackMessageResponse actualResponse = slackApiClient.postThreadReply(channelId, threadTs, message);

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getTs()).isEqualTo("1234567890.123456");

            verify(restTemplate).exchange(
                    eq("https://slack.com/api/chat.postMessage"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackMessageResponse.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when threadTs is null")
        void shouldThrowExceptionWhenThreadTsIsNull() {
            // Given
            String channelId = "C12345";
            String threadTs = null;
            SlackMessageRequest message = createTestMessageRequest();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postThreadReply(channelId, threadTs, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Thread timestamp cannot be null or empty");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when threadTs is empty")
        void shouldThrowExceptionWhenThreadTsIsEmpty() {
            // Given
            String channelId = "C12345";
            String threadTs = "   ";
            SlackMessageRequest message = createTestMessageRequest();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postThreadReply(channelId, threadTs, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Thread timestamp cannot be null or empty");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should validate other inputs through postMessage")
        void shouldValidateOtherInputsThroughPostMessage() {
            // Given
            String channelId = null;
            String threadTs = "1234567890.123456";
            SlackMessageRequest message = createTestMessageRequest();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postThreadReply(channelId, threadTs, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Channel ID cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("updateModal() Tests")
    class UpdateModalTests {

        private SlackModalView createTestModalView() {
            return SlackModalView.builder()
                    .type("modal")
                    .title(SlackText.plainText("Test Modal", false))
                    .blocks(java.util.List.of())
                    .build();
        }

        private SlackViewUpdateResponse createSuccessResponse() {
            return SlackViewUpdateResponse.builder()
                    .ok(true)
                    .view(SlackViewUpdateResponse.SlackModalViewResponse.builder()
                            .id("V12345")
                            .build())
                    .build();
        }

        private SlackViewUpdateResponse createErrorResponse(String error) {
            return SlackViewUpdateResponse.builder()
                    .ok(false)
                    .error(error)
                    .build();
        }

        @Test
        @DisplayName("Should update modal with valid inputs")
        void shouldUpdateModalWithValidInputs() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";
            SlackViewUpdateResponse expectedResponse = createSuccessResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SlackViewUpdateResponse actualResponse = slackApiClient.updateModal(viewId, view, viewHash);

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getOk()).isTrue();
            assertThat(actualResponse.getView()).isNotNull();
            assertThat(actualResponse.getView().getId()).isEqualTo("V12345");

            verify(restTemplate).exchange(
                    eq("https://slack.com/api/views.update"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            );
        }

        @Test
        @DisplayName("Should handle null viewHash parameter")
        void shouldHandleNullViewHashParameter() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = null;
            SlackViewUpdateResponse expectedResponse = createSuccessResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SlackViewUpdateResponse actualResponse = slackApiClient.updateModal(viewId, view, viewHash);

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getOk()).isTrue();

            verify(restTemplate).exchange(
                    eq("https://slack.com/api/views.update"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            );
        }

        @Test
        @DisplayName("Should handle empty viewHash parameter")
        void shouldHandleEmptyViewHashParameter() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "";
            SlackViewUpdateResponse expectedResponse = createSuccessResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SlackViewUpdateResponse actualResponse = slackApiClient.updateModal(viewId, view, viewHash);

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getOk()).isTrue();

            verify(restTemplate).exchange(
                    eq("https://slack.com/api/views.update"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            );
        }

        @Test
        @DisplayName("Should return response with updated view")
        void shouldReturnResponseWithUpdatedView() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";
            SlackViewUpdateResponse expectedResponse = SlackViewUpdateResponse.builder()
                    .ok(true)
                    .view(SlackViewUpdateResponse.SlackModalViewResponse.builder()
                            .id("V12345")
                            .teamId("T12345")
                            .build())
                    .build();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SlackViewUpdateResponse actualResponse = slackApiClient.updateModal(viewId, view, viewHash);

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getOk()).isTrue();
            assertThat(actualResponse.getView()).isNotNull();
            assertThat(actualResponse.getView().getId()).isEqualTo("V12345");
            assertThat(actualResponse.getView().getTeamId()).isEqualTo("T12345");
        }

        @Test
        @DisplayName("Should throw exception when botToken is null")
        void shouldThrowExceptionWhenBotTokenIsNull() {
            // Given
            when(slackProperties.getBotToken()).thenReturn(null);
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Slack bot token is not configured");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when botToken is empty")
        void shouldThrowExceptionWhenBotTokenIsEmpty() {
            // Given
            when(slackProperties.getBotToken()).thenReturn("   ");
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Slack bot token is not configured");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when viewId is null")
        void shouldThrowExceptionWhenViewIdIsNull() {
            // Given
            String viewId = null;
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("View ID cannot be null or empty");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when viewId is empty")
        void shouldThrowExceptionWhenViewIdIsEmpty() {
            // Given
            String viewId = "   ";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("View ID cannot be null or empty");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should throw exception when updatedView is null")
        void shouldThrowExceptionWhenUpdatedViewIsNull() {
            // Given
            String viewId = "V12345";
            SlackModalView view = null;
            String viewHash = "hash123";

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Updated view cannot be null");

            verify(restTemplate, never()).exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    any(Class.class)
            );
        }

        @Test
        @DisplayName("Should handle null response body from Slack")
        void shouldHandleNullResponseBodyFromSlack() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(null));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Unexpected error calling Slack API: Null response from Slack API");
        }

        @Test
        @DisplayName("Should handle Slack API error responses")
        void shouldHandleSlackApiErrorResponses() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";
            SlackViewUpdateResponse errorResponse = createErrorResponse("account_inactive");

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(errorResponse));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Unexpected error calling Slack API: Slack API error: account_inactive");
        }

        @Test
        @DisplayName("Should propagate Slack error messages")
        void shouldPropagateSlackErrorMessages() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";
            SlackViewUpdateResponse errorResponse = createErrorResponse("invalid_auth");

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(errorResponse));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("invalid_auth");
        }

        @Test
        @DisplayName("Should handle non-200 HTTP status codes")
        void shouldHandleNon200HttpStatusCodes() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Unexpected error calling Slack API: Null response from Slack API");
        }

        @Test
        @DisplayName("Should handle RestClientException")
        void shouldHandleRestClientException() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenThrow(new RestClientException("Network error"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API")
                    .hasCauseInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("Should handle HttpClientErrorException")
        void shouldHandleHttpClientErrorException() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            HttpClientErrorException httpException = new HttpClientErrorException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token"
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenThrow(httpException);

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API")
                    .hasCauseInstanceOf(HttpClientErrorException.class);
        }

        @Test
        @DisplayName("Should handle HttpServerErrorException")
        void shouldHandleHttpServerErrorException() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            HttpServerErrorException httpException = new HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server error"
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenThrow(httpException);

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API")
                    .hasCauseInstanceOf(HttpServerErrorException.class);
        }

        @Test
        @DisplayName("Should handle generic exceptions")
        void shouldHandleGenericExceptions() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenThrow(new RuntimeException("Unexpected error"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateModal(viewId, view, viewHash))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API");
        }

        @Test
        @DisplayName("Should convert SlackModalView correctly")
        void shouldConvertSlackModalViewCorrectly() {
            // Given
            String viewId = "V12345";
            SlackModalView view = SlackModalView.builder()
                    .type("modal")
                    .title(SlackText.plainText("Test Title", false))
                    .blocks(java.util.List.of())
                    .privateMetadata("private-data")
                    .callbackId("callback-123")
                    .submit(SlackText.plainText("Submit", true))
                    .close(SlackText.plainText("Close", true))
                    .notifyOnClose(true)
                    .clearOnClose(false)
                    .build();
            String viewHash = "hash123";
            SlackViewUpdateResponse expectedResponse = createSuccessResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            slackApiClient.updateModal(viewId, view, viewHash);

            // Then
            ArgumentCaptor<HttpEntity<SlackViewUpdateRequest>> entityCaptor =
                    ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(
                    eq("https://slack.com/api/views.update"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(SlackViewUpdateResponse.class)
            );

            SlackViewUpdateRequest capturedRequest = entityCaptor.getValue().getBody();
            assertThat(capturedRequest).isNotNull();
            assertThat(capturedRequest.getViewId()).isEqualTo(viewId);
            assertThat(capturedRequest.getHash()).isEqualTo(viewHash);
            assertThat(capturedRequest.getView()).isNotNull();
            assertThat(capturedRequest.getView().getType()).isEqualTo("modal");
            assertThat(capturedRequest.getView().getPrivateMetadata()).isEqualTo("private-data");
            assertThat(capturedRequest.getView().getCallbackId()).isEqualTo("callback-123");
            assertThat(capturedRequest.getView().getNotifyOnClose()).isTrue();
            assertThat(capturedRequest.getView().getClearOnClose()).isFalse();
        }

        @Test
        @DisplayName("Should include hash parameter in request")
        void shouldIncludeHashParameterInRequest() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "test-hash-123";
            SlackViewUpdateResponse expectedResponse = createSuccessResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            slackApiClient.updateModal(viewId, view, viewHash);

            // Then
            ArgumentCaptor<HttpEntity<SlackViewUpdateRequest>> entityCaptor =
                    ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(
                    eq("https://slack.com/api/views.update"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(SlackViewUpdateResponse.class)
            );

            SlackViewUpdateRequest capturedRequest = entityCaptor.getValue().getBody();
            assertThat(capturedRequest).isNotNull();
            assertThat(capturedRequest.getHash()).isEqualTo(viewHash);
        }

        @Test
        @DisplayName("Should set correct headers")
        void shouldSetCorrectHeaders() {
            // Given
            String viewId = "V12345";
            SlackModalView view = createTestModalView();
            String viewHash = "hash123";
            SlackViewUpdateResponse expectedResponse = createSuccessResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            slackApiClient.updateModal(viewId, view, viewHash);

            // Then
            ArgumentCaptor<HttpEntity<SlackViewUpdateRequest>> entityCaptor =
                    ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(
                    eq("https://slack.com/api/views.update"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(SlackViewUpdateResponse.class)
            );

            HttpEntity<SlackViewUpdateRequest> capturedEntity = entityCaptor.getValue();
            assertThat(capturedEntity.getHeaders()).isNotNull();
            assertThat(capturedEntity.getHeaders().getContentType()).isNotNull();
            assertThat(capturedEntity.getHeaders().getContentType().toString()).contains("application/json");
            assertThat(capturedEntity.getHeaders().getContentType().getCharset()).isNotNull();
            assertThat(capturedEntity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer xoxb-test-token");
        }

        @Test
        @DisplayName("Should exclude externalId from update request")
        void shouldExcludeExternalIdFromUpdateRequest() {
            // Given
            String viewId = "V12345";
            SlackModalView view = SlackModalView.builder()
                    .type("modal")
                    .title(SlackText.plainText("Test", false))
                    .externalId("external-123") // This should be excluded (only valid for views.open)
                    .blocks(java.util.List.of())
                    .build();
            String viewHash = "hash123";
            SlackViewUpdateResponse expectedResponse = createSuccessResponse();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(SlackViewUpdateResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            slackApiClient.updateModal(viewId, view, viewHash);

            // Then
            ArgumentCaptor<HttpEntity<SlackViewUpdateRequest>> entityCaptor =
                    ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(
                    eq("https://slack.com/api/views.update"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(SlackViewUpdateResponse.class)
            );

            SlackViewUpdateRequest capturedRequest = entityCaptor.getValue().getBody();
            assertThat(capturedRequest).isNotNull();
            assertThat(capturedRequest.getView()).isNotNull();
            // SlackModalViewUpdate class doesn't have externalId field (only valid for views.open)
            // The fact that the request was successful proves externalId was not included
            assertThat(capturedRequest.getView().getType()).isEqualTo("modal");
        }
    }

    @Nested
    @DisplayName("postErrorMessage() Edge Cases")
    class PostErrorMessageEdgeCases {

        @Test
        @DisplayName("Should handle null response URL gracefully")
        void shouldHandleNullResponseUrl() {
            // Given
            String responseUrl = null;
            String errorMessage = "Test error";

            // When & Then - Should not throw, just log and return
            slackApiClient.postErrorMessage(responseUrl, errorMessage);
            // No exception thrown
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        }

        @Test
        @DisplayName("Should handle empty response URL gracefully")
        void shouldHandleEmptyResponseUrl() {
            // Given
            String responseUrl = "   ";
            String errorMessage = "Test error";

            // When & Then
            slackApiClient.postErrorMessage(responseUrl, errorMessage);
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        }

        @Test
        @DisplayName("Should handle null error message gracefully")
        void shouldHandleNullErrorMessage() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            String errorMessage = null;

            // When & Then
            slackApiClient.postErrorMessage(responseUrl, errorMessage);
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        }

        @Test
        @DisplayName("Should handle empty error message gracefully")
        void shouldHandleEmptyErrorMessage() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            String errorMessage = "   ";

            // When & Then
            slackApiClient.postErrorMessage(responseUrl, errorMessage);
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        }

        @Test
        @DisplayName("Should handle HTTP errors when posting error message")
        void shouldHandleHttpErrorsWhenPostingError() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            String errorMessage = "Test error";

            when(restTemplate.exchange(
                    eq(responseUrl),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

            // When & Then - Should log but not throw
            slackApiClient.postErrorMessage(responseUrl, errorMessage);
            // Exception is caught and logged, not re-thrown
        }
    }

    @Nested
    @DisplayName("updateMessage() Validation Tests")
    class UpdateMessageValidationTests {

        @Test
        @DisplayName("Should validate message timestamp is not null")
        void shouldValidateMessageTimestamp() {
            // Given
            String channelId = "C12345";
            String messageTs = null;
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Updated message")
                    .build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage(channelId, messageTs, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Message timestamp cannot be null or empty");
        }

        @Test
        @DisplayName("Should validate message timestamp is not empty")
        void shouldValidateMessageTimestampNotEmpty() {
            // Given
            String channelId = "C12345";
            String messageTs = "   ";
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Updated message")
                    .build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage(channelId, messageTs, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Message timestamp cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("postThreadReply() Validation Tests")
    class PostThreadReplyValidationTests {

        @Test
        @DisplayName("Should validate thread timestamp is not null")
        void shouldValidateThreadTimestamp() {
            // Given
            String channelId = "C12345";
            String threadTs = null;
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Reply")
                    .build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postThreadReply(channelId, threadTs, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Thread timestamp cannot be null or empty");
        }

        @Test
        @DisplayName("Should validate thread timestamp is not empty")
        void shouldValidateThreadTimestampNotEmpty() {
            // Given
            String channelId = "C12345";
            String threadTs = "   ";
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Reply")
                    .build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.postThreadReply(channelId, threadTs, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Thread timestamp cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("fetchWorkspaceUsers() Pagination Tests")
    class FetchWorkspaceUsersPaginationTests {

        @Test
        @DisplayName("Should handle single page of users")
        void shouldHandleSinglePageOfUsers() {
            // Given
            SlackUsersListResponse response = SlackUsersListResponse.builder()
                    .ok(true)
                    .members(java.util.List.of(
                            createSlackUser("U001", "user1")
                    ))
                    .responseMetadata(SlackUsersListResponse.ResponseMetadata.builder()
                            .nextCursor("") // No more pages
                            .build())
                    .build();

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(SlackUsersListResponse.class)
            )).thenReturn(ResponseEntity.ok(response));

            // When
            java.util.List<SlackUserDto> users = slackApiClient.fetchWorkspaceUsers();

            // Then
            assertThat(users).hasSize(1);
            verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SlackUsersListResponse.class));
        }
    }

    @Nested
    @DisplayName("sendViaResponseUrl() Validation Tests")
    class SendViaResponseUrlValidationTests {

        @Test
        @DisplayName("Should validate response URL is not null")
        void shouldValidateResponseUrl() {
            // Given
            String responseUrl = null;
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Test message")
                    .build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.sendViaResponseUrl(responseUrl, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Response URL cannot be null or empty");
        }

        @Test
        @DisplayName("Should validate message is not null")
        void shouldValidateMessage() {
            // Given
            String responseUrl = "https://hooks.slack.com/commands/1234/5678";
            SlackMessageRequest message = null;

            // When & Then
            assertThatThrownBy(() -> slackApiClient.sendViaResponseUrl(responseUrl, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Message cannot be null");
        }

        @Test
        @DisplayName("Should validate response URL is not empty")
        void shouldValidateResponseUrlNotEmpty() {
            // Given
            String responseUrl = "   ";
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Test message")
                    .build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.sendViaResponseUrl(responseUrl, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Response URL cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("updateMessage() Tests")
    class UpdateMessageTests {

        @Test
        @DisplayName("Should update message successfully")
        void shouldUpdateMessageSuccessfully() {
            // Given
            String channelId = "C12345";
            String messageTs = "1234567890.123456";
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Updated text")
                    .build();

            SlackMessageResponse response = SlackMessageResponse.builder()
                    .ok(true)
                    .channel(channelId)
                    .ts(messageTs)
                    .message(SlackMessageResponse.SlackMessage.builder()
                            .text("Updated text")
                            .build())
                    .build();

            ResponseEntity<SlackMessageResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(SlackMessageResponse.class)))
                    .thenReturn(responseEntity);

            // When
            slackApiClient.updateMessage(channelId, messageTs, message);

            // Then
            verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(SlackMessageResponse.class));
        }

        @Test
        @DisplayName("Should throw exception when bot token is null")
        void shouldThrowExceptionWhenBotTokenNull() {
            // Given
            when(slackProperties.getBotToken()).thenReturn(null);
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", "123456", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Slack bot token is not configured");
        }

        @Test
        @DisplayName("Should throw exception when bot token is empty")
        void shouldThrowExceptionWhenBotTokenEmpty() {
            // Given
            when(slackProperties.getBotToken()).thenReturn("   ");
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", "123456", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Slack bot token is not configured");
        }

        @Test
        @DisplayName("Should throw exception when channelId is null")
        void shouldThrowExceptionWhenChannelIdNull() {
            // Given
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage(null, "123456", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Channel ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when channelId is empty")
        void shouldThrowExceptionWhenChannelIdEmpty() {
            // Given
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("   ", "123456", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Channel ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when messageTs is null")
        void shouldThrowExceptionWhenMessageTsNull() {
            // Given
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", null, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Message timestamp cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when messageTs is empty")
        void shouldThrowExceptionWhenMessageTsEmpty() {
            // Given
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", "   ", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Message timestamp cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when message is null")
        void shouldThrowExceptionWhenMessageNull() {
            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", "123456", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Message request cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when API response is null")
        void shouldThrowExceptionWhenResponseNull() {
            // Given
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();
            ResponseEntity<SlackMessageResponse> responseEntity = new ResponseEntity<>(HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(SlackMessageResponse.class)))
                    .thenReturn(responseEntity);

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", "123456", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Null response from Slack API");
        }

        @Test
        @DisplayName("Should throw exception when API returns ok=false")
        void shouldThrowExceptionWhenApiReturnsNotOk() {
            // Given
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();
            SlackMessageResponse response = SlackMessageResponse.builder()
                    .ok(false)
                    .error("Some error")
                    .build();

            ResponseEntity<SlackMessageResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(SlackMessageResponse.class)))
                    .thenReturn(responseEntity);

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", "123456", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Slack API error");
        }

        @Test
        @DisplayName("Should wrap RestClientException in RuntimeException")
        void shouldWrapRestClientException() {
            // Given
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(SlackMessageResponse.class)))
                    .thenThrow(new RestClientException("Connection error"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", "123456", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API");
        }

        @Test
        @DisplayName("Should wrap generic exception in RuntimeException")
        void shouldWrapGenericException() {
            // Given
            SlackMessageRequest message = SlackMessageRequest.builder().text("Test").build();
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(SlackMessageResponse.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.updateMessage("C123", "123456", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error calling Slack API");
        }

        @Test
        @DisplayName("Should set channel and timestamp on message before sending")
        void shouldSetChannelAndTimestampOnMessage() {
            // Given
            String channelId = "C12345";
            String messageTs = "1234567890.123456";
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Updated text")
                    .build();

            SlackMessageResponse response = SlackMessageResponse.builder()
                    .ok(true)
                    .build();

            ResponseEntity<SlackMessageResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(SlackMessageResponse.class)))
                    .thenReturn(responseEntity);

            // When
            slackApiClient.updateMessage(channelId, messageTs, message);

            // Then
            ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), entityCaptor.capture(), eq(SlackMessageResponse.class));

            SlackMessageRequest capturedRequest = (SlackMessageRequest) entityCaptor.getValue().getBody();
            assertThat(capturedRequest.getChannel()).isEqualTo(channelId);
            assertThat(capturedRequest.getTs()).isEqualTo(messageTs);
        }
    }

    @Nested
    @DisplayName("fetchWorkspaceUsers() Tests")
    class FetchWorkspaceUsersTests {

        @Test
        @DisplayName("Should fetch single page of users")
        void shouldFetchSinglePageOfUsers() {
            // Given
            String botToken = "xoxb-test-token";
            when(slackProperties.getBotToken()).thenReturn(botToken);

            SlackUsersListResponse response = SlackUsersListResponse.builder()
                    .ok(true)
                    .members(java.util.List.of(
                            createSlackUser("U001", "user1"),
                            createSlackUser("U002", "user2")
                    ))
                    .responseMetadata(SlackUsersListResponse.ResponseMetadata.builder()
                            .nextCursor("") // No more pages
                            .build())
                    .build();

            ResponseEntity<SlackUsersListResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SlackUsersListResponse.class)))
                    .thenReturn(responseEntity);

            // When
            java.util.List<one.june.leave_management.application.slack.dto.SlackUserDto> users = slackApiClient.fetchWorkspaceUsers();

            // Then
            assertThat(users).hasSize(2);
            verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SlackUsersListResponse.class));
        }

        @Test
        @DisplayName("Should fetch multiple pages of users with pagination")
        void shouldFetchMultiplePagesWithPagination() {
            // Given
            String botToken = "xoxb-test-token";
            when(slackProperties.getBotToken()).thenReturn(botToken);

            SlackUsersListResponse page1Response = SlackUsersListResponse.builder()
                    .ok(true)
                    .members(java.util.List.of(
                            createSlackUser("U001", "user1")
                    ))
                    .responseMetadata(SlackUsersListResponse.ResponseMetadata.builder()
                            .nextCursor("cursor123")
                            .build())
                    .build();

            SlackUsersListResponse page2Response = SlackUsersListResponse.builder()
                    .ok(true)
                    .members(java.util.List.of(
                            createSlackUser("U002", "user2")
                    ))
                    .responseMetadata(SlackUsersListResponse.ResponseMetadata.builder()
                            .nextCursor("") // No more pages
                            .build())
                    .build();

            // Mock first call (without cursor)
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SlackUsersListResponse.class)))
                    .thenReturn(new ResponseEntity<>(page1Response, HttpStatus.OK))
                    // Mock second call (with cursor)
                    .thenReturn(new ResponseEntity<>(page2Response, HttpStatus.OK));

            // When
            java.util.List<one.june.leave_management.application.slack.dto.SlackUserDto> users = slackApiClient.fetchWorkspaceUsers();

            // Then
            assertThat(users).hasSize(2);
            verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SlackUsersListResponse.class));
        }

        @Test
        @DisplayName("Should return empty list when no users")
        void shouldReturnEmptyListWhenNoUsers() {
            // Given
            String botToken = "xoxb-test-token";
            when(slackProperties.getBotToken()).thenReturn(botToken);

            SlackUsersListResponse response = SlackUsersListResponse.builder()
                    .ok(true)
                    .members(java.util.List.of())
                    .responseMetadata(SlackUsersListResponse.ResponseMetadata.builder()
                            .nextCursor("")
                            .build())
                    .build();

            ResponseEntity<SlackUsersListResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SlackUsersListResponse.class)))
                    .thenReturn(responseEntity);

            // When
            java.util.List<one.june.leave_management.application.slack.dto.SlackUserDto> users = slackApiClient.fetchWorkspaceUsers();

            // Then
            assertThat(users).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when API returns ok=false")
        void shouldThrowExceptionWhenApiReturnsNotOk() {
            // Given
            String botToken = "xoxb-test-token";
            when(slackProperties.getBotToken()).thenReturn(botToken);

            SlackUsersListResponse response = SlackUsersListResponse.builder()
                    .ok(false)
                    .error("Invalid auth")
                    .build();

            ResponseEntity<SlackUsersListResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SlackUsersListResponse.class)))
                    .thenReturn(responseEntity);

            // When & Then
            assertThatThrownBy(() -> slackApiClient.fetchWorkspaceUsers())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Slack API error");
        }

        @Test
        @DisplayName("Should handle RestClientException gracefully")
        void shouldHandleRestClientException() {
            // Given
            String botToken = "xoxb-test-token";
            when(slackProperties.getBotToken()).thenReturn(botToken);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SlackUsersListResponse.class)))
                    .thenThrow(new RestClientException("Network error"));

            // When & Then
            assertThatThrownBy(() -> slackApiClient.fetchWorkspaceUsers())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP error calling Slack API");
        }
    }

    @Nested
    @DisplayName("mapToSlackUserDto() Tests")
    class MapToSlackUserDtoTests {

        @Test
        @DisplayName("Should map user with all fields present")
        void shouldMapUserWithAllFields() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(false)
                    .isBot(false)
                    .presence("active")
                    .profile(SlackUsersListResponse.Profile.builder()
                            .realName("Test User")
                            .displayName("Test Display")
                            .email("test@example.com")
                            .build())
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then
            assertThat(dto.getSlackId()).isEqualTo("U123");
            assertThat(dto.getName()).isEqualTo("Test User");
            assertThat(dto.getDisplayName()).isEqualTo("Test Display");
            assertThat(dto.getTeamId()).isEqualTo("T456");
            assertThat(dto.getIsBot()).isFalse();
            assertThat(dto.getDeleted()).isFalse();
            assertThat(dto.getIsActive()).isTrue();
            assertThat(dto.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should use name fallback when profile is null")
        void shouldUseNameFallbackWhenProfileNull() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(false)
                    .isBot(false)
                    .presence("active")
                    .profile(null)
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then
            assertThat(dto.getName()).isEqualTo("testuser");
            assertThat(dto.getDisplayName()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should mark user as active when presence is active")
        void shouldMarkUserAsActiveWhenPresenceActive() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(false)
                    .isBot(false)
                    .presence("active")
                    .profile(SlackUsersListResponse.Profile.builder()
                            .realName("Test User")
                            .displayName("Test Display")
                            .build())
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then
            assertThat(dto.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should mark user as active when not deleted, not bot")
        void shouldMarkUserAsActiveWhenNotDeletedNotBot() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(false)
                    .isBot(false)
                    .presence("away")
                    .profile(SlackUsersListResponse.Profile.builder()
                            .realName("Test User")
                            .displayName("Test Display")
                            .build())
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then
            assertThat(dto.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should mark user as inactive when deleted")
        void shouldMarkUserAsInactiveWhenDeleted() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(true)
                    .isBot(false)
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then
            assertThat(dto.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should mark user as inactive when is bot")
        void shouldMarkUserAsInactiveWhenIsBot() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(false)
                    .isBot(true)
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then
            assertThat(dto.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should handle null email in profile")
        void shouldHandleNullEmailInProfile() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(false)
                    .isBot(false)
                    .profile(SlackUsersListResponse.Profile.builder()
                            .realName("Test User")
                            .displayName("Test Display")
                            .email(null)
                            .build())
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then
            assertThat(dto.getEmail()).isNull();
        }

        @Test
        @DisplayName("Should handle null real name in profile")
        void shouldHandleNullRealNameInProfile() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(false)
                    .isBot(false)
                    .profile(SlackUsersListResponse.Profile.builder()
                            .realName(null)
                            .displayName("Test Display")
                            .email(null)
                            .build())
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then - if profile exists but realName is null, name will be null (doesn't fall back to user name)
            assertThat(dto.getName()).isNull();
        }

        @Test
        @DisplayName("Should handle null display name in profile")
        void shouldHandleNullDisplayNameInProfile() throws Exception {
            // Given
            SlackUsersListResponse.SlackUser slackUser = SlackUsersListResponse.SlackUser.builder()
                    .id("U123")
                    .name("testuser")
                    .teamId("T456")
                    .deleted(false)
                    .isBot(false)
                    .profile(SlackUsersListResponse.Profile.builder()
                            .realName("Test User")
                            .displayName(null)
                            .email(null)
                            .build())
                    .build();

            // When
            java.lang.reflect.Method method = SlackApiClient.class.getDeclaredMethod("mapToSlackUserDto", SlackUsersListResponse.SlackUser.class);
            method.setAccessible(true);
            one.june.leave_management.application.slack.dto.SlackUserDto dto =
                    (one.june.leave_management.application.slack.dto.SlackUserDto) method.invoke(slackApiClient, slackUser);

            // Then - if profile exists but displayName is null, displayName will be null (doesn't fall back to user name)
            assertThat(dto.getDisplayName()).isNull();
        }
    }

    // Helper methods for fetchWorkspaceUsers tests

    private SlackUsersListResponse.SlackUser createSlackUser(String id, String name) {
        SlackUsersListResponse.Profile profile = SlackUsersListResponse.Profile.builder()
                .realName(name)
                .displayName(name)
                .email(name.toLowerCase().replace(" ", "") + "@example.com")
                .build();

        return SlackUsersListResponse.SlackUser.builder()
                .id(id)
                .name(name)
                .teamId("T123")
                .profile(profile)
                .deleted(false)
                .isBot(false)
                .presence("active")
                .build();
    }
}
