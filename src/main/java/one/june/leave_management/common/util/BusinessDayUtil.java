package one.june.leave_management.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Utility class for business day calculations.
 * Weekends are hardcoded to Saturday and Sunday.
 */
public final class BusinessDayUtil {

    private BusinessDayUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if a given date is a weekend (Saturday or Sunday).
     *
     * @param date the date to check
     * @return true if the date is a weekend, false otherwise
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Counts the number of business days (Monday to Friday) between two dates, inclusive.
     *
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return the number of business days
     */
    public static long countBusinessDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }

        long days = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (!isWeekend(currentDate)) {
                days++;
            }
            currentDate = currentDate.plusDays(1);
        }

        return days;
    }

    /**
     * Checks if all dates in the given range are weekends.
     *
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return true if all dates in the range are weekends, false otherwise
     */
    public static boolean isWeekendOnly(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (!isWeekend(currentDate)) {
                return false;
            }
            currentDate = currentDate.plusDays(1);
        }

        return true;
    }
}
