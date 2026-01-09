package one.june.leave_management.domain.leave.validation;

import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for leave validation strategies.
 * Contains common helper methods that can be used by concrete strategy implementations.
 * Helper methods receive data as parameters and return validation results.
 */
public abstract class LeaveValidationStrategyBase {
    private static final Logger logger = LoggerFactory.getLogger(LeaveValidationStrategyBase.class);

    protected final EmployeeRepository employeeRepository;

    protected LeaveValidationStrategyBase(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Returns the leave type this strategy handles.
     *
     * @return the leave type
     */
    public abstract LeaveType getType();

    /**
     * Validates the given leave according to this strategy's rules.
     *
     * @param leave the leave to validate
     * @return the validation result
     */
    public abstract LeaveValidationResult validate(Leave leave);

    /**
     * Validates basic leave requirements:
     * - Leave is not null
     * - Status, type, and durationType are not null
     * - New leaves (id == null) must have at least one source reference
     * - Employee exists for the given userId
     *
     * @param leave the leave to validate
     * @return validation result
     */
    protected LeaveValidationResult validateBasicRequirements(Leave leave) {
        logger.debug("Validating basic requirements for leave");

        if (leave == null) {
            return LeaveValidationResult.failure("Leave cannot be null");
        }

        if (leave.getStatus() == null) {
            return LeaveValidationResult.failure("Leave status cannot be null");
        }

        if (leave.getType() == null) {
            return LeaveValidationResult.failure("Leave type cannot be null");
        }

        if (leave.getDurationType() == null) {
            return LeaveValidationResult.failure("Leave duration type cannot be null");
        }

        if (leave.getId() == null && !leave.hasSourceRefs()) {
            return LeaveValidationResult.failure("New leaves must have at least one source reference");
        }

        // Validate that the employee exists for the given userId (employee UUID)
        if (leave.getUserId() != null) {
            String userId = leave.getUserId();
            // Parse UUID and check if employee exists
            try {
                java.util.UUID employeeUuid = java.util.UUID.fromString(userId);
                if (!employeeRepository.findById(employeeUuid).isPresent()) {
                    return LeaveValidationResult.failure(
                            String.format("Employee not found with ID: %s", userId)
                    );
                }
            } catch (IllegalArgumentException e) {
                return LeaveValidationResult.failure(
                        String.format("Invalid employee UUID format: %s", userId)
                );
            }
        }

        return LeaveValidationResult.success();
    }

    /**
     * Validates that the start date is not after the end date.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return validation result
     */
    protected LeaveValidationResult validateDateRange(LocalDate startDate, LocalDate endDate) {
        logger.debug("Validating date range: {} to {}", startDate, endDate);

        if (startDate == null || endDate == null) {
            return LeaveValidationResult.failure("Start date and end date cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            return LeaveValidationResult.failure("Start date cannot be after end date");
        }

        return LeaveValidationResult.success();
    }

    /**
     * Validates half-day leave constraints.
     * Half-day leaves (FIRST_HALF or SECOND_HALF) must have the same start and end date.
     *
     * @param leave the leave to validate
     * @return validation result
     */
    protected LeaveValidationResult validateHalfDayConstraints(Leave leave) {
        logger.debug("Validating half-day constraints for leave with duration type: {}", leave.getDurationType());

        if (leave.getDurationType() != LeaveDurationType.FULL_DAY) {
            LocalDate startDate = leave.getStartDate();
            LocalDate endDate = leave.getEndDate();

            if (startDate == null || endDate == null) {
                return LeaveValidationResult.failure("Start date and end date cannot be null for half-day leaves");
            }

            if (!startDate.equals(endDate)) {
                return LeaveValidationResult.failure("Half-day leaves must have the same start and end date");
            }
        }

        return LeaveValidationResult.success();
    }

    /**
     * Validates that the leave does not overlap with existing leaves.
     *
     * @param leave the leave to validate
     * @param existingLeaves the list of existing leaves to check against
     * @return validation result
     */
    protected LeaveValidationResult validateNoOverlappingLeaves(Leave leave, List<Leave> existingLeaves) {
        logger.debug("Checking for overlapping leaves for user {} with date range {}",
                    leave.getUserId(), leave.getDateRange());

        if (existingLeaves == null || existingLeaves.isEmpty()) {
            return LeaveValidationResult.success();
        }

        Leave overlappingLeave = existingLeaves.get(0);
        String error = String.format(
                "User %s already has a leave on %s",
                leave.getUserId(),
                leave.getStartDate()
        );

        logger.warn(error);
        return LeaveValidationResult.failure(error);
    }

    /**
     * Validates that the leave is a single-day leave (start date equals end date).
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return validation result
     */
    protected LeaveValidationResult validateSingleDay(LocalDate startDate, LocalDate endDate) {
        logger.debug("Validating single-day leave: {} to {}", startDate, endDate);

        if (startDate == null || endDate == null) {
            return LeaveValidationResult.failure("Start date and end date cannot be null");
        }

        if (!startDate.equals(endDate)) {
            return LeaveValidationResult.failure("Optional holidays must be single-day only");
        }

        return LeaveValidationResult.success();
    }

    /**
     * Validates that the given date exists in the list of optional holiday dates.
     *
     * @param date the date to check
     * @param holidayDates the list of valid optional holiday dates
     * @return validation result
     */
    protected LeaveValidationResult validateOptionalHolidayExists(LocalDate date, List<LocalDate> holidayDates) {
        logger.debug("Validating that date {} exists in optional holidays", date);

        if (date == null) {
            return LeaveValidationResult.failure("Date cannot be null");
        }

        // If the list is null or empty, or doesn't contain the date, it's invalid
        if (holidayDates == null || !holidayDates.contains(date)) {
            return LeaveValidationResult.failure(
                    String.format("Date %s is not a valid optional holiday", date)
            );
        }

        return LeaveValidationResult.success();
    }

    /**
     * Validates that approved leaves are at least 1 day long (start date <= end date).
     *
     * @param leave the leave to validate
     * @return validation result
     */
    protected LeaveValidationResult validateApprovedLeaveConstraints(Leave leave) {
        logger.debug("Validating approved leave constraints");

        if (leave.getStatus() == LeaveStatus.APPROVED) {
            LocalDate startDate = leave.getStartDate();
            LocalDate endDate = leave.getEndDate();

            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                return LeaveValidationResult.failure("Approved leaves must be at least 1 day long");
            }
        }

        return LeaveValidationResult.success();
    }
}
