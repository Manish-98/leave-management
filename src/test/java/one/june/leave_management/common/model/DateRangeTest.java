package one.june.leave_management.common.model;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Test
    void validDateRangeShouldPassValidation() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        Set<ConstraintViolation<DateRange>> violations = validator.validate(dateRange);
        assertTrue(violations.isEmpty(), "Valid date range should not have violations");
    }

    @Test
    void nullStartDateShouldFailValidation() {
        DateRange dateRange = DateRange.builder()
                .startDate(null)
                .endDate(LocalDate.now().plusDays(3))
                .build();

        Set<ConstraintViolation<DateRange>> violations = validator.validate(dateRange);
        assertEquals(1, violations.size());
        ConstraintViolation<DateRange> violation = violations.iterator().next();
        assertEquals("startDate", violation.getPropertyPath().toString());
        assertEquals("Start date is required", violation.getMessage());
    }

    @Test
    void nullEndDateShouldFailValidation() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(null)
                .build();

        Set<ConstraintViolation<DateRange>> violations = validator.validate(dateRange);
        assertEquals(1, violations.size());
        ConstraintViolation<DateRange> violation = violations.iterator().next();
        assertEquals("endDate", violation.getPropertyPath().toString());
        assertEquals("End date is required", violation.getMessage());
    }

    @Test
    void endDateBeforeStartDateShouldFailValidation() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        Set<ConstraintViolation<DateRange>> violations = validator.validate(dateRange);
        assertEquals(1, violations.size());
        ConstraintViolation<DateRange> violation = violations.iterator().next();
        assertEquals("End date must be after or equal to start date", violation.getMessage());
    }

    @Test
    void sameStartAndEndDateShouldPassValidation() {
        LocalDate date = LocalDate.now().plusDays(1);
        DateRange dateRange = DateRange.builder()
                .startDate(date)
                .endDate(date)
                .build();

        Set<ConstraintViolation<DateRange>> violations = validator.validate(dateRange);
        assertTrue(violations.isEmpty(), "Same start and end date should be valid");
    }

    @Test
    void toDaysShouldCalculateCorrectly() {
        // Using specific dates (Monday to Wednesday) to ensure consistent test behavior
        LocalDate startDate = LocalDate.of(2024, 1, 8);  // Monday
        LocalDate endDate = LocalDate.of(2024, 1, 10);   // Wednesday
        DateRange dateRange = DateRange.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        assertEquals(3, dateRange.toDays());
    }

    @Test
    void toDaysForSingleDayShouldReturnOne() {
        LocalDate date = LocalDate.of(2024, 1, 8);  // Monday
        DateRange dateRange = DateRange.builder()
                .startDate(date)
                .endDate(date)
                .build();

        assertEquals(1, dateRange.toDays());
    }

    @Test
    void toDaysShouldExcludeWeekends() {
        LocalDate friday = LocalDate.of(2024, 1, 5);   // Friday
        LocalDate monday = LocalDate.of(2024, 1, 8);    // Monday
        DateRange dateRange = DateRange.builder()
                .startDate(friday)
                .endDate(monday)
                .build();

        assertEquals(2, dateRange.toDays()); // Friday and Monday only (Saturday and Sunday excluded)
    }

    @Test
    void toDaysShouldReturnZeroForWeekendOnly() {
        LocalDate saturday = LocalDate.of(2024, 1, 6);   // Saturday
        LocalDate sunday = LocalDate.of(2024, 1, 7);     // Sunday
        DateRange dateRange = DateRange.builder()
                .startDate(saturday)
                .endDate(sunday)
                .build();

        assertEquals(0, dateRange.toDays());
    }

    @Test
    void toDaysShouldHandleMultipleWeeks() {
        LocalDate monday1 = LocalDate.of(2024, 1, 1);   // Monday
        LocalDate friday2 = LocalDate.of(2024, 1, 12);  // Friday (2 weeks)
        DateRange dateRange = DateRange.builder()
                .startDate(monday1)
                .endDate(friday2)
                .build();

        assertEquals(10, dateRange.toDays()); // 10 business days in 2 weeks
    }

    @Test
    void toCalendarDaysShouldIncludeWeekends() {
        LocalDate friday = LocalDate.of(2024, 1, 5);   // Friday
        LocalDate monday = LocalDate.of(2024, 1, 8);    // Monday
        DateRange dateRange = DateRange.builder()
                .startDate(friday)
                .endDate(monday)
                .build();

        assertEquals(4, dateRange.toCalendarDays()); // Friday, Saturday, Sunday, Monday
    }

    @Test
    void toDaysWithNullDatesShouldReturnZero() {
        DateRange dateRange1 = DateRange.builder()
                .startDate(null)
                .endDate(LocalDate.now().plusDays(3))
                .build();

        DateRange dateRange2 = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(null)
                .build();

        assertEquals(0, dateRange1.toDays());
        assertEquals(0, dateRange2.toDays());
    }

    @Test
    void containsShouldReturnTrueForDateInRange() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 5);
        DateRange dateRange = DateRange.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        assertTrue(dateRange.contains(LocalDate.of(2024, 1, 1)));
        assertTrue(dateRange.contains(LocalDate.of(2024, 1, 3)));
        assertTrue(dateRange.contains(LocalDate.of(2024, 1, 5)));
        assertFalse(dateRange.contains(LocalDate.of(2023, 12, 31)));
        assertFalse(dateRange.contains(LocalDate.of(2024, 1, 6)));
    }

    @Test
    void containsWithNullValuesShouldReturnFalse() {
        DateRange dateRange = DateRange.builder().build();

        assertFalse(dateRange.contains(LocalDate.now()));
        assertFalse(dateRange.contains(null));
    }

    @Test
    void overlapsWithShouldDetectOverlappingRanges() {
        DateRange range1 = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 5))
                .build();

        DateRange overlappingRange = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 3))
                .endDate(LocalDate.of(2024, 1, 8))
                .build();

        DateRange nonOverlappingRange = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 10))
                .endDate(LocalDate.of(2024, 1, 15))
                .build();

        assertTrue(range1.overlapsWith(overlappingRange));
        assertFalse(range1.overlapsWith(nonOverlappingRange));
    }

    @Test
    void overlapsWithShouldDetectAdjacentRanges() {
        DateRange range1 = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 5))
                .build();

        DateRange adjacentRange = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 6))
                .endDate(LocalDate.of(2024, 1, 10))
                .build();

        assertFalse(range1.overlapsWith(adjacentRange));
    }

    @Test
    void overlapsWithNullShouldReturnFalse() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        assertFalse(dateRange.overlapsWith(null));
    }

    @Test
    void overlapsWithShouldReturnTrueForSameDates() {
        DateRange range1 = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 5))
                .build();

        DateRange range2 = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 5))
                .build();

        assertTrue(range1.overlapsWith(range2));
    }

    @Test
    void overlapsWithShouldReturnTrueForContainedRange() {
        DateRange range1 = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 10))
                .build();

        DateRange containedRange = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 3))
                .endDate(LocalDate.of(2024, 1, 7))
                .build();

        assertTrue(range1.overlapsWith(containedRange));
        assertTrue(containedRange.overlapsWith(range1));
    }
}
