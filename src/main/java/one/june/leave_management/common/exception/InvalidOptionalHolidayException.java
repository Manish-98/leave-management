package one.june.leave_management.common.exception;

import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Exception thrown when an optional holiday leave request is invalid.
 * This can happen when:
 * - The requested date is not found in the optional_holidays table
 * - The leave request spans multiple days (optional holidays must be single-day)
 */
@Getter
public class InvalidOptionalHolidayException extends RuntimeException {

    private final String userId;
    private final LocalDate requestedDate;
    private final UUID leaveId;

    public InvalidOptionalHolidayException(String userId, LocalDate requestedDate, UUID leaveId, String reason) {
        super(String.format("Invalid optional holiday request for user %s on date %s: %s (Leave ID: %s)",
                userId, requestedDate, reason, leaveId));
        this.userId = userId;
        this.requestedDate = requestedDate;
        this.leaveId = leaveId;
    }

    /**
     * Creates an exception for multi-day optional holiday requests.
     */
    public static InvalidOptionalHolidayException multiDayNotAllowed(String userId, LocalDate startDate, LocalDate endDate, UUID leaveId) {
        return new InvalidOptionalHolidayException(
                userId,
                startDate,
                leaveId,
                String.format("Optional holidays must be single-day only. Requested range: %s to %s", startDate, endDate)
        );
    }

    /**
     * Creates an exception when the requested date is not found in the optional_holidays table.
     */
    public static InvalidOptionalHolidayException dateNotFound(String userId, LocalDate requestedDate, UUID leaveId) {
        return new InvalidOptionalHolidayException(
                userId,
                requestedDate,
                leaveId,
                String.format("Date %s is not a valid optional holiday. Please add it to the optional holidays table first.", requestedDate)
        );
    }
}
