package one.june.leave_management.domain.employee.service;

import one.june.leave_management.common.exception.DuplicateExternalIdException;
import one.june.leave_management.domain.employee.model.Employee;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Domain service for Employee business logic validation.
 * Contains cross-entity business rules and validation logic.
 */
@Service
public class EmployeeDomainService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeDomainService.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeDomainService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Validates that external IDs (Slack and Google) are unique across all employees.
     * For new employees, checks if any employee with the same external IDs exists.
     * For existing employees, excludes the current employee from the uniqueness check.
     *
     * @param employee the employee to validate
     * @throws DuplicateExternalIdException if external IDs conflict with existing employees
     */
    public void validateExternalIdUniqueness(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null");
        }

        logger.debug("Validating external ID uniqueness for employee: {}",
                    employee.getId() != null ? employee.getId() : "new employee");

        // Check Slack ID uniqueness
        if (employee.getSlackId() != null && !employee.getSlackId().trim().isEmpty()) {
            boolean slackIdExists;
            if (employee.getId() != null) {
                // For updates, exclude current employee
                slackIdExists = employeeRepository.existsBySlackIdAndIdNot(
                        employee.getSlackId(), employee.getId());
            } else {
                // For new employees, check all employees
                slackIdExists = employeeRepository.existsBySlackId(employee.getSlackId());
            }

            if (slackIdExists) {
                logger.warn("Duplicate Slack ID found: {}", employee.getSlackId());
                throw new DuplicateExternalIdException(
                        "slackId",
                        employee.getSlackId(),
                        employee.getId()
                );
            }
        }

        // Check Google ID uniqueness
        if (employee.getGoogleId() != null && !employee.getGoogleId().trim().isEmpty()) {
            boolean googleIdExists;
            if (employee.getId() != null) {
                // For updates, exclude current employee
                googleIdExists = employeeRepository.existsByGoogleIdAndIdNot(
                        employee.getGoogleId(), employee.getId());
            } else {
                // For new employees, check all employees
                googleIdExists = employeeRepository.existsByGoogleId(employee.getGoogleId());
            }

            if (googleIdExists) {
                logger.warn("Duplicate Google ID found: {}", employee.getGoogleId());
                throw new DuplicateExternalIdException(
                        "googleId",
                        employee.getGoogleId(),
                        employee.getId()
                );
            }
        }

        logger.debug("External ID uniqueness validation passed for employee: {}",
                    employee.getId() != null ? employee.getId() : "new employee");
    }

    /**
     * Validates employee data before persistence.
     * Performs comprehensive validation including external ID uniqueness.
     *
     * @param employee the employee to validate
     */
    public void validateEmployeeForPersistence(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null");
        }

        logger.debug("Validating employee for persistence: {}",
                    employee.getId() != null ? employee.getId() : "new employee");

        // Validate domain invariants
        employee.validate();

        // Validate external ID uniqueness
        validateExternalIdUniqueness(employee);

        logger.debug("Employee validation passed for: {}",
                    employee.getId() != null ? employee.getId() : "new employee");
    }
}
