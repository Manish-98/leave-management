package one.june.leave_management.application.leave.service;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.web.csv.CsvLeaveParserStrategy;
import one.june.leave_management.adapter.inbound.web.csv.ParsedResult;
import one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.common.event.EntityEvent;
import one.june.leave_management.common.event.EntityType;
import one.june.leave_management.common.event.EventType;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadJob.BulkUploadStatus;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BulkUploadService {

    private final CsvLeaveParserStrategy csvLeaveParserStrategy;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkUploadService(CsvLeaveParserStrategy csvLeaveParserStrategy,
                             BulkUploadJobRepository bulkUploadJobRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.csvLeaveParserStrategy = csvLeaveParserStrategy;
        this.bulkUploadJobRepository = bulkUploadJobRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Initiate bulk upload process for leaves
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

        // Parse CSV to get parsed results (commands + metadata)
        List<ParsedResult<LeaveIngestionCommand>> parsedResults;
        try {
            parsedResults = csvLeaveParserStrategy.parse(file, jobId.toString());
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse CSV file: " + e.getMessage(), e);
        }

        // Extract commands from parsed results
        List<LeaveIngestionCommand> commands = parsedResults.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

        // Extract metadata from parsed results
        List<Map<String, String>> metadataList = parsedResults.stream()
                .map(ParsedResult::getCsvMetadata)
                .collect(Collectors.toList());

        // Create job entity with type LEAVE
        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .status(BulkUploadStatus.PROCESSING)
                .totalRecords(commands.size())
                .successfulRecords(0)
                .failedRecords(0)
                .fileName(file.getOriginalFilename())
                .build();

        bulkUploadJobRepository.save(job);
        bulkUploadJobRepository.flush(); // Ensure job is persisted before event is published

        log.info("Created bulk upload job {} with {} records", jobId, commands.size());

        // Publish event to trigger async processing after transaction commits
        // Include both commands and metadata for processing
        EntityEvent event = EntityEvent.builder()
                .eventType(EventType.BULK_UPLOAD_JOB_CREATED)
                .entityType(EntityType.BULK_UPLOAD_JOB)
                .entityId(jobId)
                .metadata(Map.of(
                        "commands", commands,
                        "csvMetadata", metadataList
                ))
                .build();
        eventPublisher.publishEvent(event);

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
