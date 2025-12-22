package one.june.leave_management.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class RequestIdInterceptor implements HandlerInterceptor {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = generateOrExtractRequestId(request);

        // Add to MDC for logging
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        // Add to response header
        response.setHeader(REQUEST_ID_HEADER, requestId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        // Clean up MDC
        MDC.remove(REQUEST_ID_MDC_KEY);
    }

    private String generateOrExtractRequestId(HttpServletRequest request) {
        // Try to get existing request ID from header
        String requestId = request.getHeader(REQUEST_ID_HEADER);

        // If not present, generate a new one
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = generateRequestId();
        }

        return requestId;
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}