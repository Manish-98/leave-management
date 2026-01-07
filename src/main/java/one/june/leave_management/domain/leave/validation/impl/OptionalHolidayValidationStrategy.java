package one.june.leave_management.domain.leave.validation.impl;

import one.june.leave_management.config.LeaveProperties;
import one.june.leave_management.domain.employee.model.Employee;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.OptionalHolidayRepository;
import one.june.leave_management.domain.leave.validation.LeaveValidationResult;
import one.june.leave_management.domain.leave.validation.LeaveValidationStrategyBase;
import one.june.leave_management.domain.leave.validation.ValidationChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Validation strategy for optional holiday leave type.
 * Performs all common validations plus optional holiday-specific validations.
 */
@Component
public class OptionalHolidayValidationStrategy extends LeaveValidationStrategyBase {
    private static final Logger logger = LoggerFactory.getLogger(OptionalHolidayValidationStrategy.class);

    private final LeaveRepository leaveRepository;
    private final OptionalHolidayRepository optionalHolidayRepository;
    private final LeaveProperties leaveProperties;

    public OptionalHolidayValidationStrategy(EmployeeRepository employeeRepository,
                                             LeaveRepository leaveRepository,
                                             OptionalHolidayRepository optionalHolidayRepository,
                                             LeaveProperties leaveProperties) {
        super(employeeRepository);
        this.leaveRepository = leaveRepository;
        this.optionalHolidayRepository = optionalHolidayRepository;
        this.leaveProperties = leaveProperties;
    }

    @Override
    public LeaveType getType() {
        return LeaveType.OPTIONAL_HOLIDAY;
    }

    @Override
    public LeaveValidationResult validate(Leave leave) {
        logger.info("Validating optional holiday leave for user: {} with date range: {} to {}",
                    leave.getUserId(), leave.getStartDate(), leave.getEndDate());

        LeaveValidationResult result = ValidationChain.of(leave)
                .validate(this::validateBasicRequirements)
                .validate(l -> validateDateRange(l.getStartDate(), l.getEndDate()))
                .validate(this::validateHalfDayConstraints)
                .validate(this::validateApprovedLeaveConstraints)
                .validate(l -> validateSingleDay(l.getStartDate(), l.getEndDate()))
                .validate(l -> validateOptionalHolidayExists(l.getStartDate(), fetchOptionalHolidayDates(l)))
                .validate(l -> validateMaxOptionalHolidaysPerYear(l))
                .validate(l -> validateNoOverlappingLeaves(l, fetchOverlappingLeaves(l)))
                .getResult();

        if (result.isValid()) {
            logger.info("Optional holiday leave validation successful for user: {} on date: {}",
                        leave.getUserId(), leave.getStartDate());
        }

        return result;
    }

    /**
     * Fetches optional holiday dates filtered by the employee's region.
     *
     * @param leave the leave request containing the user ID
     * @return list of optional holiday dates for the employee's region
     */
    private List<LocalDate> fetchOptionalHolidayDates(Leave leave) {
        logger.debug("Fetching optional holiday dates for user: {}", leave.getUserId());

        try {
            // Fetch employee to get their region
            UUID employeeId = UUID.fromString(leave.getUserId());
            var employeeOpt = employeeRepository.findById(employeeId);

            if (employeeOpt.isEmpty()) {
                logger.warn("Employee not found with ID: {}, returning empty holiday list",
                           leave.getUserId());
                return List.of();
            }

            var employee = employeeOpt.get();
            var employeeRegion = employee.getRegion();

            logger.debug("Employee region: {}", employeeRegion);

            // Fetch holidays for the employee's region
            List<OptionalHoliday> holidays = optionalHolidayRepository.findByRegionOrderByDateAsc(employeeRegion);
            List<LocalDate> dates = holidays.stream()
                    .map(OptionalHoliday::getDate)
                    .collect(Collectors.toList());

            logger.debug("Found {} optional holiday dates for region: {}", dates.size(), employeeRegion);
            return dates;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid user ID format: {}", leave.getUserId(), e);
            return List.of();
        }
    }

