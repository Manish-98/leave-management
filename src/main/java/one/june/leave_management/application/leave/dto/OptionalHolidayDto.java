package one.june.leave_management.application.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * DTO for OptionalHoliday.
 * Used in application layer for data transfer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = {"id", "date"})
public class OptionalHolidayDto {

    private Long id;
    private LocalDate date;
    private String name;
    private String description;
}
