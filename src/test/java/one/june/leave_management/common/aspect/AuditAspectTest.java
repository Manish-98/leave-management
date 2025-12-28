package one.june.leave_management.common.aspect;

import one.june.leave_management.application.audit.service.AuditService;
import one.june.leave_management.domain.audit.model.AuditLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditAspect}
 * Tests the AOP auditing aspect for controller methods annotated with @Auditable.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditAspect Unit Tests")
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(auditService);
    }

    @Nested
    @DisplayName("Basic Audit Flow Tests")
    class BasicAuditFlowTests {

        @Test
        @DisplayName("Should capture all fields when request is valid")
        void shouldCaptureAllFieldsWhenRequestIsValid() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String endpoint = "/api/leaves/ingest";
            String httpMethod = "POST";

            // Create test request DTO
            TestRequestDto requestDto = new TestRequestDto("test-user");

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                // Setup mocks
                when(request.getRequestURI()).thenReturn(endpoint);
                when(request.getMethod()).thenReturn(httpMethod);
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));

                // Setup joinPoint
                when(joinPoint.getArgs()).thenReturn(new Object[]{requestDto});
                ResponseEntity<String> response = ResponseEntity.ok().body("Success");
                when(joinPoint.proceed()).thenReturn(response);

                // When
                Object result = auditAspect.auditAround(joinPoint);

                // Then
                assertThat(result).isNotNull();
                assertThat(result).isEqualTo(response);

                // Verify audit log was saved with correct fields
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getRequestId().equals(requestId) &&
                        log.getEndpoint().equals(endpoint) &&
                        log.getHttpMethod().equals(httpMethod) &&
                        log.getSourceType().equals("WEB") &&
                        log.getRequestBody() != null &&
                        log.getResponseStatus() == 200
                ));

                // Verify execution time was captured
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getExecutionTimeMs() >= 0
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should proceed without auditing when request is null")
        void shouldProceedWithoutAuditingWhenRequestIsNull() throws Throwable {
            // Given
            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);

            try {
                when(RequestContextHolder.getRequestAttributes()).thenReturn(null);

                ResponseEntity<String> response = ResponseEntity.ok().body("Success");
                when(joinPoint.proceed()).thenReturn(response);

                // When
                Object result = auditAspect.auditAround(joinPoint);

                // Then
                assertThat(result).isEqualTo(response);
                verify(auditService, never()).saveAuditLog(any());

            } finally {
                requestContextHolder.close();
            }
        }

        @Test
        @DisplayName("Should capture response status from ResponseEntity")
        void shouldCaptureResponseStatusFromResponseEntity() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                ResponseEntity<String> response = ResponseEntity.status(HttpStatus.CREATED).body("Created");
                when(joinPoint.proceed()).thenReturn(response);

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getResponseStatus() == 201
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should capture error details when exception occurs")
        void shouldCaptureErrorDetailsWhenExceptionOccurs() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            RuntimeException testException = new RuntimeException("Test error");

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                when(joinPoint.proceed()).thenThrow(testException);

                // When & Then
                assertThatThrownBy(() -> auditAspect.auditAround(joinPoint))
                        .isSameAs(testException);

                // Verify audit log captured error details
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getResponseStatus() == 500 &&
                        log.getErrorMessage().equals("Test error")
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should calculate execution time correctly")
        void shouldCalculateExecutionTimeCorrectly() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});

                // Add delay to test execution time calculation
                when(joinPoint.proceed()).thenAnswer(invocation -> {
                    Thread.sleep(50); // Sleep for 50ms
                    return ResponseEntity.ok().body("Success");
                });

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getExecutionTimeMs() >= 50 &&
                        log.getExecutionTimeMs() < 200 // Should be close to 50ms
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }
    }

    @Nested
    @DisplayName("Source Type Detection Tests")
    class SourceTypeDetectionTests {

        @Test
        @DisplayName("Should detect WEB source type for API endpoints")
        void shouldDetectWebSourceTypeForApiEndpoints() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getSourceType().equals("WEB")
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should detect SLACK source type for Slack endpoints")
        void shouldDetectSlackSourceTypeForSlackEndpoints() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/integrations/slack/interactions");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getSourceType().equals("SLACK")
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should default to WEB when endpoint is null")
        void shouldDefaultToWebWhenEndpointIsNull() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn(null);
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getSourceType().equals("WEB")
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }
    }

    @Nested
    @DisplayName("Request Body Capture Tests")
    class RequestBodyCaptureTests {

        @Test
        @DisplayName("Should capture single POJO as request body")
        void shouldCaptureSinglePojoAsRequestBody() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            TestRequestDto requestDto = new TestRequestDto("user123");

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{requestDto});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getRequestBody() != null
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should filter out HttpServletRequest from args")
        void shouldFilterOutHttpServletRequestFromArgs() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            TestRequestDto requestDto = new TestRequestDto("user123");

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                // Include HttpServletRequest in args (should be filtered out)
                when(joinPoint.getArgs()).thenReturn(new Object[]{requestDto, request});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log -> {
                    Object requestBody = log.getRequestBody();
                    return requestBody instanceof TestRequestDto;
                }));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should return null when args are null or empty")
        void shouldReturnNullWhenArgsAreNullOrEmpty() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getRequestBody() == null
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }
    }

    @Nested
    @DisplayName("Slack Payload Processing Tests")
    class SlackPayloadProcessingTests {

        @Test
        @DisplayName("Should parse Slack form-encoded payload to JSON")
        void shouldParseSlackFormEncodedPayloadToJson() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String slackPayload = "payload={\"user\":{\"id\":\"U12345\"},\"text\":\"test\"}";
            byte[] rawPayload = slackPayload.getBytes();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/integrations/slack/interactions");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{rawPayload});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log -> {
                    String requestBody = (String) log.getRequestBody();
                    return requestBody.contains("\"user\"") &&
                           requestBody.contains("U12345");
                }));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should return original for non-Slack endpoints")
        void shouldReturnOriginalForNonSlackEndpoints() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            TestRequestDto requestDto = new TestRequestDto("user123");

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{requestDto});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getRequestBody() == requestDto // Original object, not parsed
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should return original when Slack payload is invalid")
        void shouldReturnOriginalWhenSlackPayloadIsInvalid() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String invalidPayload = "invalid-form-data";
            byte[] rawPayload = invalidPayload.getBytes();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/integrations/slack/interactions");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{rawPayload});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getRequestBody() == rawPayload // Original bytes returned
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should return message for empty Slack responses")
        void shouldReturnMessageForEmptySlackResponses() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            TestRequestDto requestDto = new TestRequestDto("user123");

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/integrations/slack/interactions");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{requestDto});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body(null)); // Empty body

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log -> {
                    String responseBody = (String) log.getResponseBody();
                    return responseBody.contains("Empty response");
                }));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }
    }

    @Nested
    @DisplayName("Slack User ID Extraction Tests")
    class SlackUserIdExtractionTests {

        @Test
        @DisplayName("Should extract user ID from Slack payload")
        void shouldExtractUserIdFromSlackPayload() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String userId = "U12345";
            String slackPayload = "payload={\"user\":{\"id\":\"" + userId + "\"}}";
            byte[] rawPayload = slackPayload.getBytes();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/integrations/slack/interactions");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{rawPayload});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getUserId().equals(userId)
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should extract user ID from nested Slack payload")
        void shouldExtractUserIdFromNestedSlackPayload() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String userId = "U12345";
            // More complex nested structure
            String slackPayload = "payload={\"api_app_id\":\"A123\",\"team\":{\"id\":\"T123\"},\"user\":{\"id\":\"" + userId + "\"},\"text\":\"test\"}";
            byte[] rawPayload = slackPayload.getBytes();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/integrations/slack/interactions");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{rawPayload});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getUserId().equals(userId)
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should return null when Slack payload has no user")
        void shouldReturnNullWhenSlackPayloadHasNoUser() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String slackPayload = "payload={\"api_app_id\":\"A123\",\"team\":{\"id\":\"T123\"}}";
            byte[] rawPayload = slackPayload.getBytes();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/integrations/slack/interactions");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{rawPayload});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getUserId() == null
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should return null when Slack payload is malformed")
        void shouldReturnNullWhenSlackPayloadIsMalformed() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String slackPayload = "not-valid-form-encoded";
            byte[] rawPayload = slackPayload.getBytes();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/integrations/slack/interactions");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{rawPayload});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getUserId() == null
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }
    }

    @Nested
    @DisplayName("User ID Extraction Tests")
    class UserIdExtractionTests {

        @Test
        @DisplayName("Should extract user ID from request with getUserId method")
        void shouldExtractUserIdFromRequestWithGetMethod() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String userId = "user123";
            RequestWithGetId requestWithGetId = new RequestWithGetId(userId);

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{requestWithGetId});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getUserId().equals(userId)
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should extract user ID from request with userId field")
        void shouldExtractUserIdFromRequestWithUserIdField() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String userId = "user456";
            RequestWithUserIdField requestWithField = new RequestWithUserIdField(userId);

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{requestWithField});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getUserId().equals(userId)
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should return null when request has no user ID")
        void shouldReturnNullWhenRequestHasNoUserId() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            Object requestWithoutUserId = new Object();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{requestWithoutUserId});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getUserId() == null
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should extract user ID from response")
        void shouldExtractUserIdFromResponse() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();
            String userId = "user789";
            ResponseWithUserId responseWithUserId = new ResponseWithUserId(userId);

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{}); // No user ID in request
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body(responseWithUserId));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getUserId().equals(userId)
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should log error and proceed when audit service throws exception")
        void shouldLogErrorAndProceedWhenAuditServiceThrows() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // Mock audit service to throw exception
                doThrow(new RuntimeException("Database error")).when(auditService).saveAuditLog(any());

                // When
                Object result = auditAspect.auditAround(joinPoint);

                // Then - Should not affect response
                assertThat(result).isNotNull();
                // Verify request still processed successfully despite audit failure

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should get request ID from MDC")
        void shouldGetRequestIdFromMdc() throws Throwable {
            // Given
            String requestId = UUID.randomUUID().toString();

            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(requestId);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getRequestId().equals(requestId)
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }

        @Test
        @DisplayName("Should return null when MDC is empty")
        void shouldReturnNullWhenMdcIsEmpty() throws Throwable {
            // Given
            MockedStatic<RequestContextHolder> requestContextHolder = mockStatic(RequestContextHolder.class);
            MockedStatic<MDC> mdc = mockStatic(MDC.class);

            try {
                requestContextHolder.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(new ServletRequestAttributes(request));
                when(request.getRequestURI()).thenReturn("/api/leaves/ingest");
                when(request.getMethod()).thenReturn("POST");
                mdc.when(() -> MDC.get("requestId")).thenReturn(null);

                when(joinPoint.getArgs()).thenReturn(new Object[]{});
                when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().body("Success"));

                // When
                auditAspect.auditAround(joinPoint);

                // Then
                verify(auditService).saveAuditLog(argThat(log ->
                        log.getRequestId() == null
                ));

            } finally {
                requestContextHolder.close();
                mdc.close();
            }
        }
    }

    // Test DTOs and helper classes
    private static class TestRequestDto {
        private final String userId;

        public TestRequestDto(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }
    }

    private static class RequestWithGetId {
        private final String userId;

        public RequestWithGetId(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }
    }

    private static class RequestWithUserIdField {
        @SuppressWarnings("unused")
        private final String userId;

        public RequestWithUserIdField(String userId) {
            this.userId = userId;
        }
    }

    private static class ResponseWithUserId {
        @SuppressWarnings("unused")
        private final String userId;

        public ResponseWithUserId(String userId) {
            this.userId = userId;
        }
    }
}
