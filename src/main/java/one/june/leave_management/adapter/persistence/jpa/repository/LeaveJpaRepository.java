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
     * Excludes DEACTIVATED leaves from validation.
     */
    @Query("SELECT l FROM LeaveJpaEntity l WHERE l.userId = :userId " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate " +
           "AND l.status != 'DEACTIVATED'")
    List<LeaveJpaEntity> findOverlappingLeaves(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find leaves that overlap with the given date range for a specific user, excluding a specific leave ID
     * Uses date range overlap logic: (start1 <= end2) AND (end1 >= start2)
     * Excludes DEACTIVATED leaves from validation.
     */
    @Query("SELECT l FROM LeaveJpaEntity l WHERE l.userId = :userId " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate " +
           "AND l.id != :excludeLeaveId " +
           "AND l.status != 'DEACTIVATED'")
    List<LeaveJpaEntity> findOverlappingLeaves(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeLeaveId") UUID excludeLeaveId);

    /**
     * Find leaves by filters with pagination support.
     * All filters are optional - null values will be ignored.
     * Uses date range overlap logic to find leaves that intersect with the specified date range.
     * Excludes DEACTIVATED leaves from results.
     * Default sorting is applied at the service layer (startDate DESC).
     * Clients can override the default sort order using Pageable sort parameters.
     *
     * @param userIds optional list of user IDs to filter by
     * @param startDate optional start date (must be provided with endDate)
     * @param endDate optional end date (must be provided with startDate)
     * @param pageable pagination and sorting parameters
     * @return page of leaves matching the filter criteria
     */
    @Query("SELECT l FROM LeaveJpaEntity l " +
           "LEFT JOIN FETCH l.sourceRefs WHERE " +
           "(:userIds IS NULL OR l.userId IN :userIds) AND " +
           "(:startDate IS NULL OR l.endDate >= :startDate) AND " +
           "(:endDate IS NULL OR l.startDate <= :endDate) AND " +
           "l.status != 'DEACTIVATED'")
    Page<LeaveJpaEntity> findByFilters(
            @Param("userIds") List<String> userIds,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            Pageable pageable);

    /**
     * Find leaves by filters with pagination support - date range variant.
     * This method is used when startDate and endDate are both non-null.
     * Using function expressions to provide type hints for PostgreSQL JDBC driver.
     * Excludes DEACTIVATED leaves from results.
     *
     * @param userIds optional list of user IDs to filter by
     * @param startDate start date (non-null)
     * @param endDate end date (non-null)
     * @param pageable pagination and sorting parameters
     * @return page of leaves matching the filter criteria
     */
    @Query("SELECT l FROM LeaveJpaEntity l " +
           "LEFT JOIN FETCH l.sourceRefs WHERE " +
           "(:userIds IS NULL OR l.userId IN :userIds) AND " +
           "l.endDate >= :startDate AND l.startDate <= :endDate AND " +
           "l.status != 'DEACTIVATED'")
    Page<LeaveJpaEntity> findByFiltersWithDateRange(
            @Param("userIds") List<String> userIds,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            Pageable pageable);

    /**
     * Count approved optional holiday leaves for a specific user in a given year.
     * Uses date range overlap logic to count leaves that fall within the given year.
     */
    @Query("SELECT COUNT(l) FROM LeaveJpaEntity l WHERE l.userId = :userId " +
           "AND l.type = 'OPTIONAL_HOLIDAY' " +
           "AND l.status = 'APPROVED' " +
           "AND l.startDate <= :yearEnd AND l.endDate >= :yearStart")
    long countApprovedOptionalHolidaysByUserAndYear(
            @Param("userId") String userId,
            @Param("yearStart") java.time.LocalDate yearStart,
            @Param("yearEnd") java.time.LocalDate yearEnd);

    /**
     * Find all active (non-deactivated) leaves for a specific user.
     * Ordered by start date descending (most recent first).
     */
    @Query("SELECT l FROM LeaveJpaEntity l WHERE l.userId = :userId " +
           "AND l.status != 'DEACTIVATED' " +
           "ORDER BY l.startDate DESC")
    List<LeaveJpaEntity> findActiveLeavesByUserId(@Param("userId") String userId);
}