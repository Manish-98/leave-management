package one.june.leave_management.application.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Data Transfer Object for leave information")
public class LeaveDto {
    @Schema(description = "Unique identifier of the leave request", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "User ID who requested the leave", example = "user123")
    private String userId;

    @Schema(description = "Date range of the leave period", implementation = DateRange.class)
    private DateRange dateRange;

    @Schema(description = "Type of leave (ANNUAL_LEAVE or OPTIONAL_HOLIDAY)", example = "ANNUAL_LEAVE")
    private LeaveType type;

    @Schema(description = "Current status of the leave request", example = "REQUESTED")
    private LeaveStatus status;

    @Builder.Default
    @Schema(description = "Duration type of the leave", example = "FULL_DAY")
    private LeaveDurationType durationType = LeaveDurationType.FULL_DAY;

    @Schema(description = "List of source references from different systems")
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