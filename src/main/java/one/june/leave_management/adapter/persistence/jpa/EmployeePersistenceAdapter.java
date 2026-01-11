package one.june.leave_management.adapter.persistence.jpa;

import one.june.leave_management.adapter.persistence.jpa.entity.EmployeeJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.EmployeeJpaRepository;
import one.june.leave_management.common.exception.EmployeeNotFoundException;
import one.june.leave_management.common.mapper.EmployeeMapper;
import one.june.leave_management.domain.employee.model.Employee;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for Employee entity.
 * Implements the domain repository port and bridges domain and persistence layers.
 */
@Component
public class EmployeePersistenceAdapter implements EmployeeRepository {

    private static final Logger logger = LoggerFactory.getLogger(EmployeePersistenceAdapter.class);

    private final EmployeeJpaRepository employeeJpaRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeePersistenceAdapter(EmployeeJpaRepository employeeJpaRepository,
                                       EmployeeMapper employeeMapper) {
        this.employeeJpaRepository = employeeJpaRepository;
        this.employeeMapper = employeeMapper;
    }

    @Override
    @Transactional
    public Employee save(Employee employee) {
        logger.debug("Saving employee: {}", employee.getId() != null ? employee.getId() : "new employee");

        EmployeeJpaEntity jpaEntity;
        if (employee.getId() != null) {
            // Update existing employee
            EmployeeJpaEntity existingEntity = employeeJpaRepository.findById(employee.getId())
                    .orElseThrow(() -> new EmployeeNotFoundException(employee.getId()));
            employeeMapper.updateJpaEntityFromDomain(employee, existingEntity);
            jpaEntity = employeeJpaRepository.save(existingEntity);
        } else {
            // Create new employee
            jpaEntity = employeeMapper.toJpaEntity(employee);
            jpaEntity = employeeJpaRepository.save(jpaEntity);
        }

        return employeeMapper.toDomainEntity(jpaEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findById(UUID id) {
        return employeeJpaRepository.findById(id)
                .map(employeeMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAllById(Iterable<UUID> ids) {
        return employeeJpaRepository.findAllById(ids).stream()
                .map(employeeMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findBySlackId(String slackId) {
        return employeeJpaRepository.findBySlackId(slackId)
                .map(employeeMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findByGoogleId(String googleId) {
        return employeeJpaRepository.findByGoogleId(googleId)
                .map(employeeMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findAll(Pageable pageable) {
        return employeeJpaRepository.findAll(pageable)
                .map(employeeMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findAllActive(Pageable pageable) {
        return employeeJpaRepository.findByActiveTrue(pageable)
                .map(employeeMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findByActiveStatus(Boolean active, Pageable pageable) {
        return employeeJpaRepository.findByActive(active, pageable)
                .map(employeeMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> searchByName(String name, Pageable pageable) {
        return employeeJpaRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(employeeMapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> searchByNameOrSlackDisplayName(String query) {
        return employeeJpaRepository
                .findByNameContainingIgnoreCaseOrSlackDisplayNameContainingIgnoreCase(query, query)
                .stream()
                .map(employeeMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlackId(String slackId) {
        return employeeJpaRepository.existsBySlackId(slackId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByGoogleId(String googleId) {
        return employeeJpaRepository.existsByGoogleId(googleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlackIdAndIdNot(String slackId, UUID excludeEmployeeId) {
        return employeeJpaRepository.existsBySlackIdAndIdNot(slackId, excludeEmployeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByGoogleIdAndIdNot(String googleId, UUID excludeEmployeeId) {
        return employeeJpaRepository.existsByGoogleIdAndIdNot(googleId, excludeEmployeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return employeeJpaRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (!employeeJpaRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeJpaRepository.deleteById(id);
        logger.debug("Deleted employee with id: {}", id);
    }
}
