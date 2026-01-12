package one.june.leave_management.common.exception;

import lombok.Getter;

import java.time.LocalDate;

/**
 * Exception thrown when a leave request falls entirely on weekend days (Saturday/Sunday).
 */
@Getter
public class WeekendOnlyLeaveException extends RuntimeException {

    private final LocalDate startDate;
    private final LocalDate endDate;

    public WeekendOnlyLeaveException(LocalDate startDate, LocalDate endDate) {
        super(String.format("Leave request from %s to %s includes only weekend days. Please submit leave for weekdays only.",
                startDate, endDate));
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public WeekendOnlyLeaveException(String message, LocalDate startDate, LocalDate endDate) {
        super(message);
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
