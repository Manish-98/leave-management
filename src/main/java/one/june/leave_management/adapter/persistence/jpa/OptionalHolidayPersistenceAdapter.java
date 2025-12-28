package one.june.leave_management.adapter.persistence.jpa;

import one.june.leave_management.adapter.persistence.jpa.entity.OptionalHolidayJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.OptionalHolidayJpaRepository;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import one.june.leave_management.domain.leave.port.OptionalHolidayRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OptionalHolidayPersistenceAdapter implements OptionalHolidayRepository {

    private final OptionalHolidayJpaRepository jpaRepository;

    public OptionalHolidayPersistenceAdapter(OptionalHolidayJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public OptionalHoliday save(OptionalHoliday holiday) {
        OptionalHolidayJpaEntity jpaEntity = toJpaEntity(holiday);
        OptionalHolidayJpaEntity saved = jpaRepository.save(jpaEntity);
        return toDomainEntity(saved);
    }

    @Override
    public List<OptionalHoliday> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptionalHoliday> findAllByOrderByDateAsc() {
        return jpaRepository.findAllByOrderByDateAsc().stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OptionalHoliday> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::toDomainEntity);
    }

    @Override
    public Optional<OptionalHoliday> findByDate(java.time.LocalDate date) {
        return jpaRepository.findByDate(date)
                .map(this::toDomainEntity);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    // Mapper methods
    private OptionalHoliday toDomainEntity(OptionalHolidayJpaEntity jpaEntity) {
        return OptionalHoliday.builder()
                .id(jpaEntity.getId())
                .date(jpaEntity.getDate())
                .name(jpaEntity.getName())
                .description(jpaEntity.getDescription())
                .build();
    }

    private OptionalHolidayJpaEntity toJpaEntity(OptionalHoliday domainEntity) {
        return OptionalHolidayJpaEntity.builder()
                .id(domainEntity.getId())
                .date(domainEntity.getDate())
                .name(domainEntity.getName())
                .description(domainEntity.getDescription())
                .build();
    }
}
