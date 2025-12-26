package one.june.leave_management.application.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.persistence.jpa.entity.AuditLogJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.AuditLogJpaRepository;
import one.june.leave_management.domain.audit.model.AuditLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Application service for audit log operations.
 * Handles saving audit logs to the database synchronously.
 */
@Service
@Slf4j
public class AuditService {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogJpaRepository auditLogJpaRepository) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        // Configure ObjectMapper to disable REQUIRE_HANDLERS_FOR_JAVA8_TIMES
        // This allows serialization of objects with LocalDate fields without JavaTimeModule
        this.objectMapper = new ObjectMapper()
                .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES);
    }

    /**
     * Save an audit log entry to the database.
     *
     * @param auditLog the audit log domain model
     */
    @Transactional
    public void saveAuditLog(AuditLog auditLog) {
        try {
            AuditLogJpaEntity entity = toJpaEntity(auditLog);
            auditLogJpaRepository.save(entity);
            log.debug("Saved audit log for request_id: {}, endpoint: {}, status: {}",
                    auditLog.getRequestId(), auditLog.getEndpoint(), auditLog.getResponseStatus());
        } catch (Exception e) {
            // We don't want to fail the actual request if auditing fails
            // Log the error but don't throw
            log.error("Failed to save audit log for request_id: {}, endpoint: {}",
                    auditLog.getRequestId(), auditLog.getEndpoint(), e);
        }
    }

    /**
     * Convert domain model to JPA entity.
     *
     * @param auditLog the domain model
     * @return JPA entity
     */
    private AuditLogJpaEntity toJpaEntity(AuditLog auditLog) {
        return AuditLogJpaEntity.builder()
                .id(auditLog.getId())
                .requestId(auditLog.getRequestId())
                .endpoint(auditLog.getEndpoint())
                .httpMethod(auditLog.getHttpMethod())
                .sourceType(auditLog.getSourceType())
                .requestBody(safeToJson(auditLog.getRequestBody()))
                .responseStatus(auditLog.getResponseStatus())
                .responseBody(safeToJson(auditLog.getResponseBody()))
                .userId(auditLog.getUserId())
                .executionTimeMs(auditLog.getExecutionTimeMs())
                .errorMessage(auditLog.getErrorMessage())
                .timestamp(auditLog.getTimestamp() != null ? auditLog.getTimestamp() : LocalDateTime.now())
                .build();
    }

    /**
     * Safely convert object to JSON string.
     * Returns null if conversion fails.
     *
     * @param obj the object to convert
     * @return JSON string or null
     */
    private String safeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
}
