package one.june.leave_management.domain.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing an audit log entry.
 * Contains comprehensive information about API requests and responses
 * for audit trail purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    /**
     * Unique identifier for this audit log entry
     */
    private UUID id;

    /**
     * Correlation ID from X-Request-Id header for request tracing
     */
    private String requestId;

    /**
     * The API endpoint path (e.g., /api/leaves/ingest)
     */
    private String endpoint;

    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    private String httpMethod;

    /**
     * Source system (WEB or SLACK) derived from endpoint
     */
    private String sourceType;

    /**
     * Full request payload captured as JSON
     * Stored as Object to allow flexible conversion to JSON string
     */
    private Object requestBody;

    /**
     * HTTP response status code
     */
    private Integer responseStatus;

    /**
     * Full response payload captured as JSON
     * Stored as Object to allow flexible conversion to JSON string
     */
    private Object responseBody;

    /**
     * User identifier extracted from request
     */
    private String userId;

    /**
     * Request processing duration in milliseconds
     */
    private Long executionTimeMs;

    /**
     * Error details if request failed
     */
    private String errorMessage;

    /**
     * When the request was processed
     */
    private LocalDateTime timestamp;
}
