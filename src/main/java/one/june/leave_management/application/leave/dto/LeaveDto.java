package one.june.leave_management.application.leave.dto;

import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDto {
    private UUID id;
    private String userId;
    private DateRange dateRange;
    private LeaveType type;
    private LeaveStatus status;
    @Builder.Default
    private LeaveDurationType durationType = LeaveDurationType.FULL_DAY;
    private List<LeaveSourceRefDto> sourceRefs;

    // Convenience getters for backward compatibility
    public LocalDate getStartDate() {
        return dateRange != null ? dateRange.getStartDate() : null;
    }

    public LocalDate getEndDate() {
        return dateRange != null ? dateRange.getEndDate() : null;
    }

    public double getDurationInDays() {
        if (dateRange == null) {
            return 0;
        }

        if (durationType == LeaveDurationType.FULL_DAY) {
            return dateRange.toDays();
        } else {
            // Half day leaves (FIRST_HALF or SECOND_HALF)
            return 0.5;
        }
    }
}