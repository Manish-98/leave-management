package one.june.leave_management.adapter.outbound.slack.client;

import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageResponse;
import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.SlackViewOpenResponse;
import one.june.leave_management.adapter.outbound.slack.dto.composition.SlackText;
import one.june.leave_management.config.SlackProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

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
}
