package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.adapter.persistence.jpa.entity.LeaveJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveJpaRepository extends JpaRepository<LeaveJpaEntity, UUID> {
    List<LeaveJpaEntity> findByUserId(String userId);

    /**
     * Find leaves that overlap with the given date range for a specific user
     * Uses date range overlap logic: (start1 <= end2) AND (end1 >= start2)
     */
    @Query("SELECT l FROM LeaveJpaEntity l WHERE l.userId = :userId " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<LeaveJpaEntity> findOverlappingLeaves(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find leaves that overlap with the given date range for a specific user, excluding a specific leave ID
     * Uses date range overlap logic: (start1 <= end2) AND (end1 >= start2)
     */
    @Query("SELECT l FROM LeaveJpaEntity l WHERE l.userId = :userId " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate " +
           "AND l.id != :excludeLeaveId")
    List<LeaveJpaEntity> findOverlappingLeaves(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeLeaveId") UUID excludeLeaveId);
}