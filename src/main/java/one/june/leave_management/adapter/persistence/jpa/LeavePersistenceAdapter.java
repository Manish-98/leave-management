package one.june.leave_management.adapter.persistence.jpa;

import one.june.leave_management.adapter.persistence.jpa.entity.LeaveJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.entity.LeaveSourceRefJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.LeaveJpaRepository;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LeavePersistenceAdapter implements LeaveRepository {

    private static final Logger logger = LoggerFactory.getLogger(LeavePersistenceAdapter.class);

    private final LeaveJpaRepository leaveJpaRepository;
    private final LeaveMapper leaveMapper;

    public LeavePersistenceAdapter(LeaveJpaRepository leaveJpaRepository,
                                   LeaveMapper leaveMapper) {
        this.leaveJpaRepository = leaveJpaRepository;
        this.leaveMapper = leaveMapper;
    }

    // LeaveRepository implementation
    @Override
    @Transactional
    public Leave save(Leave leave) {
        logger.debug("Saving leave: {}", leave);

        LeaveJpaEntity jpaEntity = leaveMapper.toJpaEntity(leave);

        // Handle source references for new leaves
        if (jpaEntity.getId() == null) {
            // This is a new leave
            for (LeaveSourceRef sourceRef : leave.getSourceRefs()) {
                LeaveSourceRefJpaEntity sourceRefJpaEntity = leaveMapper.toJpaEntity(sourceRef);
                sourceRefJpaEntity.setLeave(jpaEntity);
                jpaEntity.getSourceRefs().add(sourceRefJpaEntity);
            }
        } else {
            // This is an update - preserve existing source refs or merge changes
            UUID leaveId = jpaEntity.getId();
            LeaveJpaEntity existingEntity = leaveJpaRepository.findById(leaveId)
                    .orElseThrow(() -> new IllegalArgumentException("Leave not found with id: " + leaveId));
            existingEntity.getSourceRefs().clear();

            // Add or update source references
            for (LeaveSourceRef sourceRef : leave.getSourceRefs()) {
                LeaveSourceRefJpaEntity sourceRefJpaEntity = leaveMapper.toJpaEntity(sourceRef);
                sourceRefJpaEntity.setLeave(existingEntity);
                existingEntity.getSourceRefs().add(sourceRefJpaEntity);
            }

            // Copy other properties
            existingEntity.setUserId(leave.getUserId());
            existingEntity.setStartDate(leave.getStartDate());
            existingEntity.setEndDate(leave.getEndDate());
            existingEntity.setType(leave.getType());
            existingEntity.setStatus(leave.getStatus());
            jpaEntity = existingEntity;
        }

        LeaveJpaEntity savedEntity = leaveJpaRepository.save(jpaEntity);
        return leaveMapper.toDomainEntity(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Leave> findById(UUID id) {
        return leaveJpaRepository.findById(id)
                .map(leaveMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Leave> findByUserId(String userId) {
        return leaveJpaRepository.findByUserId(userId).stream()
                .map(leaveMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        leaveJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return leaveJpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Leave> findOverlappingLeaves(String userId, DateRange dateRange) {
        logger.debug("Finding overlapping leaves for user {} with date range {}", userId, dateRange);

        List<LeaveJpaEntity> overlappingEntities = leaveJpaRepository.findOverlappingLeaves(
                userId, dateRange.getStartDate(), dateRange.getEndDate());

        return overlappingEntities.stream()
                .map(leaveMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Leave> findOverlappingLeaves(String userId, DateRange dateRange, UUID excludeLeaveId) {
        logger.debug("Finding overlapping leaves for user {} with date range {} excluding leave ID {}",
                    userId, dateRange, excludeLeaveId);

        List<LeaveJpaEntity> overlappingEntities = leaveJpaRepository.findOverlappingLeaves(
                userId, dateRange.getStartDate(), dateRange.getEndDate(), excludeLeaveId);

        return overlappingEntities.stream()
                .map(leaveMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}