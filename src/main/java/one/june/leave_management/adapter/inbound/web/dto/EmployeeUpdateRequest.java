package one.june.leave_management.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for updating an existing employee")
public class EmployeeUpdateRequest {

    @Schema(
            description = "Full name of the employee",
            example = "John Doe",
            minLength = 1,
            maxLength = 255
    )
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Schema(
            description = "Slack user ID",
            example = "U12345",
            maxLength = 255
    )
    @Size(max = 255, message = "Slack ID must not exceed 255 characters")
    private String slackId;

    @Schema(
            description = "Google user ID",
            example = "john.doe@example.com",
            maxLength = 255
    )
    @Size(max = 255, message = "Google ID must not exceed 255 characters")
    private String googleId;

    @Schema(
            description = "Display name in Slack",
            example = "John D",
            maxLength = 255
    )
    @Size(max = 255, message = "Slack display name must not exceed 255 characters")
    private String slackDisplayName;

    @Schema(
            description = "Date when the employee joined the company",
            example = "2020-01-15"
    )
    private LocalDate dateOfJoining;

    @Schema(
            description = "Employee active status",
            example = "true"
    )
    private Boolean active;

    @Schema(
            description = "Carry forward leaves by year (year -> days map)",
            example = "{\"2024\": 5, \"2025\": 3}"
    )
    private Map<Integer, Integer> carryForwardLeaves;
}
