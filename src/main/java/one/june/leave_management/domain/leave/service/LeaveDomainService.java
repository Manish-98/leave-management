package one.june.leave_management.domain.leave.service;

import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveDomainService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveDomainService.class);

    private final LeaveRepository leaveRepository;

    public LeaveDomainService(LeaveRepository leaveRepository) {
        this.leaveRepository = leaveRepository;
    }

    public void validateLeaveForPersistence(Leave leave) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave cannot be null");
        }

        if (leave.getId() == null && !leave.hasSourceRefs()) {
            throw new IllegalArgumentException("New leaves must have at least one source reference");
        }

        if (leave.getStatus() == null) {
            throw new IllegalArgumentException("Leave status cannot be null");
        }

        if (leave.getType() == null) {
            throw new IllegalArgumentException("Leave type cannot be null");
        }

        // Additional business validations can be added here
        if (leave.getStatus() == LeaveStatus.APPROVED && leave.getStartDate().isAfter(leave.getEndDate())) {
            throw new IllegalArgumentException("Approved leaves must be at least 1 day long");
        }
    }

    /**
     * Validates that a leave does not overlap with existing leaves for the same user.
     *
     * @param leave the leave to validate for overlaps
     * @throws OverlappingLeaveException if the leave overlaps with existing leaves
     */
    public void validateNoOverlappingLeaves(Leave leave) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave cannot be null");
        }

        logger.debug("Checking for overlapping leaves for user {} with date range {}",
                    leave.getUserId(), leave.getDateRange());

        // Find overlapping leaves, excluding the current leave if it's an update
        List<Leave> overlappingLeaves;
        if (leave.getId() != null) {
            overlappingLeaves = leaveRepository.findOverlappingLeaves(
                    leave.getUserId(), leave.getDateRange(), leave.getId());
        } else {
            overlappingLeaves = leaveRepository.findOverlappingLeaves(
                    leave.getUserId(), leave.getDateRange());
        }

        if (!overlappingLeaves.isEmpty()) {
            Leave existingLeave = overlappingLeaves.get(0); // Take the first overlapping leave
            throw new OverlappingLeaveException(
                    leave.getUserId(),
                    leave.getStartDate(),
                    leave.getEndDate(),
                    existingLeave.getId()
            );
        }

        logger.debug("No overlapping leaves found for user {} with date range {}",
                    leave.getUserId(), leave.getDateRange());
    }
}