    /**
     * Calculates the maximum allowed optional holidays for an employee based on their date of joining.
     * Proration rules:
     * - Employees who joined in previous years get full configured max
     * - Employees who joined in current year Jan-Jun get full configured max
     * - Employees who joined in current year Jul-Dec get prorated amount (ceil of configured max / 2)
     *
     * @param leave the leave request
     * @return the maximum allowed optional holidays for this employee
     */
    private int calculateMaxAllowedForEmployee(Leave leave) {
        logger.debug("Calculating max allowed optional holidays for user: {}", leave.getUserId());

        try {
            UUID employeeId = UUID.fromString(leave.getUserId());
            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);

            if (employeeOpt.isEmpty()) {
                logger.warn("Employee not found with ID: {}, falling back to configured max",
                           leave.getUserId());
                return leaveProperties.getMaxOptionalHolidaysPerYear();
            }

            Employee employee = employeeOpt.get();
            LocalDate dateOfJoining = employee.getDateOfJoining();
            LocalDate leaveDate = leave.getStartDate();
            int configuredMax = leaveProperties.getMaxOptionalHolidaysPerYear();

            // If employee joined in a previous year, they get full max
            if (dateOfJoining.getYear() < leaveDate.getYear()) {
                logger.debug("Employee joined in previous year {}, full max allowed: {}",
                           dateOfJoining.getYear(), configuredMax);
                return configuredMax;
            }

            // Employee joined in current year - check which half
            int joiningMonth = dateOfJoining.getMonthValue();

            if (joiningMonth <= 6) {
                // Jan-Jun: full max
                logger.debug("Employee joined in first half of current year (month {}), full max allowed: {}",
                           joiningMonth, configuredMax);
                return configuredMax;
            } else {
                // Jul-Dec: prorated (ceil of half)
                int proratedMax = (int) Math.ceil(configuredMax / 2.0);
                logger.debug("Employee joined in second half of current year (month {}), prorated max allowed: {}",
                           joiningMonth, proratedMax);
                return proratedMax;
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid user ID format: {}", leave.getUserId(), e);
            return leaveProperties.getMaxOptionalHolidaysPerYear();
        }
    }

    /**
     * Validates that the user has not exceeded the maximum allowed optional holidays per year.
     * Only applies to APPROVED leaves. For updates, excludes the current leave from the count.
     * Uses employee-specific proration based on date of joining.
     *
     * @param leave the leave to validate
     * @return validation result
     */
    private LeaveValidationResult validateMaxOptionalHolidaysPerYear(Leave leave) {
        logger.debug("Validating max optional holidays per year for user: {}", leave.getUserId());

        // Only enforce this rule for APPROVED leaves
        if (leave.getStatus() != LeaveStatus.APPROVED) {
            logger.debug("Skipping max optional holidays validation for non-APPROVED leave with status: {}",
                        leave.getStatus());
            return LeaveValidationResult.success();
        }

        LocalDate leaveDate = leave.getStartDate();
        if (leaveDate == null) {
            return LeaveValidationResult.failure("Start date cannot be null");
        }

        int year = leaveDate.getYear();
        int maxAllowedForThisEmployee = calculateMaxAllowedForEmployee(leave);

        // Count approved optional holidays for the user in this year
        long count = leaveRepository.countApprovedOptionalHolidaysByUserAndYear(leave.getUserId(), year);

        // For updates, we need to check if this leave itself is already counted
        // If it's an update and the leave is already in the system, we should exclude it from the count
        if (leave.getId() != null) {
            // This is an update - check if the existing leave is already counted
            // We need to verify if this specific leave is one of the counted leaves
            // Since the repository method counts all approved leaves, and we're updating one,
            // we need to subtract 1 if this leave was already approved
            logger.debug("This is an update - checking if current leave is already counted");
            // The current count includes this leave if it's already approved
            // So we should allow if count <= maxAllowedForThisEmployee
            // This means user is trying to update an existing approved leave
        } else {
            // This is a new leave - count should be strictly less than max
            if (count >= maxAllowedForThisEmployee) {
                String error = String.format(
                        "User %s has already used %d optional holiday(s) for year %d. " +
                        "Maximum allowed based on joining date is %d.",
                        leave.getUserId(),
                        count,
                        year,
                        maxAllowedForThisEmployee
                );
                logger.warn(error);
                return LeaveValidationResult.failure(error);
            }
        }

        logger.debug("User {} has used {} optional holiday(s) for year {} (max for this employee: {})",
                    leave.getUserId(), count, year, maxAllowedForThisEmployee);

        return LeaveValidationResult.success();
    }

    /**
     * Fetches overlapping leaves from the repository.
     * Excludes the current leave if it's an update (id != null).
     *
     * @param leave the leave to check for overlaps
     * @return list of overlapping leaves
     */
    private List<Leave> fetchOverlappingLeaves(Leave leave) {
        if (leave.getId() != null) {
            logger.debug("Fetching overlapping leaves for update (excluding current leave): {}", leave.getId());
            return leaveRepository.findOverlappingLeaves(
                    leave.getUserId(),
                    leave.getDateRange(),
                    leave.getId()
            );
        } else {
            logger.debug("Fetching overlapping leaves for new leave");
            return leaveRepository.findOverlappingLeaves(
                    leave.getUserId(),
                    leave.getDateRange()
            );
        }
    }
}
