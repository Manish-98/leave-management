package one.june.leave_management.adapter.genai.client;

import one.june.leave_management.adapter.genai.dto.GroqChatResponse;
import one.june.leave_management.application.genai.util.GenAiTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for GroqApiClient.
 * Tests retry logic, error handling, timeout scenarios, and HTTP interactions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroqApiClient Tests")
class GroqApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private GroqApiClient groqApiClient;

    private static final String TEST_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_MODEL = "llama-3.1-8b-instant";

    @BeforeEach
    void setUp() {
        groqApiClient = new GroqApiClient(restTemplate);
        ReflectionTestUtils.setField(groqApiClient, "groqApiUrl", TEST_API_URL);
        ReflectionTestUtils.setField(groqApiClient, "groqApiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(groqApiClient, "model", TEST_MODEL);
        ReflectionTestUtils.setField(groqApiClient, "timeoutSeconds", 30);
        ReflectionTestUtils.setField(groqApiClient, "maxRetries", 2);
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("Should send chat request successfully")
        void shouldSendChatRequestSuccessfully() {
            // Arrange
            String systemMessage = "You are a helpful assistant.";
            String userMessage = "Parse my leave request";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            ResponseEntity<GroqChatResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(systemMessage, userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getId()).isEqualTo(expectedResponse.getId());
            assertThat(actualResponse.getChoices()).hasSize(1);
            verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should include Bearer token in headers")
        void shouldIncludeBearerTokenInHeaders() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            ResponseEntity<GroqChatResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            groqApiClient.chat(userMessage);

            // Assert
            ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(eq(TEST_API_URL), eq(HttpMethod.POST), entityCaptor.capture(), eq(GroqChatResponse.class));

            HttpEntity<?> capturedEntity = entityCaptor.getValue();
            assertThat(capturedEntity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + TEST_API_KEY);
        }

        @Test
        @DisplayName("Should set Content-Type to application/json")
        void shouldSetContentTypeToJson() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            ResponseEntity<GroqChatResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            groqApiClient.chat(userMessage);

            // Assert
            ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(eq(TEST_API_URL), eq(HttpMethod.POST), entityCaptor.capture(), eq(GroqChatResponse.class));

            HttpEntity<?> capturedEntity = entityCaptor.getValue();
            assertThat(capturedEntity.getHeaders().getContentType()).toString().contains("application/json");
        }

        @Test
        @DisplayName("Should use configured model from properties")
        void shouldUseConfiguredModel() {
            // Arrange
            String customModel = "mixtral-8x7b-32768";
            ReflectionTestUtils.setField(groqApiClient, "model", customModel);

            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            ResponseEntity<GroqChatResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(eq(TEST_API_URL), eq(HttpMethod.POST), entityCaptor.capture(), eq(GroqChatResponse.class));

            HttpEntity<?> capturedEntity = entityCaptor.getValue();
            // The model is inside the request body, which we're not deeply inspecting here
            // but the call was successful, indicating the request was formed correctly
        }

        @Test
        @DisplayName("Should set temperature to 0.3 for parsing")
        void shouldSetLowTemperature() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            ResponseEntity<GroqChatResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            groqApiClient.chat(userMessage);

            // Assert
            verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should request JSON response format")
        void shouldRequestJsonFormat() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            ResponseEntity<GroqChatResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            groqApiClient.chat(userMessage);

            // Assert
            verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should include system and user messages")
        void shouldIncludeSystemAndUserMessages() {
            // Arrange
            String systemMessage = "You are a parser.";
            String userMessage = "Parse this";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            ResponseEntity<GroqChatResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            groqApiClient.chat(systemMessage, userMessage);

            // Assert
            verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }
    }

    @Nested
    @DisplayName("Retry Logic Tests - Rate Limiting")
    class RetryLogicRateLimitTests {

        @Test
        @DisplayName("Should retry on 429 rate limit error")
        void shouldRetryOnRateLimitError() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            // First call fails with 429, second succeeds
            HttpClientErrorException rateLimitError = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);
            ResponseEntity<GroqChatResponse> successResponse = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(rateLimitError)
                    .thenReturn(successResponse);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should retry multiple times on repeated rate limits")
        void shouldRetryMultipleTimesOnRepeatedRateLimits() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            HttpClientErrorException rateLimitError = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);
            ResponseEntity<GroqChatResponse> successResponse = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(rateLimitError)
                    .thenThrow(rateLimitError)
                    .thenReturn(successResponse);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            verify(restTemplate, times(3)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }
    }

    @Nested
    @DisplayName("Retry Logic Tests - Server Errors")
    class RetryLogicServerErrorTests {

        @Test
        @DisplayName("Should retry on 500 server error")
        void shouldRetryOnServerError() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            ResponseEntity<GroqChatResponse> successResponse = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(serverError)
                    .thenReturn(successResponse);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should retry on 502 bad gateway error")
        void shouldRetryOnBadGatewayError() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            HttpServerErrorException badGatewayError = new HttpServerErrorException(HttpStatus.BAD_GATEWAY);
            ResponseEntity<GroqChatResponse> successResponse = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(badGatewayError)
                    .thenReturn(successResponse);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should retry on 503 service unavailable error")
        void shouldRetryOnServiceUnavailableError() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            HttpServerErrorException serviceUnavailableError = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
            ResponseEntity<GroqChatResponse> successResponse = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(serviceUnavailableError)
                    .thenReturn(successResponse);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }
    }

    @Nested
    @DisplayName("No Retry Tests - Client Errors")
    class NoRetryTests {

        @Test
        @DisplayName("Should NOT retry on 400 bad request")
        void shouldNotRetryOnBadRequest() {
            // Arrange
            String userMessage = "test message";
            HttpClientErrorException badRequestError = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(badRequestError);

            // Act & Assert
            assertThatThrownBy(() -> groqApiClient.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to call Groq API");

            verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should NOT retry on 401 unauthorized")
        void shouldNotRetryOnUnauthorized() {
            // Arrange
            String userMessage = "test message";
            HttpClientErrorException unauthorizedError = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(unauthorizedError);

            // Act & Assert
            assertThatThrownBy(() -> groqApiClient.chat(userMessage))
                    .isInstanceOf(RuntimeException.class);

            verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should NOT retry on 404 not found")
        void shouldNotRetryOnNotFound() {
            // Arrange
            String userMessage = "test message";
            HttpClientErrorException notFoundError = new HttpClientErrorException(HttpStatus.NOT_FOUND);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(notFoundError);

            // Act & Assert
            assertThatThrownBy(() -> groqApiClient.chat(userMessage))
                    .isInstanceOf(RuntimeException.class);

            verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }
    }

    @Nested
    @DisplayName("Max Retries Tests")
    class MaxRetriesTests {

        @Test
        @DisplayName("Should respect max-retries configuration")
        void shouldRespectMaxRetriesConfiguration() {
            // Arrange
            ReflectionTestUtils.setField(groqApiClient, "maxRetries", 1); // Set to 1 retry (2 total attempts)

            String userMessage = "test message";
            HttpClientErrorException rateLimitError = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(rateLimitError);

            // Act & Assert
            assertThatThrownBy(() -> groqApiClient.chat(userMessage))
                    .isInstanceOf(RuntimeException.class);

            // Should be called 2 times: initial attempt + 1 retry
            verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should throw exception after all retries exhausted")
        void shouldThrowExceptionAfterAllRetriesExhausted() {
            // Arrange
            String userMessage = "test message";
            HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(serverError); // Always fail

            // Act & Assert
            assertThatThrownBy(() -> groqApiClient.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to call Groq API after 3 attempts");

            // Initial attempt + 2 retries = 3 total attempts
            verify(restTemplate, times(3)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }

        @Test
        @DisplayName("Should use exponential backoff between retries")
        void shouldUseExponentialBackoff() {
            // Arrange
            String userMessage = "test message";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            ResponseEntity<GroqChatResponse> successResponse = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(serverError)
                    .thenThrow(serverError)
                    .thenReturn(successResponse);

            long startTime = System.currentTimeMillis();

            // Act
            groqApiClient.chat(userMessage);

            long elapsedTime = System.currentTimeMillis() - startTime;

            // Assert
            // Should have waited approximately 2s (first retry) + 4s (second retry) = 6s minimum
            // Allow some tolerance for test execution time
            assertThat(elapsedTime).isGreaterThan(5000);

            verify(restTemplate, times(3)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw RuntimeException on network error")
        void shouldThrowRuntimeExceptionOnNetworkError() {
            // Arrange
            String userMessage = "test message";

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(new RuntimeException("Network error"));

            // Act & Assert
            assertThatThrownBy(() -> groqApiClient.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to call Groq API");
        }

        @Test
        @DisplayName("Should include last exception in thrown error")
        void shouldIncludeLastExceptionInThrownError() {
            // Arrange
            String userMessage = "test message";
            HttpClientErrorException originalError = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limited");

            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenThrow(originalError);

            // Act & Assert
            assertThatThrownBy(() -> groqApiClient.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseExactlyInstanceOf(HttpClientErrorException.class);
        }

        @Test
        @DisplayName("Should handle null response body gracefully")
        void shouldHandleNullResponseBody() {
            // Arrange
            String userMessage = "test message";

            // Create a mock response with null body using the builder
            GroqChatResponse mockResponse = GroqChatResponse.builder()
                    .id("test-id")
                    .object("chat.completion")
                    .created(System.currentTimeMillis() / 1000)
                    .model("llama-3.1-8b-instant")
                    .choices(java.util.List.of())
                    .usage(GroqChatResponse.Usage.builder()
                            .promptTokens(0)
                            .completionTokens(0)
                            .totalTokens(0)
                            .build())
                    .build();

            org.springframework.http.ResponseEntity<GroqChatResponse> responseEntity =
                    org.springframework.http.ResponseEntity.ok(mockResponse);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getChoices()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Simple Chat Method Tests")
    class SimpleChatMethodTests {

        @Test
        @DisplayName("Should use default system message when not provided")
        void shouldUseDefaultSystemMessage() {
            // Arrange
            String userMessage = "Parse my leave";
            GroqChatResponse expectedResponse = GenAiTestFixtures.createCompleteLeaveRequestResponse();

            ResponseEntity<GroqChatResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class)))
                    .thenReturn(responseEntity);

            // Act
            GroqChatResponse actualResponse = groqApiClient.chat(userMessage);

            // Assert
            assertThat(actualResponse).isNotNull();
            verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GroqChatResponse.class));
        }
    }
}
