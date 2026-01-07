package one.june.leave_management.application.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import one.june.leave_management.domain.common.model.Region;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data Transfer Object for employee information")
public class EmployeeDto {
    @Schema(description = "Unique identifier of the employee", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Full name of the employee", example = "John Doe", required = true)
    private String name;

    @Schema(description = "Slack user ID", example = "U12345")
    private String slackId;

    @Schema(description = "Google user ID", example = "john.doe@example.com")
    private String googleId;

    @Schema(description = "Display name in Slack", example = "John D")
    private String slackDisplayName;

    @Schema(description = "Date when the employee joined the company", example = "2020-01-15", required = true)
    private LocalDate dateOfJoining;

    @Schema(description = "Employee active status", example = "true")
    private Boolean active;

    @Schema(description = "Employee region", example = "PUNE", required = true)
    private Region region;

    @Schema(description = "Carry forward leaves by year (year -> days map)", example = "{\"2024\": 5, \"2025\": 3}")
    private Map<Integer, Integer> carryForwardLeaves;

    @Schema(description = "Timestamp when the record was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the record was last updated", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
}
