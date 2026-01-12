package one.june.leave_management.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BusinessDayUtilTest {

    @Test
    @DisplayName("isWeekend should return true for Saturday")
    void isWeekend_ShouldReturnTrue_ForSaturday() {
        LocalDate saturday = LocalDate.of(2024, 1, 6); // January 6, 2024 is a Saturday
        assertTrue(BusinessDayUtil.isWeekend(saturday));
    }

    @Test
    @DisplayName("isWeekend should return true for Sunday")
    void isWeekend_ShouldReturnTrue_ForSunday() {
        LocalDate sunday = LocalDate.of(2024, 1, 7); // January 7, 2024 is a Sunday
        assertTrue(BusinessDayUtil.isWeekend(sunday));
    }

    @Test
    @DisplayName("isWeekend should return false for Monday")
    void isWeekend_ShouldReturnFalse_ForMonday() {
        LocalDate monday = LocalDate.of(2024, 1, 1); // January 1, 2024 is a Monday
        assertFalse(BusinessDayUtil.isWeekend(monday));
    }

    @Test
    @DisplayName("isWeekend should return false for Tuesday through Friday")
    void isWeekend_ShouldReturnFalse_ForWeekdays() {
        LocalDate tuesday = LocalDate.of(2024, 1, 2);
        LocalDate wednesday = LocalDate.of(2024, 1, 3);
        LocalDate thursday = LocalDate.of(2024, 1, 4);
        LocalDate friday = LocalDate.of(2024, 1, 5);

        assertFalse(BusinessDayUtil.isWeekend(tuesday));
        assertFalse(BusinessDayUtil.isWeekend(wednesday));
        assertFalse(BusinessDayUtil.isWeekend(thursday));
        assertFalse(BusinessDayUtil.isWeekend(friday));
    }

    @Test
    @DisplayName("isWeekend should return false for null date")
    void isWeekend_ShouldReturnFalse_ForNullDate() {
        assertFalse(BusinessDayUtil.isWeekend(null));
    }

    @Test
    @DisplayName("countBusinessDays should count correctly for weekday range")
    void countBusinessDays_ShouldCountCorrectly_ForWeekdayRange() {
        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDate friday = LocalDate.of(2024, 1, 5);

        long businessDays = BusinessDayUtil.countBusinessDays(monday, friday);
        assertEquals(5, businessDays);
    }

    @Test
    @DisplayName("countBusinessDays should exclude weekends")
    void countBusinessDays_ShouldExcludeWeekends() {
        LocalDate friday = LocalDate.of(2024, 1, 5);
        LocalDate monday = LocalDate.of(2024, 1, 8);

        long businessDays = BusinessDayUtil.countBusinessDays(friday, monday);
        assertEquals(2, businessDays); // Friday and Monday only
    }

    @Test
    @DisplayName("countBusinessDays should return 0 for weekend-only range")
    void countBusinessDays_ShouldReturnZero_ForWeekendOnlyRange() {
        LocalDate saturday = LocalDate.of(2024, 1, 6);
        LocalDate sunday = LocalDate.of(2024, 1, 7);

        long businessDays = BusinessDayUtil.countBusinessDays(saturday, sunday);
        assertEquals(0, businessDays);
    }

    @Test
    @DisplayName("countBusinessDays should handle single day")
    void countBusinessDays_ShouldHandleSingleDay() {
        LocalDate wednesday = LocalDate.of(2024, 1, 3);

        long businessDays = BusinessDayUtil.countBusinessDays(wednesday, wednesday);
        assertEquals(1, businessDays);
    }

    @Test
    @DisplayName("countBusinessDays should return 0 for single weekend day")
    void countBusinessDays_ShouldReturnZero_ForSingleWeekendDay() {
        LocalDate saturday = LocalDate.of(2024, 1, 6);

        long businessDays = BusinessDayUtil.countBusinessDays(saturday, saturday);
        assertEquals(0, businessDays);
    }

    @Test
    @DisplayName("countBusinessDays should handle range spanning multiple weeks")
    void countBusinessDays_ShouldHandleMultipleWeeks() {
        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDate sunday = LocalDate.of(2024, 1, 14); // 2 weeks

        long businessDays = BusinessDayUtil.countBusinessDays(monday, sunday);
        assertEquals(10, businessDays); // 5 days per week × 2 weeks
    }

    @Test
    @DisplayName("countBusinessDays should return 0 for null dates")
    void countBusinessDays_ShouldReturnZero_ForNullDates() {
        assertEquals(0, BusinessDayUtil.countBusinessDays(null, LocalDate.now()));
        assertEquals(0, BusinessDayUtil.countBusinessDays(LocalDate.now(), null));
        assertEquals(0, BusinessDayUtil.countBusinessDays(null, null));
    }

    @Test
    @DisplayName("isWeekendOnly should return true for Saturday to Sunday")
    void isWeekendOnly_ShouldReturnTrue_ForSaturdayToSunday() {
        LocalDate saturday = LocalDate.of(2024, 1, 6);
        LocalDate sunday = LocalDate.of(2024, 1, 7);

        assertTrue(BusinessDayUtil.isWeekendOnly(saturday, sunday));
    }

    @Test
    @DisplayName("isWeekendOnly should return false for weekday range")
    void isWeekendOnly_ShouldReturnFalse_ForWeekdayRange() {
        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDate friday = LocalDate.of(2024, 1, 5);

        assertFalse(BusinessDayUtil.isWeekendOnly(monday, friday));
    }

    @Test
    @DisplayName("isWeekendOnly should return false for mixed range")
    void isWeekendOnly_ShouldReturnFalse_ForMixedRange() {
        LocalDate friday = LocalDate.of(2024, 1, 5);
        LocalDate monday = LocalDate.of(2024, 1, 8);

        assertFalse(BusinessDayUtil.isWeekendOnly(friday, monday));
    }

    @Test
    @DisplayName("isWeekendOnly should return false for single weekday")
    void isWeekendOnly_ShouldReturnFalse_ForSingleWeekday() {
        LocalDate wednesday = LocalDate.of(2024, 1, 3);

        assertFalse(BusinessDayUtil.isWeekendOnly(wednesday, wednesday));
    }

    @Test
    @DisplayName("isWeekendOnly should return true for single weekend day")
    void isWeekendOnly_ShouldReturnTrue_ForSingleWeekendDay() {
        LocalDate saturday = LocalDate.of(2024, 1, 6);

        assertTrue(BusinessDayUtil.isWeekendOnly(saturday, saturday));
    }

    @Test
    @DisplayName("isWeekendOnly should return false for null dates")
    void isWeekendOnly_ShouldReturnFalse_ForNullDates() {
        assertFalse(BusinessDayUtil.isWeekendOnly(null, LocalDate.now()));
        assertFalse(BusinessDayUtil.isWeekendOnly(LocalDate.now(), null));
        assertFalse(BusinessDayUtil.isWeekendOnly(null, null));
    }
}
