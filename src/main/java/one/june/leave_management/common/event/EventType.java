package one.june.leave_management.common.event;

/**
 * Types of domain events that can be published.
 */
public enum EventType {
    /**
     * Bulk upload job created and ready for processing
     */
    BULK_UPLOAD_JOB_CREATED,

    /**
     * Bulk upload job completed
     */
    BULK_UPLOAD_JOB_COMPLETED,

    /**
     * Bulk upload job failed
     */
    BULK_UPLOAD_JOB_FAILED
}
