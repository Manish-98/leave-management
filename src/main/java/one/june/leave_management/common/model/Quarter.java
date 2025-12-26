package one.june.leave_management.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Represents a quarter of the year with corresponding month ranges.
 * Used for filtering leaves by quarterly periods.
 */
@Getter
@Schema(description = "Quarter of the year for filtering leave requests")
public enum Quarter {
    @Schema(description = "First Quarter (January-March)")
    Q1(1, 3, "First Quarter (January-March)"),
    @Schema(description = "Second Quarter (April-June)")
    Q2(4, 6, "Second Quarter (April-June)"),
    @Schema(description = "Third Quarter (July-September)")
    Q3(7, 9, "Third Quarter (July-September)"),
    @Schema(description = "Fourth Quarter (October-December)")
    Q4(10, 12, "Fourth Quarter (October-December)");

    private final int startMonth;
    private final int endMonth;
    private final String description;

    Quarter(int startMonth, int endMonth, String description) {
        this.startMonth = startMonth;
        this.endMonth = endMonth;
        this.description = description;
    }

    /**
     * Converts a quarter number (1-4) to Quarter enum.
     *
     * @param quarterNumber the quarter number (1 for Q1, 2 for Q2, etc.)
     * @return the corresponding Quarter enum
     * @throws IllegalArgumentException if quarterNumber is not between 1 and 4
     */
    public static Quarter fromQuarterNumber(int quarterNumber) {
        if (quarterNumber < 1 || quarterNumber > 4) {
            throw new IllegalArgumentException("Quarter number must be between 1 and 4, got: " + quarterNumber);
        }
        return values()[quarterNumber - 1];
    }
}
