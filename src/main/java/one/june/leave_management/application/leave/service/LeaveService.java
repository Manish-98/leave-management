package one.june.leave_management.application.leave.service;

import one.june.leave_management.adapter.inbound.web.dto.LeaveFetchQuery;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.employee.service.EmployeeService;
import one.june.leave_management.common.annotation.Auditable;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.common.exception.EmployeeNotFoundException;
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
import one.june.leave_management.application.bulk.strategy.LeaveBulkUploadStrategy;
import one.june.leave_management.application.bulk.service.CsvResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified service for leave operations.
 * Handles both leave ingestion (create/update) and fetching with filters.
 */
@Service
public class LeaveService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LeaveRepository leaveRepository;
    private final LeaveSourceRefRepository leaveSourceRefRepository;
    private final OutboundSyncService outboundSyncService;
    private final LeaveDomainService leaveDomainService;
    private final LeaveMapper leaveMapper;
    private final BulkUploadRecordRepository bulkUploadRecordRepository;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final CsvResultService csvResultService;
    private final LeaveBulkUploadStrategy leaveBulkUploadStrategy;
    private final EmployeeService employeeService;

    public LeaveService(LeaveRepository leaveRepository,
                        LeaveSourceRefRepository leaveSourceRefRepository,
                        OutboundSyncService outboundSyncService,
                        LeaveDomainService leaveDomainService,
                        LeaveMapper leaveMapper,
                        BulkUploadRecordRepository bulkUploadRecordRepository,
                        BulkUploadJobRepository bulkUploadJobRepository,
                        CsvResultService csvResultService,
                        LeaveBulkUploadStrategy leaveBulkUploadStrategy,
                        EmployeeService employeeService) {
        this.leaveRepository = leaveRepository;
        this.leaveSourceRefRepository = leaveSourceRefRepository;
        this.outboundSyncService = outboundSyncService;
        this.leaveDomainService = leaveDomainService;
        this.leaveMapper = leaveMapper;
        this.bulkUploadRecordRepository = bulkUploadRecordRepository;
        this.bulkUploadJobRepository = bulkUploadJobRepository;
        this.csvResultService = csvResultService;
        this.leaveBulkUploadStrategy = leaveBulkUploadStrategy;
        this.employeeService = employeeService;
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

        // Apply default sort if not specified
        Pageable sortedPageable = applyDefaultSort(pageable);

        // Fetch leaves from repository
        Page<Leave> leavesPage = leaveRepository.findByFilters(filters, sortedPageable);

        // Convert to DTOs
        Page<LeaveDto> dtoPage = leavesPage.map(leaveMapper::toDto);

        logger.info("Successfully fetched {} leaves (page {} of {})",
                    dtoPage.getNumberOfElements(),
                    dtoPage.getNumber() + 1,
                    dtoPage.getTotalPages());

        return dtoPage;
    }

    /**
     * Apply default sort if no sort is specified in the Pageable.
     * Default sort: startDate DESC (most recent first)
     *
     * @param pageable the original pageable
     * @return pageable with default sort if unsorted, otherwise original pageable
     */
    private Pageable applyDefaultSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            logger.debug("Applying default sort: startDate DESC");
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "startDate")
            );
        }
        return pageable;
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
        validateEmployeeExists(command.getUserId());
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

        validateEmployeeExists(command.getUserId());

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

    /**
     * Validates that the userId is a valid UUID and the employee exists
     *
     * @param userId the user ID to validate
     * @throws IllegalArgumentException if userId is not a valid UUID
     * @throws EmployeeNotFoundException if employee doesn't exist
     */
    private void validateEmployeeExists(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        try {
            UUID employeeUuid = UUID.fromString(userId);
            // Validate that employee exists
            employeeService.findById(employeeUuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format for user ID: " + userId, e);
        }
    }

    private LeaveSourceRef createSourceReference(LeaveIngestionCommand command, Leave leave) {
        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .sourceType(command.getSourceType())
                .sourceId(command.getSourceId())
                .build();
        leave.addSourceRef(sourceRef);
        return sourceRef;
    }

    // Package-private for testing
    void performOutboundSync(Leave leave, SourceType sourceType) {
        // Skip outbound sync for bulk uploads
        if (sourceType == SourceType.BULK_UPLOAD) {
            logger.info("Skipping outbound sync for bulk upload leave {}", leave.getId());
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

    // Package-private for testing
    Leave findOrCreateLeaveFromSourceRef(LeaveSourceRef sourceRef) {
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
}
