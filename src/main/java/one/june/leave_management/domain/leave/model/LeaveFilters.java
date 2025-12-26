package one.june.leave_management.domain.leave.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Value object for filtering leaves in the domain layer.
 * Used by repository ports to pass filter criteria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveFilters {

    /**
     * Optional user ID to filter leaves by user.
     */
    private String userId;

    /**
     * Optional year to filter leaves by.
     */
    private Integer year;

    /**
     * Optional start month for quarter filtering (1-12).
     * If provided, endMonth must also be provided.
     */
    private Integer startMonth;

    /**
     * Optional end month for quarter filtering (1-12).
     * If provided, startMonth must also be provided.
     */
    private Integer endMonth;

    /**
     * Checks if any filter is set.
     *
     * @return true if at least one filter parameter is provided, false otherwise
     */
    public boolean hasFilters() {
        return userId != null || year != null || (startMonth != null && endMonth != null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaveFilters that = (LeaveFilters) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(year, that.year) &&
                Objects.equals(startMonth, that.startMonth) &&
                Objects.equals(endMonth, that.endMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, year, startMonth, endMonth);
    }
}
