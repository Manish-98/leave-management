package one.june.leave_management.application.leave.service;

import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.LeaveSourceRefRepository;
import one.june.leave_management.domain.leave.service.LeaveDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LeaveIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveIngestionService.class);

    private final LeaveRepository leaveRepository;
    private final LeaveSourceRefRepository leaveSourceRefRepository;
    private final OutboundSyncService outboundSyncService;
    private final LeaveDomainService leaveDomainService;
    private final LeaveMapper leaveMapper;

    public LeaveIngestionService(LeaveRepository leaveRepository,
                                 LeaveSourceRefRepository leaveSourceRefRepository,
                                 OutboundSyncService outboundSyncService,
                                 LeaveDomainService leaveDomainService,
                                 LeaveMapper leaveMapper) {
        this.leaveRepository = leaveRepository;
        this.leaveSourceRefRepository = leaveSourceRefRepository;
        this.outboundSyncService = outboundSyncService;
        this.leaveDomainService = leaveDomainService;
        this.leaveMapper = leaveMapper;
    }

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

    private Leave createNewLeave(LeaveIngestionCommand command) {
        logger.debug("Creating new leave from command");
        return Leave.builder()
                .userId(command.getUserId())
                .dateRange(command.getDateRange())
                .type(command.getType())
                .status(command.getStatus())
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

    private void performOutboundSync(Leave leave, one.june.leave_management.domain.leave.model.SourceType sourceType) {
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
}