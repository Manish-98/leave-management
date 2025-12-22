package one.june.leave_management.domain.leave.port;

import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveRepository {
    Leave save(Leave leave);
    Optional<Leave> findById(UUID id);
    List<Leave> findByUserId(String userId);
    void deleteById(UUID id);
    boolean existsById(UUID id);

    /**
     * Find leaves that overlap with the given date range for a specific user
     * @param userId the user ID
     * @param dateRange the date range to check for overlaps
     * @return list of leaves that overlap with the given date range
     */
    List<Leave> findOverlappingLeaves(String userId, DateRange dateRange);

    /**
     * Find leaves that overlap with the given date range for a specific user, excluding a specific leave ID
     * @param userId the user ID
     * @param dateRange the date range to check for overlaps
     * @param excludeLeaveId the leave ID to exclude from the check
     * @return list of leaves that overlap with the given date range
     */
    List<Leave> findOverlappingLeaves(String userId, DateRange dateRange, UUID excludeLeaveId);
}