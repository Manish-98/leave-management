package one.june.leave_management.domain.leave.validation.impl;

import one.june.leave_management.common.exception.WeekendOnlyLeaveException;
import one.june.leave_management.common.util.BusinessDayUtil;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.validation.LeaveValidationResult;
import one.june.leave_management.domain.leave.validation.LeaveValidationStrategyBase;
import one.june.leave_management.domain.leave.validation.ValidationChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Validation strategy for annual leave type.
 * Performs all common validations required for annual leaves.
 */
@Component
public class AnnualLeaveValidationStrategy extends LeaveValidationStrategyBase {
    private static final Logger logger = LoggerFactory.getLogger(AnnualLeaveValidationStrategy.class);

    private final LeaveRepository leaveRepository;

    public AnnualLeaveValidationStrategy(EmployeeRepository employeeRepository,
                                         LeaveRepository leaveRepository) {
        super(employeeRepository);
        this.leaveRepository = leaveRepository;
    }

    @Override
    public LeaveType getType() {
        return LeaveType.ANNUAL_LEAVE;
    }

    @Override
    public LeaveValidationResult validate(Leave leave) {
        logger.info("Validating annual leave for user: {} with date range: {} to {}",
                    leave.getUserId(), leave.getStartDate(), leave.getEndDate());

        LeaveValidationResult result = ValidationChain.of(leave)
                .validate(this::validateBasicRequirements)
                .validate(l -> validateDateRange(l.getStartDate(), l.getEndDate()))
                .validate(this::validateWeekendConstraints)
                .validate(this::validateHalfDayConstraints)
                .validate(this::validateApprovedLeaveConstraints)
                .validate(l -> validateNoOverlappingLeaves(l, fetchOverlappingLeaves(l)))
                .getResult();

        if (result.isValid()) {
            logger.info("Annual leave validation successful for user: {}", leave.getUserId());
        }

        return result;
    }

    /**
     * Validates weekend-related constraints:
     * 1. Rejects leave requests that fall entirely on weekends
     * 2. Rejects half-day leaves that fall on weekends
     *
     * @param leave the leave to validate
     * @return validation result
     */
    private LeaveValidationResult validateWeekendConstraints(Leave leave) {
        // Check if leave falls entirely on weekends
        if (BusinessDayUtil.isWeekendOnly(leave.getStartDate(), leave.getEndDate())) {
            logger.warn("Leave request from {} to {} includes only weekend days for user: {}",
                       leave.getStartDate(), leave.getEndDate(), leave.getUserId());
            throw new WeekendOnlyLeaveException(leave.getStartDate(), leave.getEndDate());
        }

        // Check if half-day leave falls on weekend
        if (leave.getDurationType() == LeaveDurationType.FIRST_HALF ||
            leave.getDurationType() == LeaveDurationType.SECOND_HALF) {
            // For half-day leaves, start and end dates must be the same
            LocalDate halfDayDate = leave.getStartDate();
            if (BusinessDayUtil.isWeekend(halfDayDate)) {
                logger.warn("Half-day leave on weekend day {} for user: {}",
                           halfDayDate, leave.getUserId());
                return LeaveValidationResult.failure(
                    String.format("Half-day leave cannot be on a weekend (Saturday/Sunday). Date: %s", halfDayDate)
                );
            }
        }

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
