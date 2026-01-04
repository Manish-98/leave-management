package one.june.leave_management.domain.leave.validation.impl;

import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.leave.model.Leave;
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

    public OptionalHolidayValidationStrategy(EmployeeRepository employeeRepository,
                                             LeaveRepository leaveRepository,
                                             OptionalHolidayRepository optionalHolidayRepository) {
        super(employeeRepository);
        this.leaveRepository = leaveRepository;
        this.optionalHolidayRepository = optionalHolidayRepository;
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
                .validate(l -> validateOptionalHolidayExists(l.getStartDate(), fetchOptionalHolidayDates()))
                .validate(l -> validateNoOverlappingLeaves(l, fetchOverlappingLeaves(l)))
                .getResult();

        if (result.isValid()) {
            logger.info("Optional holiday leave validation successful for user: {} on date: {}",
                        leave.getUserId(), leave.getStartDate());
        }

        return result;
    }

    /**
     * Fetches all optional holiday dates from the repository.
     *
     * @return list of optional holiday dates
     */
    private List<LocalDate> fetchOptionalHolidayDates() {
        logger.debug("Fetching all optional holiday dates");
        List<OptionalHoliday> holidays = optionalHolidayRepository.findAll();
        List<LocalDate> dates = holidays.stream()
                .map(OptionalHoliday::getDate)
                .collect(Collectors.toList());
        logger.debug("Found {} optional holiday dates", dates.size());
        return dates;
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
