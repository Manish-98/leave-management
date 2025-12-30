package one.june.leave_management.application.employee.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCreateCommand {
    private UUID id; // Optional - if provided, it's an update
    private String name;
    private String slackId;
    private String googleId;
    private String slackDisplayName;
    private LocalDate dateOfJoining;
    private Boolean active;
    private Map<Integer, Integer> carryForwardLeaves;
}
