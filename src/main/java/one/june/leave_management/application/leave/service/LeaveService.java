package one.june.leave_management.application.leave.service;

import one.june.leave_management.adapter.inbound.web.dto.LeaveFetchQuery;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.common.annotation.Auditable;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveFilters;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.LeaveSourceRefRepository;
import one.june.leave_management.domain.leave.service.LeaveDomainService;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Unified service for leave operations.
 * Handles both leave ingestion (create/update) and fetching with filters.
 */
@Service
public class LeaveService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRepository leaveRepository;
    private final LeaveSourceRefRepository leaveSourceRefRepository;
    private final OutboundSyncService outboundSyncService;
    private final LeaveDomainService leaveDomainService;
    private final LeaveMapper leaveMapper;
    private final BulkUploadRecordRepository bulkUploadRecordRepository;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final CsvResultService csvResultService;

    public LeaveService(LeaveRepository leaveRepository,
                        LeaveSourceRefRepository leaveSourceRefRepository,
                        OutboundSyncService outboundSyncService,
                        LeaveDomainService leaveDomainService,
                        LeaveMapper leaveMapper,
                        BulkUploadRecordRepository bulkUploadRecordRepository,
                        BulkUploadJobRepository bulkUploadJobRepository,
                        CsvResultService csvResultService) {
        this.leaveRepository = leaveRepository;
        this.leaveSourceRefRepository = leaveSourceRefRepository;
        this.outboundSyncService = outboundSyncService;
        this.leaveDomainService = leaveDomainService;
        this.leaveMapper = leaveMapper;
        this.bulkUploadRecordRepository = bulkUploadRecordRepository;
        this.bulkUploadJobRepository = bulkUploadJobRepository;
        this.csvResultService = csvResultService;
    }

    /**
     * Ingest a leave request (create or update).
     * Creates a new leave or updates an existing one based on source type and source ID.
     *
     * @param command the leave ingestion command
     * @return the created or updated leave DTO
     */
    @Transactional
    public LeaveDto ingest(LeaveIngestionCommand command) {
        logger.info("Ingesting leave: {}", command);

        Optional<LeaveSourceRef> existingSourceRef = leaveSourceRefRepository
                .findBySourceTypeAndSourceIdWithLeave(command.getSourceType(), command.getSourceId());

        Leave leave = existingSourceRef
                .map(sourceRef -> updateExistingLeave(command, sourceRef))
                .orElseGet(() -> createNewLeave(command));

        existingSourceRef.orElseGet(() -> createSourceReference(command, leave));

        leaveDomainService.validateLeaveForPersistence(leave);
        leaveDomainService.validateNoOverlappingLeaves(leave);
        Leave savedLeave = leaveRepository.save(leave);
        performOutboundSync(savedLeave, command.getSourceType());

        logger.info("Successfully ingested leave: {}", leave);
        return leaveMapper.toDto(savedLeave);
    }

    /**
     * Fetch leaves based on the provided filter criteria with pagination support.
     * All filters are optional - if no filters are provided, returns all leaves.
     *
     * @param query the filter query containing optional userId, year, and quarter
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of leaves matching the filter criteria
     * @throws IllegalArgumentException if quarter is provided without year
     */
    @Auditable
    @Transactional(readOnly = true)
    public Page<LeaveDto> fetchLeaves(LeaveFetchQuery query, Pageable pageable) {
        logger.info("Fetching leaves with query: {} and pageable: {}", query, pageable);

        // Validate the query parameters
        query.validate();

        // Convert query to domain filters
        LeaveFilters filters = convertToFilters(query);

        // Fetch leaves from repository
        Page<Leave> leavesPage = leaveRepository.findByFilters(filters, pageable);

        // Convert to DTOs
        Page<LeaveDto> dtoPage = leavesPage.map(leaveMapper::toDto);

        logger.info("Successfully fetched {} leaves (page {} of {})",
                    dtoPage.getNumberOfElements(),
                    dtoPage.getNumber() + 1,
                    dtoPage.getTotalPages());

        return dtoPage;
    }

    /**
     * Convert LeaveFetchQuery to LeaveFilters domain model.
     * Extracts quarter start/end months if quarter is provided.
     *
     * @param query the web layer query object
     * @return the domain layer filters object
     */
    private LeaveFilters convertToFilters(LeaveFetchQuery query) {
        LeaveFilters.LeaveFiltersBuilder builder = LeaveFilters.builder();

        // User ID filter
        if (query.getUserId() != null && !query.getUserId().isBlank()) {
            builder.userId(query.getUserId());
        }

        // Year filter
        if (query.getYear() != null) {
            builder.year(query.getYear());
        }

        // Quarter filter - extract start and end months
        if (query.getQuarter() != null) {
            builder.startMonth(query.getQuarter().getStartMonth());
            builder.endMonth(query.getQuarter().getEndMonth());
        }

        return builder.build();
    }

    private Leave createNewLeave(LeaveIngestionCommand command) {
        logger.debug("Creating new leave from command");
        return Leave.builder()
                .userId(command.getUserId())
                .dateRange(command.getDateRange())
                .type(command.getType())
                .status(command.getStatus())
                .durationType(command.getDurationType())
                .build();
    }

    private Leave updateExistingLeave(LeaveIngestionCommand command, LeaveSourceRef sourceRef) {
        logger.debug("Updating existing leave for source reference: {}", sourceRef);

        Leave leave = findOrCreateLeaveFromSourceRef(sourceRef);
        leave.update(
                command.getUserId(),
                command.getStartDate(),
                command.getEndDate(),
                command.getType(),
                command.getStatus()
        );
        return leave;
    }

    private LeaveSourceRef createSourceReference(LeaveIngestionCommand command, Leave leave) {
        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .sourceType(command.getSourceType())
                .sourceId(command.getSourceId())
                .build();
        leave.addSourceRef(sourceRef);
        return sourceRef;
    }

    private void performOutboundSync(Leave leave, SourceType sourceType) {
        // Skip outbound sync for bulk CSV uploads
        if (sourceType == SourceType.CSV_BULK) {
            logger.info("Skipping outbound sync for CSV bulk upload leave {}", leave.getId());
            return;
        }

        try {
            outboundSyncService.sync(leave, sourceType);
            logger.info("Successfully synced leave {} to external systems", leave.getId());
        } catch (Exception e) {
            logger.error("Failed to sync leave {} to external systems", leave.getId(), e);
            // Continue with the response even if sync fails - this is a business decision
            // In production, consider adding monitoring/alerting for sync failures
        }
    }

    private Leave findOrCreateLeaveFromSourceRef(LeaveSourceRef sourceRef) {
        logger.debug("Finding leave for source reference: {}", sourceRef);

        // The sourceRef should already have the leave ID populated from the repository query
        if (sourceRef.getLeaveId() == null) {
            logger.error("Source reference {} has no associated leave ID", sourceRef);
            throw new IllegalStateException("Source reference exists but has no associated leave ID: " + sourceRef);
        }

        // Find the associated leave
        Optional<Leave> existingLeave = leaveRepository.findById(sourceRef.getLeaveId());

        if (existingLeave.isPresent()) {
            logger.debug("Found existing leave {} for source reference: {}", existingLeave.get(), sourceRef);
            return existingLeave.get();
        } else {
            // This is a data inconsistency - source reference points to non-existent leave
            logger.error("Source reference {} points to non-existent leave ID: {}", sourceRef, sourceRef.getLeaveId());
            throw new IllegalStateException("Source reference points to non-existent leave: " + sourceRef.getLeaveId());
        }
    }

    /**
     * Bulk ingest leaves from CSV upload asynchronously
     * Processes each leave individually, continuing on errors
     *
     * @param job the bulk upload job entity
     * @param commands list of leave ingestion commands from CSV
     */
    @Async("taskExecutor")
    @Transactional
    public void bulkIngestAsync(BulkUploadJob job, List<LeaveIngestionCommand> commands) {
        logger.info("Starting bulk ingest for job {} with {} commands", job.getId(), commands.size());

        int rowNumber = 0;

        for (LeaveIngestionCommand command : commands) {
            rowNumber++;

            try {
                // Ingest the leave
                LeaveDto result = ingest(command);

                // Create success record
                BulkUploadRecord record = BulkUploadRecord.createSuccess(
                        job,
                        rowNumber,
                        command.getUserId(),
                        result.getId()
                );
                bulkUploadRecordRepository.save(record);

                // Update job counters
                job.incrementSuccess();

                logger.debug("Successfully ingested row {} for job {}", rowNumber, job.getId());

            } catch (Exception e) {
                // Create failure record
                BulkUploadRecord record = BulkUploadRecord.createFailure(
                        job,
                        rowNumber,
                        command.getUserId(),
                        e.getMessage()
                );
                bulkUploadRecordRepository.save(record);

                // Update job counters
                job.incrementFailure();

                logger.warn("Failed to ingest row {} for job {}: {}", rowNumber, job.getId(), e.getMessage());
                // Continue processing next record
            }
        }

        // Mark job as completed
        job.markAsCompleted();
        bulkUploadJobRepository.save(job);

        logger.info("Completed bulk ingest for job {}: {} success, {} failed",
                job.getId(), job.getSuccessfulRecords(), job.getFailedRecords());

        // Generate CSV result file
        try {
            String resultFilePath = csvResultService.generateResultFile(job);
            job.setResultFilePath(resultFilePath);
            bulkUploadJobRepository.save(job);
            logger.info("Successfully generated result CSV for job {} at {}", job.getId(), resultFilePath);
        } catch (Exception e) {
            logger.error("Failed to generate result CSV for job {}", job.getId(), e);
            // Don't fail the job - the ingest was successful, only result generation failed
        }
    }
}
