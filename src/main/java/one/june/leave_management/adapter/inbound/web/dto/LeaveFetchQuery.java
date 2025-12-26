package one.june.leave_management.adapter.inbound.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.june.leave_management.common.model.Quarter;

import java.util.Objects;

/**
 * DTO for fetching leaves with optional filters.
 * All fields are optional to allow flexible querying.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveFetchQuery {

    /**
     * Optional user ID to filter leaves by user.
     */
    private String userId;

    /**
     * Optional year to filter leaves by.
     * Leaves will be filtered if any part of their date range falls in the specified year.
     */
    private Integer year;

    /**
     * Optional quarter to filter leaves by.
     * Leaves will be filtered if any part of their date range falls in the specified quarter.
     * Must be used in combination with year parameter.
     */
    private Quarter quarter;

    /**
     * Checks if any filter is set.
     *
     * @return true if at least one filter parameter is provided, false otherwise
     */
    public boolean hasFilters() {
        return userId != null || year != null || quarter != null;
    }

    /**
     * Validates that quarter is only used with year.
     *
     * @throws IllegalArgumentException if quarter is provided without year
     */
    public void validate() {
        if (quarter != null && year == null) {
            throw new IllegalArgumentException("Quarter filter requires year parameter to be specified");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaveFetchQuery that = (LeaveFetchQuery) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(year, that.year) &&
                quarter == that.quarter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, year, quarter);
    }
}
