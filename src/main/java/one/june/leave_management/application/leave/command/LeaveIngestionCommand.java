package one.june.leave_management.application.leave.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;

import java.time.LocalDate;

@Setter
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LeaveIngestionCommand {
    private SourceType sourceType;
    private String sourceId;
    private String userId;
    private DateRange dateRange;
    private LeaveType type;
    private LeaveStatus status;
    @Builder.Default
    private LeaveDurationType durationType = LeaveDurationType.FULL_DAY;

    // Convenience getters for backward compatibility
    public LocalDate getStartDate() {
        return dateRange != null ? dateRange.getStartDate() : null;
    }

    public LocalDate getEndDate() {
        return dateRange != null ? dateRange.getEndDate() : null;
    }
}