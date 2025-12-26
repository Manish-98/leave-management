package one.june.leave_management.domain.leave.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of leave request")
public enum LeaveType {
    @Schema(description = "Regular annual leave/vacation days")
    ANNUAL_LEAVE,
    @Schema(description = "Optional holiday leave")
    OPTIONAL_HOLIDAY
}