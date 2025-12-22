package one.june.leave_management.adapter.inbound.rest.dto;

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
public class LeaveIngestionRequest {

    @NotNull(message = "Source type is required")
    private SourceType sourceType;

    @NotBlank(message = "Source ID is required")
    @Size(max = 100, message = "Source ID must not exceed 100 characters")
    private String sourceId;

    @NotBlank(message = "User ID is required")
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId;

    @Valid
    @NotNull(message = "Date range is required")
    private DateRange dateRange;

    @NotNull(message = "Leave type is required")
    private LeaveType type;

    @NotNull(message = "Leave status is required")
    private LeaveStatus status;
}