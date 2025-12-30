package one.june.leave_management.application.leave.processor;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import one.june.leave_management.application.bulk.service.CsvResultService;
import one.june.leave_management.application.bulk.strategy.LeaveBulkUploadStrategy;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.SourceType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Processes bulk upload jobs for leaves.
 * This component contains the business logic for processing bulk uploads,
 * separate from the service layer to enable event-driven execution.
 */
@Component
@Slf4j
public class LeaveBulkUploadProcessor {

    private final LeaveService leaveService;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final BulkUploadRecordRepository bulkUploadRecordRepository;
    private final CsvResultService csvResultService;
    private final LeaveBulkUploadStrategy leaveBulkUploadStrategy;

    public LeaveBulkUploadProcessor(LeaveService leaveService,
                                    BulkUploadJobRepository bulkUploadJobRepository,
                                    BulkUploadRecordRepository bulkUploadRecordRepository,
                                    CsvResultService csvResultService,
                                    LeaveBulkUploadStrategy leaveBulkUploadStrategy) {
        this.leaveService = leaveService;
        this.bulkUploadJobRepository = bulkUploadJobRepository;
        this.bulkUploadRecordRepository = bulkUploadRecordRepository;
        this.csvResultService = csvResultService;
        this.leaveBulkUploadStrategy = leaveBulkUploadStrategy;
    }

    /**
     * Process bulk upload for a job.
     * Each row is processed in its own transaction to isolate failures.
     *
     * @param jobId   The ID of the bulk upload job
     * @param commands The list of leave ingestion commands
     * @param csvMetadata The list of CSV metadata maps (one per row)
     */
    public void processBulkUpload(UUID jobId, List<LeaveIngestionCommand> commands,
                                   List<Map<String, String>> csvMetadata) {
        log.info("Starting processing for leave bulk upload job {}", jobId);

        // Fetch the job (not in a transaction initially)
        BulkUploadJob job = bulkUploadJobRepository.findById(jobId)
                .orElseThrow(() -> new BulkUploadJobNotFoundException(jobId));

        try {
            // Process each row in its own transaction
            for (int i = 0; i < commands.size(); i++) {
                LeaveIngestionCommand command = commands.get(i);
                Map<String, String> metadata = csvMetadata.get(i);
                int rowNumber = i + 1; // 1-based row number

                // Re-fetch job for each row to ensure it's managed in the new transaction
                BulkUploadJob managedJob = bulkUploadJobRepository.findById(jobId)
                        .orElseThrow(() -> new BulkUploadJobNotFoundException(jobId));

                processSingleRow(managedJob, command, metadata, rowNumber);
            }

            // Update job status in separate transaction
            BulkUploadJob finalJob = bulkUploadJobRepository.findById(jobId)
                    .orElseThrow(() -> new BulkUploadJobNotFoundException(jobId));
            updateJobAsCompleted(finalJob);

        } catch (Exception e) {
            log.error("Failed to process leave bulk upload job {}", jobId, e);
            BulkUploadJob failedJob = bulkUploadJobRepository.findById(jobId)
                    .orElseThrow(() -> new BulkUploadJobNotFoundException(jobId));
            markJobAsFailed(failedJob);
        }
    }

    /**
     * Process a single row in its own transaction.
     * Uses REQUIRES_NEW to ensure each row commits independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processSingleRow(BulkUploadJob job, LeaveIngestionCommand command,
                                    Map<String, String> metadata, int rowNumber) {
        BulkUploadRecord record = BulkUploadRecord.builder()
                .job(job)
                .rowNumber(rowNumber)
                .metadata(metadata) // Use CSV metadata directly from parser
                .build();

        try {
            LeaveDto result = leaveService.ingest(command);
            record.setEntityId(result.getId());
            record.markAsSuccess();
            job.incrementSuccess();
        } catch (Exception e) {
            log.error("Failed to process row {} in job {}: {}", rowNumber, job.getId(), e.getMessage());
            record.markAsFailure(e.getMessage());
            job.incrementFailure();
        }

        bulkUploadRecordRepository.save(record);
    }

    /**
     * Update job as completed in a separate transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateJobAsCompleted(BulkUploadJob job) {
        job.markAsCompleted();
        bulkUploadJobRepository.save(job);

        // Generate result CSV file
        try {
            String resultFilePath = csvResultService.generateResultCsv(job.getId());
            job.setResultFilePath(resultFilePath);
            bulkUploadJobRepository.save(job);
        } catch (Exception e) {
            log.error("Failed to generate result CSV for job {}", job.getId(), e);
            // Don't fail the job - the processing was successful, only result generation failed
        }

        log.info("Completed leave bulk upload job {} - Success: {}, Failed: {}",
                job.getId(), job.getSuccessfulRecords(), job.getFailedRecords());
    }

    /**
     * Mark job as failed in a separate transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markJobAsFailed(BulkUploadJob job) {
        job.markAsFailed();
        bulkUploadJobRepository.save(job);
    }
}
