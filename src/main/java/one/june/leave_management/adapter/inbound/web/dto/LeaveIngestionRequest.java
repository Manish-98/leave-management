package one.june.leave_management.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

/**
 * Request DTO for leave ingestion endpoint
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating a new leave request")
public class LeaveIngestionRequest {

    @Schema(
            description = "Source system from which this leave request originated",
            example = "WEB",
            required = true
    )
    @NotNull(message = "Source type is required")
    private SourceType sourceType;

    @Schema(
            description = "ID of the leave in the source system",
            example = "web-req-12345",
            required = true,
            minLength = 1,
            maxLength = 100
    )
    @NotBlank(message = "Source ID is required")
    @Size(max = 100, message = "Source ID must not exceed 100 characters")
    private String sourceId;

    @Schema(
            description = "User ID requesting the leave",
            example = "user123",
            required = true,
            minLength = 1,
            maxLength = 50
    )
    @NotBlank(message = "User ID is required")
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId;

    @Schema(
            description = "Start and end dates of the leave period",
            required = true,
            implementation = DateRange.class
    )
    @Valid
    @NotNull(message = "Date range is required")
    private DateRange dateRange;

    @Schema(
            description = "Type of leave being requested",
            example = "ANNUAL_LEAVE",
            required = true
    )
    @NotNull(message = "Leave type is required")
    private LeaveType type;

    @Schema(
            description = "Initial status of the leave request",
            example = "REQUESTED",
            required = true,
            allowableValues = {"REQUESTED", "APPROVED", "CANCELLED"}
    )
    @NotNull(message = "Leave status is required")
    private LeaveStatus status;

    @Schema(
            description = "Duration type of the leave (defaults to FULL_DAY)",
            example = "FULL_DAY",
            allowableValues = {"FULL_DAY", "FIRST_HALF", "SECOND_HALF"}
    )
    @Builder.Default
    private LeaveDurationType durationType = LeaveDurationType.FULL_DAY;
}