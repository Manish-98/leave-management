package one.june.leave_management.common.exception;

import java.util.UUID;

public class BulkUploadJobNotFoundException extends RuntimeException {

    public BulkUploadJobNotFoundException(UUID jobId) {
        super(String.format("Bulk upload job not found: %s", jobId));
    }

    public BulkUploadJobNotFoundException(String message) {
        super(message);
    }
}
