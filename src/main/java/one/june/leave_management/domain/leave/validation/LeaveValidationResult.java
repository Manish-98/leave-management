package one.june.leave_management.domain.leave.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a leave validation operation.
 * Contains validation status and any error messages encountered.
 */
public class LeaveValidationResult {
    private final boolean valid;
    private final List<String> errors;

    private LeaveValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    /**
     * Creates a successful validation result with no errors.
     *
     * @return a valid LeaveValidationResult
     */
    public static LeaveValidationResult success() {
        return new LeaveValidationResult(true, List.of());
    }

    /**
     * Creates a failed validation result with a single error message.
     *
     * @param error the error message
     * @return an invalid LeaveValidationResult
     */
    public static LeaveValidationResult failure(String error) {
        return new LeaveValidationResult(false, List.of(error));
    }

    /**
     * Creates a failed validation result with multiple error messages.
     *
     * @param errors the list of error messages
     * @return an invalid LeaveValidationResult
     */
    public static LeaveValidationResult failure(List<String> errors) {
        return new LeaveValidationResult(false, errors);
    }

    /**
     * Merges multiple validation results into a single result.
     * If any result is invalid, the merged result will be invalid.
     * All errors from all results are combined.
     *
     * @param results the validation results to merge
     * @return a merged LeaveValidationResult
     */
    public static LeaveValidationResult merge(LeaveValidationResult... results) {
        List<String> allErrors = new ArrayList<>();
        boolean allValid = true;

        for (LeaveValidationResult result : results) {
            if (result == null) {
                continue;
            }
            if (!result.isValid()) {
                allValid = false;
            }
            allErrors.addAll(result.getErrors());
        }

        return allValid ? success() : failure(allErrors);
    }

    /**
     * Checks if the validation was successful.
     *
     * @return true if validation passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the list of error messages.
     * Returns an empty list if validation was successful.
     *
     * @return an immutable list of error messages
     */
    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaveValidationResult that = (LeaveValidationResult) o;
        return valid == that.valid && Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, errors);
    }

    @Override
    public String toString() {
        return "LeaveValidationResult{" +
                "valid=" + valid +
                ", errors=" + errors +
                '}';
    }
}
