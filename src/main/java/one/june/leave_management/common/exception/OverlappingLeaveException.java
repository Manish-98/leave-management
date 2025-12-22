package one.june.leave_management.common.exception;

import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Exception thrown when a leave request overlaps with an existing leave for the same user.
 */
@Getter
public class OverlappingLeaveException extends RuntimeException {

    private final String userId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final UUID existingLeaveId;

    public OverlappingLeaveException(String userId, LocalDate startDate, LocalDate endDate, UUID existingLeaveId) {
        super(String.format("User %s already has a leave from %s to %s that overlaps with the requested leave (ID: %s)",
                userId, startDate, endDate, existingLeaveId));
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.existingLeaveId = existingLeaveId;
    }

    public OverlappingLeaveException(String message, String userId, LocalDate startDate, LocalDate endDate, UUID existingLeaveId) {
        super(message);
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.existingLeaveId = existingLeaveId;
    }

}