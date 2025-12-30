package one.june.leave_management.common.event;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.application.employee.command.EmployeeCreateCommand;
import one.june.leave_management.application.employee.processor.EmployeeBulkUploadProcessor;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.processor.LeaveBulkUploadProcessor;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Event listener for domain events.
 * Uses @TransactionalEventListener to ensure events are only processed after the transaction commits,
 * and @Async to process events in a separate thread with a new transaction.
 */
@Component
@Slf4j
public class EntityEventListener {

    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final EmployeeBulkUploadProcessor employeeBulkUploadProcessor;
    private final LeaveBulkUploadProcessor leaveBulkUploadProcessor;

    public EntityEventListener(BulkUploadJobRepository bulkUploadJobRepository,
                               EmployeeBulkUploadProcessor employeeBulkUploadProcessor,
                               LeaveBulkUploadProcessor leaveBulkUploadProcessor) {
        this.bulkUploadJobRepository = bulkUploadJobRepository;
        this.employeeBulkUploadProcessor = employeeBulkUploadProcessor;
        this.leaveBulkUploadProcessor = leaveBulkUploadProcessor;
    }

    /**
     * Handle bulk upload job created events.
     * This method is executed asynchronously AFTER the transaction commits.
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBulkUploadJobCreated(EntityEvent event) {
        if (event.getEventType() != EventType.BULK_UPLOAD_JOB_CREATED) {
            return;
        }

        if (event.getEntityType() != EntityType.BULK_UPLOAD_JOB) {
            return;
        }

        log.info("Received BULK_UPLOAD_JOB_CREATED event for entity {}", event.getEntityId());

        // Fetch the job to determine the type
        bulkUploadJobRepository.findById(event.getEntityId()).ifPresent(job -> {
            // Extract commands and csvMetadata from metadata
            @SuppressWarnings("unchecked")
            List<?> commands = (List<?>) event.getMetadata().get("commands");

            @SuppressWarnings("unchecked")
            List<java.util.Map<String, String>> csvMetadata = (List<java.util.Map<String, String>>) event.getMetadata().get("csvMetadata");

            if (job.getType() == BulkUploadType.EMPLOYEE) {
                log.info("Processing employee bulk upload job {}", job.getId());
                @SuppressWarnings("unchecked")
                List<EmployeeCreateCommand> employeeCommands = (List<EmployeeCreateCommand>) commands;
                employeeBulkUploadProcessor.processBulkUpload(job.getId(), employeeCommands, csvMetadata);
            } else if (job.getType() == BulkUploadType.LEAVE) {
                log.info("Processing leave bulk upload job {}", job.getId());
                @SuppressWarnings("unchecked")
                List<LeaveIngestionCommand> leaveCommands = (List<LeaveIngestionCommand>) commands;
                leaveBulkUploadProcessor.processBulkUpload(job.getId(), leaveCommands, csvMetadata);
            } else {
                log.warn("Unknown bulk upload type: {} for job {}", job.getType(), job.getId());
            }
        });
    }
}
