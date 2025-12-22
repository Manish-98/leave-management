package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.adapter.persistence.jpa.entity.LeaveSourceRefJpaEntity;
import one.june.leave_management.domain.leave.model.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveSourceRefJpaRepository extends JpaRepository<LeaveSourceRefJpaEntity, UUID> {

    @Query("SELECT lsr FROM LeaveSourceRefJpaEntity lsr WHERE lsr.sourceType = :sourceType AND lsr.sourceId = :sourceId")
    Optional<LeaveSourceRefJpaEntity> findBySourceTypeAndSourceId(@Param("sourceType") SourceType sourceType, @Param("sourceId") String sourceId);

    List<LeaveSourceRefJpaEntity> findByLeaveId(UUID leaveId);

    @Query("SELECT lsr FROM LeaveSourceRefJpaEntity lsr WHERE lsr.sourceType = :sourceType AND lsr.sourceId = :sourceId")
    Optional<LeaveSourceRefJpaEntity> findBySourceTypeAndSourceIdWithLeave(@Param("sourceType") SourceType sourceType, @Param("sourceId") String sourceId);
}