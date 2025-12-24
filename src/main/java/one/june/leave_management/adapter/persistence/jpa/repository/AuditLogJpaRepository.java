package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.adapter.persistence.jpa.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AuditLogJpaEntity.
 * Provides database access operations for audit log entries.
 */
@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    /**
     * Find all audit logs for a specific request ID (correlation ID).
     *
     * @param requestId the correlation ID
     * @return list of audit logs with the given request ID
     */
    List<AuditLogJpaEntity> findByRequestId(String requestId);

    /**
     * Find all audit logs for a specific user ID.
     *
     * @param userId the user identifier
     * @return list of audit logs for the given user
     */
    List<AuditLogJpaEntity> findByUserId(String userId);

    /**
     * Find all audit logs for a specific source type (WEB or SLACK).
     *
     * @param sourceType the source system (WEB or SLACK)
     * @return list of audit logs from the given source
     */
    List<AuditLogJpaEntity> findBySourceType(String sourceType);

    /**
     * Find all audit logs within a time range.
     *
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return list of audit logs within the time range
     */
    List<AuditLogJpaEntity> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find all audit logs with error messages (failed requests).
     *
     * @return list of audit logs where error message is not null
     */
    List<AuditLogJpaEntity> findByErrorMessageIsNotNull();
}
