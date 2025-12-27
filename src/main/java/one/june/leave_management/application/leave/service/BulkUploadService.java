package one.june.leave_management.application.leave.service;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.web.csv.CsvLeaveParser;
import one.june.leave_management.adapter.inbound.web.csv.CsvValidationException;
import one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadJob.BulkUploadStatus;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class BulkUploadService {

    @Autowired
    private CsvLeaveParser csvLeaveParser;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private BulkUploadJobRepository bulkUploadJobRepository;

    /**
     * Initiate bulk upload process
     *
     * @param file CSV file to upload
     * @return BulkUploadResponse with jobId
     */
    @Transactional
    public BulkUploadResponse initiateBulkUpload(MultipartFile file) {
        // Validate file
        validateFile(file);

        // Generate job ID
        UUID jobId = UUID.randomUUID();

        // Parse CSV to get commands and total count
        List<LeaveIngestionCommand> commands;
        try {
            commands = csvLeaveParser.parse(file, jobId.toString());
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse CSV file: " + e.getMessage(), e);
        }

        // Create job entity
        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .status(BulkUploadStatus.PROCESSING)
                .totalRecords(commands.size())
                .successfulRecords(0)
                .failedRecords(0)
                .fileName(file.getOriginalFilename())
                .build();

        bulkUploadJobRepository.save(job);

        log.info("Created bulk upload job {} with {} records", jobId, commands.size());

        // Trigger async processing
        processBulkUploadAsync(job, commands);

        // Return immediate response
        return BulkUploadResponse.builder()
                .jobId(jobId)
                .status(job.getStatus().name())
                .totalRecords(job.getTotalRecords())
                .successfulRecords(0)
                .failedRecords(0)
                .resultAvailable(false)
                .build();
    }

    /**
     * Process bulk upload asynchronously
     */
    @Async("taskExecutor")
    protected void processBulkUploadAsync(BulkUploadJob job, List<LeaveIngestionCommand> commands) {
        log.info("Starting async processing for job {}", job.getId());

        try {
            leaveService.bulkIngestAsync(job, commands);
        } catch (Exception e) {
            log.error("Failed to process bulk upload job {}", job.getId(), e);
            job.markAsFailed();
            bulkUploadJobRepository.save(job);
        }
    }

    /**
     * Get job status
     */
    @Transactional(readOnly = true)
    public BulkUploadResponse getJobStatus(UUID jobId) {
        BulkUploadJob job = bulkUploadJobRepository.findById(jobId)
                .orElseThrow(() -> new BulkUploadJobNotFoundException(jobId));

        return BulkUploadResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus().name())
                .totalRecords(job.getTotalRecords())
                .successfulRecords(job.getSuccessfulRecords())
                .failedRecords(job.getFailedRecords())
                .resultAvailable(job.getResultFilePath() != null)
                .build();
    }

    /**
     * Get result file path for a job
     */
    @Transactional(readOnly = true)
    public String getResultFilePath(UUID jobId) {
        BulkUploadJob job = bulkUploadJobRepository.findById(jobId)
                .orElseThrow(() -> new BulkUploadJobNotFoundException(jobId));

        if (job.getResultFilePath() == null) {
            throw new IllegalStateException("Result file not available for job " + jobId);
        }

        return job.getResultFilePath();
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are allowed");
        }

        // Check file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
    }
}
