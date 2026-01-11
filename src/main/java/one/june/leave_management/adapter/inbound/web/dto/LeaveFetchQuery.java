package one.june.leave_management.adapter.inbound.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
     * Optional user name to filter leaves by user.
     * Searches for employees by name or slack display name (partial match, case-insensitive).
     */
    private String userName;

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
        return userName != null || startDate != null || endDate != null;
    }

    /**
     * Validates that startDate and endDate are provided together and that startDate is not after endDate.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if ((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
            throw new IllegalArgumentException("Both startDate and endDate must be provided together");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaveFetchQuery that = (LeaveFetchQuery) o;
        return Objects.equals(userName, that.userName) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, startDate, endDate);
    }
}
