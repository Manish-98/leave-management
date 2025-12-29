package one.june.leave_management.domain.leave.service;

import one.june.leave_management.common.exception.InvalidOptionalHolidayException;
import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.OptionalHolidayRepository;
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

    public LeaveDomainService(LeaveRepository leaveRepository, OptionalHolidayRepository optionalHolidayRepository) {
        this.leaveRepository = leaveRepository;
        this.optionalHolidayRepository = optionalHolidayRepository;
    }

    public void validateLeaveForPersistence(Leave leave) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave cannot be null");
        }

        if (leave.getId() == null && !leave.hasSourceRefs()) {
            throw new IllegalArgumentException("New leaves must have at least one source reference");
        }

        if (leave.getStatus() == null) {
            throw new IllegalArgumentException("Leave status cannot be null");
        }

        if (leave.getType() == null) {
            throw new IllegalArgumentException("Leave type cannot be null");
        }

        if (leave.getDurationType() == null) {
            throw new IllegalArgumentException("Leave duration type cannot be null");
        }

        // Validate half-day leaves
        if (leave.getDurationType() != LeaveDurationType.FULL_DAY) {
            if (!leave.getStartDate().equals(leave.getEndDate())) {
                throw new IllegalArgumentException("Half-day leaves must have the same start and end date");
            }
        }

        // Additional business validations can be added here
        if (leave.getStatus() == LeaveStatus.APPROVED && leave.getStartDate().isAfter(leave.getEndDate())) {
            throw new IllegalArgumentException("Approved leaves must be at least 1 day long");
        }
    }

    /**
     * Validates that a leave does not overlap with existing leaves for the same user.
     *
     * @param leave the leave to validate for overlaps
     * @throws OverlappingLeaveException if the leave overlaps with existing leaves
     */
    public void validateNoOverlappingLeaves(Leave leave) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave cannot be null");
        }

        logger.debug("Checking for overlapping leaves for user {} with date range {}",
                    leave.getUserId(), leave.getDateRange());

        // Find overlapping leaves, excluding the current leave if it's an update
        List<Leave> overlappingLeaves;
        if (leave.getId() != null) {
            overlappingLeaves = leaveRepository.findOverlappingLeaves(
                    leave.getUserId(), leave.getDateRange(), leave.getId());
        } else {
            overlappingLeaves = leaveRepository.findOverlappingLeaves(
                    leave.getUserId(), leave.getDateRange());
        }

        if (!overlappingLeaves.isEmpty()) {
            Leave existingLeave = overlappingLeaves.get(0); // Take the first overlapping leave
            throw new OverlappingLeaveException(
                    leave.getUserId(),
                    leave.getStartDate(),
                    leave.getEndDate(),
                    existingLeave.getId()
            );
        }

        logger.debug("No overlapping leaves found for user {} with date range {}",
                    leave.getUserId(), leave.getDateRange());
    }

    /**
     * Validates that an optional holiday leave is valid:
     * 1. Must be a single-day leave (start date == end date)
     * 2. The requested date must exist in the optional_holidays table
     *
     * @param leave the leave to validate
     * @throws InvalidOptionalHolidayException if the optional holiday leave is invalid
     */
    public void validateOptionalHolidayDate(Leave leave) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave cannot be null");
        }

        // Only validate optional holiday leaves
        if (leave.getType() != LeaveType.OPTIONAL_HOLIDAY) {
            logger.debug("Skipping optional holiday validation for leave type: {}", leave.getType());
            return;
        }

        logger.debug("Validating optional holiday for user {} with date range {}",
                    leave.getUserId(), leave.getDateRange());

        // Validate that it's a single-day leave
        if (!leave.getStartDate().equals(leave.getEndDate())) {
            logger.warn("Multi-day optional holiday requested for user {}: {} to {}",
                       leave.getUserId(), leave.getStartDate(), leave.getEndDate());
            throw InvalidOptionalHolidayException.multiDayNotAllowed(
                    leave.getUserId(),
                    leave.getStartDate(),
                    leave.getEndDate(),
                    leave.getId()
            );
        }

        // Validate that the date exists in the optional_holidays table
        LocalDate requestedDate = leave.getStartDate();
        boolean holidayExists = optionalHolidayRepository.findByDate(requestedDate).isPresent();

        if (!holidayExists) {
            logger.warn("Optional holiday date not found in database: {} for user {}",
                       requestedDate, leave.getUserId());
            throw InvalidOptionalHolidayException.dateNotFound(
                    leave.getUserId(),
                    requestedDate,
                    leave.getId()
            );
        }

        logger.debug("Optional holiday validation successful for user {} on date {}",
                    leave.getUserId(), requestedDate);
    }
}