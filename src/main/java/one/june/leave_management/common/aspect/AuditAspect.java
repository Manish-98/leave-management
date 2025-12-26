package one.june.leave_management.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.application.audit.service.AuditService;
import one.june.leave_management.domain.audit.model.AuditLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * AOP Aspect for auditing controller methods annotated with @Auditable.
 * Captures request and response details and stores them in the audit log.
 */
@Aspect
@Component
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Around advice for methods annotated with @Auditable.
     * Captures request details, executes the method, captures response details,
     * and saves everything to the audit log.
     *
     * @param joinPoint the join point representing the method execution
     * @return the result of the method execution
     * @throws Throwable if the method execution throws an exception
     */
    @Around("@annotation(one.june.leave_management.common.annotation.Auditable)")
    public Object auditAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Get HTTP request
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            // No HTTP context, proceed without auditing
            return joinPoint.proceed();
        }

        // Build audit log with request details
        AuditLog.AuditLogBuilder auditLogBuilder = AuditLog.builder()
                .requestId(getRequestId())
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .sourceType(determineSourceType(request.getRequestURI()))
                .timestamp(java.time.LocalDateTime.now());

        // Capture request body as Object (will be converted to JSON by AuditService)
        Object requestBody = captureRequestBody(joinPoint);
        if (requestBody != null) {
            // For Slack endpoints (byte arrays), parse the form-encoded payload to make it readable
            Object readableRequestBody = makeReadable(requestBody, request.getRequestURI());
            auditLogBuilder.requestBody(readableRequestBody);

            // Extract user ID from request body
            String userId = extractUserIdFromRequest(requestBody);
            if (userId != null) {
                auditLogBuilder.userId(userId);
            }
        }

        Object result = null;
        Throwable exception = null;

        try {
            // Execute the actual method
            result = joinPoint.proceed();

            // Capture response details
            auditLogBuilder
                    .responseStatus(extractResponseStatus(result));

            // Capture response body as Object (will be converted to JSON by AuditService)
            if (result != null) {
                // For empty responses (like Slack), store a descriptive message instead of ResponseEntity object
                Object responseBody = makeReadableResponse(result, request.getRequestURI());
                auditLogBuilder.responseBody(responseBody);

                // If user ID wasn't found in request, try to extract from response
                if (auditLogBuilder.build().getUserId() == null) {
                    String userIdFromResponse = extractUserIdFromResponse(result);
                    if (userIdFromResponse != null) {
                        auditLogBuilder.userId(userIdFromResponse);
                    }
                }
            }

            return result;

        } catch (Throwable e) {
            exception = e;
            // Capture error details
            auditLogBuilder
                    .responseStatus(500) // Internal Server Error for exceptions
                    .errorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());

            throw e; // Re-throw the exception

        } finally {
            // Calculate execution time
            long executionTime = System.currentTimeMillis() - startTime;
            auditLogBuilder.executionTimeMs(executionTime);

            // Save audit log (synchronous)
            try {
                auditService.saveAuditLog(auditLogBuilder.build());
            } catch (Exception e) {
                // Don't fail the request if auditing fails
                log.error("Failed to save audit log for endpoint: {}", request.getRequestURI(), e);
            }
        }
    }

    /**
     * Get the current HTTP request from Spring context.
     *
     * @return the HttpServletRequest or null if not in a web context
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Get the request ID from MDC (set by RequestIdInterceptor).
     *
     * @return the request ID or null if not set
     */
    private String getRequestId() {
        return MDC.get("requestId");
    }

    /**
     * Determine the source type (WEB or SLACK) based on the endpoint path.
     *
     * @param endpoint the request URI
     * @return "WEB" or "SLACK"
     */
    private String determineSourceType(String endpoint) {
        if (endpoint != null && endpoint.startsWith("/integrations/slack")) {
            return "SLACK";
        }
        return "WEB";
    }

    /**
     * Capture the request body from method arguments.
     *
     * @param joinPoint the join point
     * @return the request body object or null
     */
    private Object captureRequestBody(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        // Filter out HttpServletRequest, HttpServletResponse, and null objects (from optional @RequestParam)
        return Arrays.stream(args)
                .filter(arg -> arg != null
                        && !(arg instanceof HttpServletRequest)
                        && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                .findFirst()
                .orElse(null);
    }

    /**
     * Extract HTTP status code from response object.
     * Handles ResponseEntity and other response types.
     *
     * @param response the response object
     * @return the HTTP status code or null
     */
    private Integer extractResponseStatus(Object response) {
        if (response instanceof org.springframework.http.ResponseEntity) {
            return ((org.springframework.http.ResponseEntity<?>) response).getStatusCode().value();
        }
        return null;
    }

    /**
     * Make response body more readable by handling empty responses.
     *
     * @param response the response object
     * @param endpoint the request endpoint
     * @return readable response body
     */
    private Object makeReadableResponse(Object response, String endpoint) {
        // For Slack endpoints with empty responses, return a descriptive message
        if (endpoint.startsWith("/integrations/slack") &&
            response instanceof org.springframework.http.ResponseEntity) {
            org.springframework.http.ResponseEntity<?> resp =
                (org.springframework.http.ResponseEntity<?>) response;
            if (resp.getBody() == null) {
                return "{\"message\": \"Empty response (as required by Slack API)\"}";
            }
        }
        return response;
    }

    /**
     * Make request body more readable by parsing form-encoded Slack payloads.
     *
     * @param requestBody the raw request body
     * @param endpoint the request endpoint
     * @return readable request body (parsed JSON for Slack, original for others)
     */
    private Object makeReadable(Object requestBody, String endpoint) {
        // Only process Slack endpoints with byte array bodies
        if (!endpoint.startsWith("/integrations/slack") || !(requestBody instanceof byte[])) {
            return requestBody;
        }

        try {
            byte[] rawBody = (byte[]) requestBody;
            String bodyString = new String(rawBody, StandardCharsets.UTF_8);

            // Parse form-encoded payload (format: payload={json})
            String[] pairs = bodyString.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "payload".equals(kv[0])) {
                    // Return the decoded JSON instead of raw form data
                    return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse Slack payload, storing raw body", e);
        }

        // If parsing fails, return the original body
        return requestBody;
    }

    /**
     * Extract user ID from response object.
     * Looks for userId field using reflection.
     *
     * @param response the response object
     * @return the user ID or null
     */
    private String extractUserIdFromResponse(Object response) {
        if (response == null) {
            return null;
        }

        // Try to get userId from response body using reflection
        try {
            java.lang.reflect.Field userIdField = response.getClass().getDeclaredField("userId");
            userIdField.setAccessible(true);
            return (String) userIdField.get(response);
        } catch (Exception e) {
            // Field not found or inaccessible
            return null;
        }
    }

    /**
     * Extract user ID from request object.
     * Handles both regular POJOs and Slack form-encoded payloads.
     *
     * @param request the request object
     * @return the user ID or null
     */
    private String extractUserIdFromRequest(Object request) {
        if (request == null) {
            return null;
        }

        // For byte arrays (Slack form-encoded payloads), extract from JSON
        if (request instanceof byte[]) {
            return extractUserIdFromSlackPayload((byte[]) request);
        }

        // For regular POJOs, try to call getUserId() method
        try {
            java.lang.reflect.Method getUserIdMethod = request.getClass().getMethod("getUserId");
            Object userId = getUserIdMethod.invoke(request);
            if (userId instanceof String) {
                return (String) userId;
            }
        } catch (Exception e) {
            // Method not found or invocation failed, try field access
        }

        // If method doesn't work, try to access userId field directly
        try {
            java.lang.reflect.Field userIdField = request.getClass().getDeclaredField("userId");
            userIdField.setAccessible(true);
            Object userId = userIdField.get(request);
            if (userId instanceof String) {
                return (String) userId;
            }
        } catch (Exception e) {
            // Field not found or inaccessible
            return null;
        }

        return null;
    }

    /**
     * Extract user ID from Slack form-encoded payload.
     * Parses the payload parameter and extracts user.id from JSON.
     *
     * @param rawBody the raw request body bytes
     * @return the user ID or null
     */
    private String extractUserIdFromSlackPayload(byte[] rawBody) {
        try {
            String requestBody = new String(rawBody, StandardCharsets.UTF_8);

            // Parse form-encoded payload (format: payload={json})
            String[] pairs = requestBody.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "payload".equals(kv[0])) {
                    String payloadJson = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);

                    // Extract user_id from JSON using simple string parsing
                    // Looking for "user":{"id":"U056RHT..."} pattern
                    int userIndex = payloadJson.indexOf("\"user\"");
                    if (userIndex != -1) {
                        int idStart = payloadJson.indexOf("\"id\"", userIndex);
                        if (idStart != -1) {
                            idStart = payloadJson.indexOf("\"", idStart + 5);
                            if (idStart != -1) {
                                int idEnd = payloadJson.indexOf("\"", idStart + 1);
                                if (idEnd != -1) {
                                    return payloadJson.substring(idStart + 1, idEnd);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract user ID from Slack payload", e);
        }
        return null;
    }
}
