package one.june.leave_management.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import one.june.leave_management.common.validation.ValidDateRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Generic DateRange entity that can be reused across different contexts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidDateRange
@Schema(description = "Date range with start and end dates (inclusive)")
public class DateRange {

    @Schema(
            description = "Start date of the leave period (inclusive)",
            example = "2024-01-15",
            required = true
    )
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Schema(
            description = "End date of the leave period (inclusive)",
            example = "2024-01-20",
            required = true
    )
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /**
     * Returns the duration of this date range in days
     */
    public long toDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Checks if a given date falls within this date range (inclusive)
     */
    public boolean contains(LocalDate date) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Checks if this date range overlaps with another date range
     */
    public boolean overlapsWith(DateRange other) {
        if (other == null || other.getStartDate() == null || other.getEndDate() == null ||
            this.startDate == null || this.endDate == null) {
            return false;
        }

        return !this.endDate.isBefore(other.getStartDate()) && !this.startDate.isAfter(other.getEndDate());
    }
}