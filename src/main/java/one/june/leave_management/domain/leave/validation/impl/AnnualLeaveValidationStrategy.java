package one.june.leave_management.domain.leave.validation.impl;

import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.validation.LeaveValidationResult;
import one.june.leave_management.domain.leave.validation.LeaveValidationStrategyBase;
import one.june.leave_management.domain.leave.validation.ValidationChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
