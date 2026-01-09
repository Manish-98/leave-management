package one.june.leave_management.domain.employee.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import one.june.leave_management.domain.common.model.Region;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
public class Employee {
    private UUID id;
    private String name;
    private String slackId;
    private String googleId;
    private String slackDisplayName;
    private LocalDate dateOfJoining;
    @Builder.Default
    private Boolean active = true;
    private Region region;
    @Builder.Default
    private Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Static factory method for creating an employee with validation
     *
     * @param name           Employee's full name (required)
     * @param slackId        Slack ID (optional)
     * @param googleId       Google ID (optional)
     * @param slackDisplayName Slack display name (optional)
     * @param dateOfJoining  Date of joining (required)
     * @param region         Employee's region (required)
     * @return Created and validated Employee instance
     */
    public static Employee create(String name, String slackId, String googleId,
                                  String slackDisplayName, LocalDate dateOfJoining, Region region) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (dateOfJoining == null) {
            throw new IllegalArgumentException("dateOfJoining cannot be null");
        }
        if (region == null) {
            throw new IllegalArgumentException("region cannot be null");
        }

        Employee employee = Employee.builder()
                .name(name.trim())
                .slackId(slackId != null ? slackId.trim() : null)
                .googleId(googleId != null ? googleId.trim() : null)
                .slackDisplayName(slackDisplayName != null ? slackDisplayName.trim() : null)
                .dateOfJoining(dateOfJoining)
                .active(true)
                .region(region)
                .carryForwardLeaves(new HashMap<>())
                .build();

        employee.validate();
        return employee;
    }

    /**
     * Update employee information
     *
     * @param name           New name
     * @param slackId        New slack ID
     * @param googleId       New google ID
     * @param slackDisplayName New slack display name
     * @param dateOfJoining  New date of joining
     * @param region         New region
     */
    public void update(String name, String slackId, String googleId,
                      String slackDisplayName, LocalDate dateOfJoining, Region region) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (dateOfJoining == null) {
            throw new IllegalArgumentException("dateOfJoining cannot be null");
        }
        if (region == null) {
            throw new IllegalArgumentException("region cannot be null");
        }

        this.name = name.trim();

        this.slackId = slackId != null ? slackId.trim() : null;
        this.googleId = googleId != null ? googleId.trim() : null;
        this.slackDisplayName = slackDisplayName != null ? slackDisplayName.trim() : null;
        this.dateOfJoining = dateOfJoining;
        this.region = region;

        validate();
    }

    /**
     * Update carry forward leaves for a specific year
     *
     * @param year  Year to update
     * @param days  Number of carry forward days
     */
    public void updateCarryForwardLeaves(Integer year, Integer days) {
        if (year == null) {
            throw new IllegalArgumentException("year cannot be null");
        }
        if (days == null) {
            throw new IllegalArgumentException("days cannot be null");
        }

        if (days < 0) {
            throw new IllegalArgumentException("Carry forward leaves cannot be negative");
        }

        if (days == 0) {
            carryForwardLeaves.remove(year);
        } else {
            carryForwardLeaves.put(year, days);
        }
    }

    /**
     * Get carry forward leaves for a specific year
     *
     * @param year Year to query
     * @return Number of carry forward days, or 0 if not set
     */
    public int getCarryForwardLeavesForYear(Integer year) {
        if (year == null) {
            throw new IllegalArgumentException("year cannot be null");
        }
        return carryForwardLeaves.getOrDefault(year, 0);
    }

    /**
     * Deactivate employee (soft delete)
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Activate employee
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Check if employee has a specific external ID
     *
     * @param slackId  Slack ID to check
     * @param googleId Google ID to check
     * @return true if employee has at least one matching external ID
     */
    public boolean hasMatchingExternalId(String slackId, String googleId) {
        if (slackId != null && slackId.equals(this.slackId)) {
            return true;
        }
        if (googleId != null && googleId.equals(this.googleId)) {
            return true;
        }
        return false;
    }

    /**
     * Validation method - can be called explicitly
     */
    public void validate() {
        // Name validation
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }

        // Date of joining validation
        if (dateOfJoining == null) {
            throw new IllegalArgumentException("dateOfJoining cannot be null");
        }

        if (dateOfJoining.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dateOfJoining cannot be in the future");
        }

        // Region validation
        if (region == null) {
            throw new IllegalArgumentException("region cannot be null");
        }

        // Carry forward leaves validation
        if (carryForwardLeaves != null) {
            carryForwardLeaves.forEach((year, days) -> {
                if (year == null) {
                    throw new IllegalArgumentException("Carry forward leaves year cannot be null");
                }
                if (days == null || days < 0) {
                    throw new IllegalArgumentException("Carry forward leaves days cannot be null or negative");
                }
            });
        }
    }

    /**
     * Custom getter to return defensive copy of carry forward leaves
     */
    public Map<Integer, Integer> getCarryForwardLeaves() {
        return carryForwardLeaves != null ? new HashMap<>(carryForwardLeaves) : new HashMap<>();
    }

    /**
     * Override setter to create defensive copy
     */
    public void setCarryForwardLeaves(Map<Integer, Integer> carryForwardLeaves) {
        this.carryForwardLeaves = carryForwardLeaves != null ? new HashMap<>(carryForwardLeaves) : new HashMap<>();
    }

    /**
     * Check if employee is active
     */
    public boolean isActive() {
        return active != null && active;
    }
}
