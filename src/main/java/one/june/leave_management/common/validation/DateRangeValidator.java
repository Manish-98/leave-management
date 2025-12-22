package one.june.leave_management.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import one.june.leave_management.common.model.DateRange;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, DateRange> {

    @Override
    public boolean isValid(DateRange dateRange, ConstraintValidatorContext context) {
        if (dateRange == null) {
            return true; // Let @NotNull handle null checks at class level
        }

        if (dateRange.getStartDate() == null || dateRange.getEndDate() == null) {
            return true; // Let @NotNull handle null checks for individual fields
        }

        return !dateRange.getEndDate().isBefore(dateRange.getStartDate());
    }
}