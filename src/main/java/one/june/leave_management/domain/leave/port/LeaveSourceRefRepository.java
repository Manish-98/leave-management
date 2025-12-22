package one.june.leave_management.domain.leave.port;

import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.SourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveSourceRefRepository {
    LeaveSourceRef save(LeaveSourceRef sourceRef);
    Optional<LeaveSourceRef> findById(UUID id);
    Optional<LeaveSourceRef> findBySourceTypeAndSourceId(SourceType sourceType, String sourceId);
    List<LeaveSourceRef> findByLeaveId(UUID leaveId);
    void deleteById(UUID id);
    boolean existsById(UUID id);

    /**
     * Finds a LeaveSourceRef by source type and source ID and returns it with the associated leave ID
     * @param sourceType the source type
     * @param sourceId the source ID
     * @return Optional of LeaveSourceRef with leave ID populated
     */
    Optional<LeaveSourceRef> findBySourceTypeAndSourceIdWithLeave(SourceType sourceType, String sourceId);
}