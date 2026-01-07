package one.june.leave_management.domain.common.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Geographical region for employees and optional holidays")
public enum Region {
    @Schema(description = "Pune region")
    PUNE,

    @Schema(description = "Bangalore region")
    BANGALORE,

    @Schema(description = "Hyderabad region")
    HYDERABAD
}
