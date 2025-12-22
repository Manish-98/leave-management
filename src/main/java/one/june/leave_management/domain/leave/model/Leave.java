package one.june.leave_management.domain.leave.model;

import one.june.leave_management.common.model.DateRange;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
public class Leave {
    private UUID id;
    private String userId;
    private DateRange dateRange;
    private LeaveType type;
    private LeaveStatus status;
    @Builder.Default
    private LeaveDurationType durationType = LeaveDurationType.FULL_DAY;
    @ToString.Exclude
    @Builder.Default
    private List<LeaveSourceRef> sourceRefs = new ArrayList<>();

    // Static factory method for business logic with validation
    public static Leave create(String userId, LocalDate startDate, LocalDate endDate, LeaveType type, LeaveStatus status) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(startDate, "startDate cannot be null");
        Objects.requireNonNull(endDate, "endDate cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(status, "status cannot be null");

        DateRange dateRange = DateRange.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Leave leave = Leave.builder()
                .userId(userId)
                .dateRange(dateRange)
                .type(type)
                .status(status)
                .sourceRefs(new ArrayList<>())
                .build();
        leave.validate();
        return leave;
    }

    public void addSourceRef(LeaveSourceRef sourceRef) {
        Objects.requireNonNull(sourceRef, "sourceRef cannot be null");
        if (!sourceRefs.contains(sourceRef)) {
            sourceRefs.add(sourceRef);
        }
    }

    public void removeSourceRef(LeaveSourceRef sourceRef) {
        sourceRefs.remove(sourceRef);
    }

    public boolean hasSourceRefs() {
        return !sourceRefs.isEmpty();
    }

    public void update(String userId, LocalDate startDate, LocalDate endDate, LeaveType type, LeaveStatus status) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");

        this.dateRange = DateRange.builder()
                .startDate(Objects.requireNonNull(startDate, "startDate cannot be null"))
                .endDate(Objects.requireNonNull(endDate, "endDate cannot be null"))
                .build();

        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");

        validate();
    }

    // Convenience getters for backward compatibility
    public LocalDate getStartDate() {
        return dateRange != null ? dateRange.getStartDate() : null;
    }

    public LocalDate getEndDate() {
        return dateRange != null ? dateRange.getEndDate() : null;
    }

    // Custom getter to return defensive copy
    public List<LeaveSourceRef> getSourceRefs() {
        return new ArrayList<>(sourceRefs);
    }

    // Override setter to validate and create defensive copy
    public void setSourceRefs(List<LeaveSourceRef> sourceRefs) {
        this.sourceRefs = sourceRefs != null ? new ArrayList<>(sourceRefs) : new ArrayList<>();
    }

    // Validation method - can be called explicitly
    void validate() {
        if (dateRange != null) {
            LocalDate startDate = dateRange.getStartDate();
            LocalDate endDate = dateRange.getEndDate();
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("startDate cannot be after endDate");
            }

            // Validate half-day leaves
            if (durationType != null && durationType != LeaveDurationType.FULL_DAY) {
                if (startDate == null || endDate == null || !startDate.equals(endDate)) {
                    throw new IllegalArgumentException("Half-day leaves must have the same start and end date");
                }
            }
        }
    }
}