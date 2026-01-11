package one.june.leave_management.domain.leave.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
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
     * Optional list of user IDs to filter leaves by users.
     * When provided, only leaves for these user IDs will be returned.
     */
    private List<String> userIds;

    /**
     * Optional start date to filter leaves by.
     * If provided, endDate must also be provided.
     * Leaves will be filtered if any part of their date range falls within the specified date range.
     */
    private LocalDate startDate;

    /**
     * Optional end date to filter leaves by.
     * If provided, startDate must also be provided.
     * Leaves will be filtered if any part of their date range falls within the specified date range.
     */
    private LocalDate endDate;

    /**
     * Checks if any filter is set.
     *
     * @return true if at least one filter parameter is provided, false otherwise
     */
    public boolean hasFilters() {
        return (userIds != null && !userIds.isEmpty()) || startDate != null || endDate != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaveFilters that = (LeaveFilters) o;
        return Objects.equals(userIds, that.userIds) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userIds, startDate, endDate);
    }
}
