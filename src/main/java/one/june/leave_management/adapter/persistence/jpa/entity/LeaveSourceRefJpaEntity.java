package one.june.leave_management.adapter.persistence.jpa.entity;

import one.june.leave_management.domain.leave.model.SourceType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "leave_source_ref",
       uniqueConstraints = @UniqueConstraint(columnNames = {"source_type", "source_id"}))
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = {"sourceType", "sourceId"})
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LeaveSourceRefJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_id", nullable = false, columnDefinition = "UUID")
    @ToString.Exclude
    private LeaveJpaEntity leave;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private String sourceId;
}