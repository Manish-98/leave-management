package one.june.leave_management.domain.leave.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the duration type of a leave request.
 * FULL_DAY: Entire day(s)
 * FIRST_HALF: First half of a day (morning)
 * SECOND_HALF: Second half of a day (afternoon)
 */
@Schema(description = "Duration type of a leave request")
public enum LeaveDurationType {
    @Schema(description = "Full day leave (entire day(s))")
    FULL_DAY,
    @Schema(description = "First half of the day (morning)")
    FIRST_HALF,
    @Schema(description = "Second half of the day (afternoon)")
    SECOND_HALF
}
