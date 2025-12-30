package one.june.leave_management.adapter.persistence.jpa.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import one.june.leave_management.adapter.persistence.jpa.converter.CarryForwardLeavesConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "employee")
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slack_id", unique = true)
    private String slackId;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "slack_display_name")
    private String slackDisplayName;

    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "carry_forward_leaves", columnDefinition = "TEXT")
    @Convert(converter = CarryForwardLeavesConverter.class)
    @Builder.Default
    private Map<Integer, Integer> carryForwardLeaves = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Initialize defaults if null
        if (active == null) {
            active = true;
        }
        if (carryForwardLeaves == null) {
            carryForwardLeaves = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if employee has any external ID set
     */
    public boolean hasExternalId() {
        return (slackId != null && !slackId.trim().isEmpty()) ||
               (googleId != null && !googleId.trim().isEmpty());
    }

    /**
     * Override setter for carryForwardLeaves to ensure defensive copying
     */
    public void setCarryForwardLeaves(Map<Integer, Integer> carryForwardLeaves) {
        this.carryForwardLeaves = carryForwardLeaves != null ? new HashMap<>(carryForwardLeaves) : new HashMap<>();
    }

    /**
     * Get carry forward leaves with defensive copying
     */
    public Map<Integer, Integer> getCarryForwardLeaves() {
        return carryForwardLeaves != null ? new HashMap<>(carryForwardLeaves) : new HashMap<>();
    }

    /**
     * Check if employee is active
     */
    public boolean isActiveEmployee() {
        return active != null && active;
    }
}
