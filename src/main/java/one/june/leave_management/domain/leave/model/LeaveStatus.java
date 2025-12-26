package one.june.leave_management.domain.leave.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of a leave request")
public enum LeaveStatus {
    @Schema(description = "Leave request has been submitted and is awaiting approval")
    REQUESTED,
    @Schema(description = "Leave request has been approved")
    APPROVED,
    @Schema(description = "Leave request has been cancelled")
    CANCELLED
}