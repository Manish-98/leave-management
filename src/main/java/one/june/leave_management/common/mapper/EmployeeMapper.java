package one.june.leave_management.common.mapper;

import one.june.leave_management.adapter.persistence.jpa.entity.EmployeeJpaEntity;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.domain.employee.model.Employee;
import org.springframework.stereotype.Component;

/**
 * Unified mapper for Employee entity that handles mapping between all layers:
 * Domain ↔ Entity ↔ DTO
 */
@Component
public class EmployeeMapper {

    // Domain ↔ Entity mappings

    /**
     * Convert domain Employee to JPA entity
     */
    public EmployeeJpaEntity toJpaEntity(Employee employee) {
        if (employee == null) {
            return null;
        }

        return EmployeeJpaEntity.builder()
                .id(employee.getId())
                .name(employee.getName())
                .slackId(employee.getSlackId())
                .googleId(employee.getGoogleId())
                .slackDisplayName(employee.getSlackDisplayName())
                .dateOfJoining(employee.getDateOfJoining())
                .active(employee.isActive())
                .carryForwardLeaves(employee.getCarryForwardLeaves())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    /**
     * Convert JPA entity to domain Employee
     */
    public Employee toDomainEntity(EmployeeJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return Employee.builder()
                .id(jpaEntity.getId())
                .name(jpaEntity.getName())
                .slackId(jpaEntity.getSlackId())
                .googleId(jpaEntity.getGoogleId())
                .slackDisplayName(jpaEntity.getSlackDisplayName())
                .dateOfJoining(jpaEntity.getDateOfJoining())
                .active(jpaEntity.getActive())
                .carryForwardLeaves(jpaEntity.getCarryForwardLeaves())
                .createdAt(jpaEntity.getCreatedAt())
                .updatedAt(jpaEntity.getUpdatedAt())
                .build();
    }

    // Domain ↔ DTO mappings

    /**
     * Convert domain Employee to DTO
     */
    public EmployeeDto toDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        return EmployeeDto.builder()
                .id(employee.getId())
                .name(employee.getName())
                .slackId(employee.getSlackId())
                .googleId(employee.getGoogleId())
                .slackDisplayName(employee.getSlackDisplayName())
                .dateOfJoining(employee.getDateOfJoining())
                .active(employee.isActive())
                .carryForwardLeaves(employee.getCarryForwardLeaves())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    /**
     * Convert JPA entity directly to DTO (bypassing domain)
     * Useful for read operations to improve performance
     */
    public EmployeeDto toDtoFromJpa(EmployeeJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return EmployeeDto.builder()
                .id(jpaEntity.getId())
                .name(jpaEntity.getName())
                .slackId(jpaEntity.getSlackId())
                .googleId(jpaEntity.getGoogleId())
                .slackDisplayName(jpaEntity.getSlackDisplayName())
                .dateOfJoining(jpaEntity.getDateOfJoining())
                .active(jpaEntity.getActive())
                .carryForwardLeaves(jpaEntity.getCarryForwardLeaves())
                .createdAt(jpaEntity.getCreatedAt())
                .updatedAt(jpaEntity.getUpdatedAt())
                .build();
    }

    /**
     * Update JPA entity from domain Employee
     * Used for updates to preserve created_at timestamp
     */
    public void updateJpaEntityFromDomain(Employee employee, EmployeeJpaEntity jpaEntity) {
        if (employee == null || jpaEntity == null) {
            return;
        }

        jpaEntity.setName(employee.getName());
        jpaEntity.setSlackId(employee.getSlackId());
        jpaEntity.setGoogleId(employee.getGoogleId());
        jpaEntity.setSlackDisplayName(employee.getSlackDisplayName());
        jpaEntity.setDateOfJoining(employee.getDateOfJoining());
        jpaEntity.setActive(employee.isActive());
        jpaEntity.setCarryForwardLeaves(employee.getCarryForwardLeaves());
        // updatedAt is automatically handled by @PreUpdate
    }
}
