package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.adapter.persistence.jpa.entity.LeaveJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Find leaves by filters with pagination support.
     * All filters are optional - null values will be ignored.
     *
     * Uses date range comparisons for year filtering to ensure compatibility with H2 database.
     *
     * @param userId optional user ID to filter by
     * @param year optional year to filter by (leaves overlapping with the year)
     * @param startMonth optional start month for quarter filtering (1-12)
     * @param endMonth optional end month for quarter filtering (1-12)
     * @param pageable pagination and sorting parameters
     * @return page of leaves matching the filter criteria
     */
    @Query("SELECT l FROM LeaveJpaEntity l WHERE " +
           "(:userId IS NULL OR l.userId = :userId) AND " +
           "(:year IS NULL OR " +
           "  (l.startDate <= :yearEnd AND l.endDate >= :yearStart)) AND " +
           "(:startMonth IS NULL OR :endMonth IS NULL OR " +
           "  (l.startDate <= :quarterEnd AND l.endDate >= :quarterStart))")
    Page<LeaveJpaEntity> findByFilters(
            @Param("userId") String userId,
            @Param("year") Integer year,
            @Param("yearStart") java.time.LocalDate yearStart,
            @Param("yearEnd") java.time.LocalDate yearEnd,
            @Param("startMonth") Integer startMonth,
            @Param("endMonth") Integer endMonth,
            @Param("quarterStart") java.time.LocalDate quarterStart,
            @Param("quarterEnd") java.time.LocalDate quarterEnd,
            Pageable pageable);
}