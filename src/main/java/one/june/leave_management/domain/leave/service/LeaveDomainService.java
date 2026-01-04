package one.june.leave_management.domain.leave.service;

import one.june.leave_management.common.exception.InvalidOptionalHolidayException;
import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.OptionalHolidayRepository;
import one.june.leave_management.domain.leave.validation.LeaveValidationResult;
import one.june.leave_management.domain.leave.validation.LeaveValidationStrategyBase;
import one.june.leave_management.domain.leave.validation.LeaveValidationStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveDomainService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveDomainService.class);

    private final LeaveRepository leaveRepository;
    private final OptionalHolidayRepository optionalHolidayRepository;
    private final LeaveValidationStrategyRegistry strategyRegistry;

    public LeaveDomainService(LeaveRepository leaveRepository,
                             OptionalHolidayRepository optionalHolidayRepository,
                             LeaveValidationStrategyRegistry strategyRegistry) {
        this.leaveRepository = leaveRepository;
        this.optionalHolidayRepository = optionalHolidayRepository;
        this.strategyRegistry = strategyRegistry;
    }

    /**
     * Validates a leave for persistence using the appropriate validation strategy.
     * Converts validation result to exception for backward compatibility.
     *
     * @param leave the leave to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateLeaveForPersistence(Leave leave) {
        logger.debug("Validating leave for persistence using strategy-based validation");

        LeaveValidationResult result = validateLeave(leave);

        if (!result.isValid()) {
            String errorMessage = String.join("; ", result.getErrors());
            logger.error("Leave validation failed: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        logger.debug("Leave validation successful");
    }

    /**
     * Validates a leave using the appropriate validation strategy.
     *
     * @param leave the leave to validate
     * @return the validation result
     */
    private LeaveValidationResult validateLeave(Leave leave) {
        if (leave == null || leave.getType() == null) {
            return LeaveValidationResult.failure("Leave or leave type cannot be null");
        }

        LeaveValidationStrategyBase strategy = strategyRegistry.getStrategy(leave.getType());
        return strategy.validate(leave);
    }
}