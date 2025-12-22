package one.june.leave_management.domain.leave.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode(of = {"sourceType", "sourceId"})
@ToString(exclude = {"leaveId"})
@NoArgsConstructor
@AllArgsConstructor
public class LeaveSourceRef {
    private UUID id;
    private SourceType sourceType;
    private String sourceId;

    // Transient reference to avoid circular issues in domain model
    // The actual relationship is maintained at the JPA level
    private transient UUID leaveId;
}