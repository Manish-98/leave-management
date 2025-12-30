package one.june.leave_management.domain.leave.model;

/**
 * Enum representing the type of bulk upload job.
 * Used to categorize and route different bulk upload operations.
 */
public enum BulkUploadType {
    /**
     * Leave bulk upload from CSV
     */
    LEAVE,

    /**
     * Employee bulk upload from CSV
     */
    EMPLOYEE
}
