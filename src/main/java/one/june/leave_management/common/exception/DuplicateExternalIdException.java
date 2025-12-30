package one.june.leave_management.common.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Exception thrown when an external ID (Slack ID or Google ID) already exists for another employee.
 */
@Getter
public class DuplicateExternalIdException extends RuntimeException {

    private final String externalIdType;
    private final String externalIdValue;
    private final UUID employeeId;

    public DuplicateExternalIdException(String externalIdType, String externalIdValue, UUID employeeId) {
        super(String.format("An employee with %s '%s' already exists (Employee ID: %s)",
                externalIdType, externalIdValue, employeeId != null ? employeeId : "new"));
        this.externalIdType = externalIdType;
        this.externalIdValue = externalIdValue;
        this.employeeId = employeeId;
    }

    public DuplicateExternalIdException(String message, String externalIdType, String externalIdValue, UUID employeeId) {
        super(message);
        this.externalIdType = externalIdType;
        this.externalIdValue = externalIdValue;
        this.employeeId = employeeId;
    }
}
