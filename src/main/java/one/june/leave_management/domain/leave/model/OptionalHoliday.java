package one.june.leave_management.domain.leave.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Domain entity representing a predefined optional holiday.
 * These holidays can be selected by users when applying for optional holiday leave.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = {"id", "date"})
public class OptionalHoliday {

    private UUID id;
    private LocalDate date;
    private String name;
    private String description;
}
