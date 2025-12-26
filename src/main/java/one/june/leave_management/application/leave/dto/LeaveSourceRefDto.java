package one.june.leave_management.application.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import one.june.leave_management.domain.leave.model.SourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reference to the source system from which this leave originated")
public class LeaveSourceRefDto {
    @Schema(description = "Unique identifier of the leave source reference", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Type of source system (e.g., WEB, SLACK, CALENDAR, KIMAI)", example = "SLACK")
    private SourceType sourceType;

    @Schema(description = "ID of the leave in the source system", example = "slack-msg-12345")
    private String sourceId;
}