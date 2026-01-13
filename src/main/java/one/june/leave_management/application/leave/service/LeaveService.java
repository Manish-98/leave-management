package one.june.leave_management.application.leave.service;

import one.june.leave_management.adapter.inbound.web.dto.LeaveFetchQuery;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.application.employee.service.EmployeeService;
import one.june.leave_management.common.annotation.Auditable;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.common.exception.EmployeeNotFoundException;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveFilters;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.LeaveSourceRefRepository;
import one.june.leave_management.domain.leave.service.LeaveDomainService;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final EmployeeRepository employeeRepository;

    public LeaveService(LeaveRepository leaveRepository,
                        LeaveSourceRefRepository leaveSourceRefRepository,
                        OutboundSyncService outboundSyncService,
                        LeaveDomainService leaveDomainService,
                        LeaveMapper leaveMapper,
                        BulkUploadRecordRepository bulkUploadRecordRepository,
                        BulkUploadJobRepository bulkUploadJobRepository,
                        CsvResultService csvResultService,
                        LeaveBulkUploadStrategy leaveBulkUploadStrategy,
                        EmployeeService employeeService,
                        EmployeeRepository employeeRepository) {
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
        this.employeeRepository = employeeRepository;
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
     * @param query the filter query containing optional userName and date range
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of leaves matching the filter criteria
     * @throws IllegalArgumentException if startDate and endDate are not both provided
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

        // Batch fetch employees to avoid N+1 queries
        java.util.Map<UUID, EmployeeDto> employeeCache = batchFetchEmployees(leavesPage.getContent());

        // Convert to DTOs using employee cache and calculate adjusted duration
        Page<LeaveDto> dtoPage;
        if (query.getStartDate() != null && query.getEndDate() != null) {
            // Calculate adjusted duration based on date range overlap
            dtoPage = leavesPage.map(leave -> {
                LeaveDto dto = leaveMapper.toDto(leave, employeeCache);
                dto.setAdjustedDurationInDays(calculateAdjustedDuration(leave, query.getStartDate(), query.getEndDate()));
                return dto;
            });
        } else {
            // No date range, use actual duration from the leave
            dtoPage = leavesPage.map(leave -> {
                LeaveDto dto = leaveMapper.toDto(leave, employeeCache);
                // Calculate actual duration from leave
                double actualDuration;
                if (leave.getDurationType() == one.june.leave_management.domain.leave.model.LeaveDurationType.FULL_DAY) {
                    actualDuration = leave.getDateRange() != null ? leave.getDateRange().toDays() : 0;
                } else {
                    actualDuration = 0.5;
                }
                dto.setAdjustedDurationInDays(actualDuration);
                return dto;
            });
        }

        logger.info("Successfully fetched {} leaves (page {} of {})",
                    dtoPage.getNumberOfElements(),
                    dtoPage.getNumber() + 1,
                    dtoPage.getTotalPages());

        return dtoPage;
    }

    /**
     * Batch fetch all employees for the given leaves to avoid N+1 query problem.
     *
     * @param leaves list of leaves
     * @return map of employee UUID to EmployeeDto
     */
    private Map<UUID, EmployeeDto> batchFetchEmployees(List<Leave> leaves) {
        // Collect all unique user IDs
        Set<UUID> userIds = leaves.stream()
                .map(Leave::getUserId)
                .filter(userId -> userId != null)
                .map(userId -> {
                    try {
                        return UUID.fromString(userId);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid UUID format for userId: {}", userId);
                        return null;
                    }
                })
                .filter(uuid -> uuid != null)
                .collect(Collectors.toSet());

        // Batch fetch all employees
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<EmployeeDto> employees = employeeService.findAllById(userIds);

        // Create a map for quick lookup
        return employees.stream()
                .collect(Collectors.toMap(
                        employee -> employee.getId(),
                        employee -> employee
                ));
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
     * When userName is provided, searches for employees by name or slackDisplayName
     * and uses their UUIDs for filtering.
     *
     * @param query the web layer query object
     * @return the domain layer filters object
     */
    private LeaveFilters convertToFilters(LeaveFetchQuery query) {
        LeaveFilters.LeaveFiltersBuilder builder = LeaveFilters.builder();

        // User ID filter - search employees by name or slackDisplayName
        if (query.getUserName() != null && !query.getUserName().isBlank()) {
            List<String> userIds = searchEmployeeIds(query.getUserName());
            if (userIds != null && !userIds.isEmpty()) {
                builder.userIds(userIds);
            } else {
                // If no employees found, set empty list to return no results
                builder.userIds(Collections.emptyList());
            }
        }

        // Date range filter
        if (query.getStartDate() != null && query.getEndDate() != null) {
            builder.startDate(query.getStartDate());
            builder.endDate(query.getEndDate());
        }

        return builder.build();
    }

    /**
     * Search for employees by name or slackDisplayName and return their UUIDs as strings.
     *
     * @param query the search query (name or slackDisplayName)
     * @return list of employee UUIDs as strings
     */
    private List<String> searchEmployeeIds(String query) {
        logger.debug("Searching for employees with query: {}", query);

        List<one.june.leave_management.domain.employee.model.Employee> employees =
                employeeRepository.searchByNameOrSlackDisplayName(query);

        logger.debug("Found {} employees matching query: {}", employees.size(), query);

        return employees.stream()
                .map(employee -> employee.getId().toString())
                .collect(Collectors.toList());
    }

    /**
     * Calculate the adjusted duration of a leave based on overlap with a query date range.
     * For full-day leaves, calculates the exact number of overlapping days.
     * For half-day leaves, returns 0.5 if the leave overlaps with the range.
     *
     * @param leave the leave entity
     * @param queryStart the start date of the query range
     * @param queryEnd the end date of the query range
     * @return the adjusted duration in days
     */
    private double calculateAdjustedDuration(Leave leave, java.time.LocalDate queryStart, java.time.LocalDate queryEnd) {
        // For half-day leaves, return 0.5 if it overlaps with the range
        if (leave.getDurationType() != one.june.leave_management.domain.leave.model.LeaveDurationType.FULL_DAY) {
            // Check if the leave date overlaps with the query range
            if (leave.getStartDate() != null && leave.getEndDate() != null) {
                boolean overlaps = leave.getStartDate().compareTo(queryEnd) <= 0
                        && leave.getEndDate().compareTo(queryStart) >= 0;
                return overlaps ? 0.5 : 0.0;
            }
            return 0.0;
        }

        // For full-day leaves, calculate overlapping days
        java.time.LocalDate leaveStart = leave.getStartDate();
        java.time.LocalDate leaveEnd = leave.getEndDate();

        if (leaveStart == null || leaveEnd == null) {
            return 0.0;
        }

        // Calculate overlap
        java.time.LocalDate overlapStart = leaveStart.isAfter(queryStart) ? leaveStart : queryStart;
        java.time.LocalDate overlapEnd = leaveEnd.isBefore(queryEnd) ? leaveEnd : queryEnd;

        // Check if there is an overlap
        if (overlapStart.isAfter(overlapEnd)) {
            return 0.0; // No overlap
        }

        // Calculate days in the overlap (inclusive)
        long days = java.time.temporal.ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
        return (double) days;
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

    /**
     * Soft delete a leave by updating its status to DEACTIVATED.
     * Validates that the leave exists and belongs to the requesting user.
     *
     * @param leaveId the UUID of the leave to deactivate
     * @param requestingUserId the user ID of the employee requesting deletion
     * @return the updated leave DTO
     * @throws IllegalArgumentException if leave not found or doesn't belong to user
     */
    @Transactional
    public LeaveDto softDeleteLeave(UUID leaveId, String requestingUserId) {
        logger.info("Soft deleting leave {} for user {}", leaveId, requestingUserId);

        // Validate inputs
        if (leaveId == null) {
            throw new IllegalArgumentException("Leave ID cannot be null");
        }
        if (requestingUserId == null || requestingUserId.isBlank()) {
            throw new IllegalArgumentException("Requesting user ID cannot be null or empty");
        }

        // Find the leave
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave not found with ID: " + leaveId));

        // Validate ownership
        if (!requestingUserId.equals(leave.getUserId())) {
            logger.warn("User {} attempted to delete leave {} belonging to user {}",
                    requestingUserId, leaveId, leave.getUserId());
            throw new IllegalArgumentException("Leave does not belong to the requesting user");
        }

        // Check if already deactivated
        if (leave.getStatus() == LeaveStatus.DEACTIVATED) {
            logger.info("Leave {} is already deactivated", leaveId);
            return leaveMapper.toDto(leave);
        }

        // Update status to DEACTIVATED
        leave.setStatus(LeaveStatus.DEACTIVATED);
        Leave updatedLeave = leaveRepository.save(leave);

        logger.info("Successfully soft deleted leave {} for user {}", leaveId, requestingUserId);
        return leaveMapper.toDto(updatedLeave);
    }

    /**
     * Find all active (non-deactivated) leaves for a specific user.
     *
     * @param userId the user ID
     * @return list of active leave DTOs
     */
    @Transactional(readOnly = true)
    public List<LeaveDto> findActiveLeavesByUserId(String userId) {
        logger.debug("Finding active leaves for user {}", userId);
        List<Leave> activeLeaves = leaveRepository.findActiveLeavesByUserId(userId);
        return activeLeaves.stream()
                .map(leaveMapper::toDto)
                .collect(Collectors.toList());
    }
}
