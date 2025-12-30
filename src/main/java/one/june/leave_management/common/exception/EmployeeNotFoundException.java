package one.june.leave_management.common.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Exception thrown when an employee cannot be found by their ID.
 */
@Getter
public class EmployeeNotFoundException extends RuntimeException {

    private final UUID employeeId;

    public EmployeeNotFoundException(UUID employeeId) {
        super(String.format("Employee not found with ID: %s", employeeId));
        this.employeeId = employeeId;
    }

    public EmployeeNotFoundException(String message, UUID employeeId) {
        super(message);
        this.employeeId = employeeId;
    }
}
