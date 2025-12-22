package one.june.leave_management.adapter.persistence.jpa.entity;

import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "leave")
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class LeaveJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LeaveType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type", nullable = false)
    @Builder.Default
    private LeaveDurationType durationType = LeaveDurationType.FULL_DAY;

    @OneToMany(mappedBy = "leave", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<LeaveSourceRefJpaEntity> sourceRefs = new ArrayList<>();

    // Helper methods
    public void addSourceRef(LeaveSourceRefJpaEntity sourceRef) {
        sourceRefs.add(sourceRef);
        sourceRef.setLeave(this);
    }

    public void removeSourceRef(LeaveSourceRefJpaEntity sourceRef) {
        sourceRefs.remove(sourceRef);
        sourceRef.setLeave(null);
    }

    // Override setter for defensive copying
    public void setSourceRefs(List<LeaveSourceRefJpaEntity> sourceRefs) {
        this.sourceRefs = sourceRefs != null ? sourceRefs : new ArrayList<>();
    }
}