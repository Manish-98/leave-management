package one.june.leave_management.adapter.persistence.jpa;

import one.june.leave_management.adapter.persistence.jpa.entity.LeaveSourceRefJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.LeaveSourceRefJpaRepository;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.domain.leave.port.LeaveSourceRefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LeaveSourceRefPersistenceAdapter implements LeaveSourceRefRepository {

    private static final Logger logger = LoggerFactory.getLogger(LeaveSourceRefPersistenceAdapter.class);

    private final LeaveSourceRefJpaRepository leaveSourceRefJpaRepository;
    private final LeaveMapper leaveMapper;

    public LeaveSourceRefPersistenceAdapter(LeaveSourceRefJpaRepository leaveSourceRefJpaRepository,
                                           LeaveMapper leaveMapper) {
        this.leaveSourceRefJpaRepository = leaveSourceRefJpaRepository;
        this.leaveMapper = leaveMapper;
    }

    @Override
    @Transactional
    public LeaveSourceRef save(LeaveSourceRef sourceRef) {
        logger.debug("Saving leave source reference: {}", sourceRef);
        LeaveSourceRefJpaEntity jpaEntity = leaveMapper.toJpaEntity(sourceRef);

        // If this is a new source ref without an associated leave, we need to handle that
        if (jpaEntity.getLeave() == null && jpaEntity.getId() == null) {
            throw new IllegalArgumentException("LeaveSourceRef must be associated with a Leave");
        }

        LeaveSourceRefJpaEntity savedEntity = leaveSourceRefJpaRepository.save(jpaEntity);
        return leaveMapper.toDomainEntity(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LeaveSourceRef> findById(UUID id) {
        return leaveSourceRefJpaRepository.findById(id)
                .map(leaveMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LeaveSourceRef> findBySourceTypeAndSourceId(SourceType sourceType, String sourceId) {
        return leaveSourceRefJpaRepository.findBySourceTypeAndSourceId(sourceType, sourceId)
                .map(leaveMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveSourceRef> findByLeaveId(UUID leaveId) {
        return leaveSourceRefJpaRepository.findByLeaveId(leaveId).stream()
                .map(leaveMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        leaveSourceRefJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return leaveSourceRefJpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LeaveSourceRef> findBySourceTypeAndSourceIdWithLeave(SourceType sourceType, String sourceId) {
        return leaveSourceRefJpaRepository.findBySourceTypeAndSourceIdWithLeave(sourceType, sourceId)
                .map(jpaEntity -> {
                    LeaveSourceRef domainRef = leaveMapper.toDomainEntity(jpaEntity);
                    // Set the leave ID from the JPA entity's leave reference
                    if (jpaEntity.getLeave() != null) {
                        domainRef.setLeaveId(jpaEntity.getLeave().getId());
                    }
                    return domainRef;
                });
    }
}