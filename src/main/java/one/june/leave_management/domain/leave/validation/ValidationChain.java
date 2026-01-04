package one.june.leave_management.domain.leave.validation;

import one.june.leave_management.domain.leave.model.Leave;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Fluent validation chain API for leave validation.
 * Provides a Stream-like API for chaining validation operations.
 *
 * <p>Usage example:
 * <pre>{@code
 * return ValidationChain.of(leave)
 *         .validate(this::validateBasicRequirements)
 *         .validate(l -> validateDateRange(l.getStartDate(), l.getEndDate()))
 *         .validate(this::validateHalfDayConstraints)
 *         .getResult();
 * }</pre>
 */
public class ValidationChain {
    private final Leave leave;
    private final List<Function<Leave, LeaveValidationResult>> validators = new ArrayList<>();

    private ValidationChain(Leave leave) {
        this.leave = leave;
    }

    /**
     * Creates a new validation chain for the given leave.
     *
     * @param leave the leave to validate
     * @return a new ValidationChain instance
     */
    public static ValidationChain of(Leave leave) {
        return new ValidationChain(leave);
    }

    /**
     * Adds a validation step to the chain.
     * Each validator is a function that takes a Leave and returns a LeaveValidationResult.
     *
     * @param validator the validation function to add
     * @return this ValidationChain for method chaining
     */
    public ValidationChain validate(Function<Leave, LeaveValidationResult> validator) {
        validators.add(validator);
        return this;
    }

    /**
     * Executes all validation steps in the chain.
     * Validations are executed in order and stops at the first failure (fail-fast).
     *
     * @return the first failed validation result, or success if all validations pass
     */
    public LeaveValidationResult getResult() {
        for (Function<Leave, LeaveValidationResult> validator : validators) {
            LeaveValidationResult result = validator.apply(leave);
            if (!result.isValid()) {
                return result;
            }
        }
        return LeaveValidationResult.success();
    }

    /**
     * Convenience method to check if all validations passed.
     * Equivalent to getResult().isValid().
     *
     * @return true if all validations passed, false otherwise
     */
    public boolean isValid() {
        return getResult().isValid();
    }
}
