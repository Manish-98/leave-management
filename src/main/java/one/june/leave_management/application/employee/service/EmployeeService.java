package one.june.leave_management.application.employee.service;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.application.employee.command.EmployeeCreateCommand;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.common.exception.EmployeeNotFoundException;
import one.june.leave_management.common.mapper.EmployeeMapper;
import one.june.leave_management.domain.employee.model.Employee;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.employee.service.EmployeeDomainService;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for Employee operations.
 * Orchestrates business logic and coordinates between domain and persistence layers.
 */
@Service
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeDomainService employeeDomainService;
    private final EmployeeMapper employeeMapper;
    private final BulkUploadJobRepository bulkUploadJobRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeeDomainService employeeDomainService,
                           EmployeeMapper employeeMapper, BulkUploadJobRepository bulkUploadJobRepository) {
        this.employeeRepository = employeeRepository;
        this.employeeDomainService = employeeDomainService;
        this.employeeMapper = employeeMapper;
        this.bulkUploadJobRepository = bulkUploadJobRepository;
    }

    /**
     * Create a new employee
     *
     * @param command the employee creation command
     * @return the created employee DTO
     */
    @Transactional
    public EmployeeDto create(EmployeeCreateCommand command) {
        log.info("Creating new employee: {}", command.getName());

        // Create domain entity using factory method
        Employee employee = Employee.create(
                command.getName(),
                command.getSlackId(),
                command.getGoogleId(),
                command.getSlackDisplayName(),
                command.getDateOfJoining()
        );

        // Set optional fields
        if (command.getActive() != null) {
            employee.setActive(command.getActive());
        }
        if (command.getCarryForwardLeaves() != null && !command.getCarryForwardLeaves().isEmpty()) {
            employee.setCarryForwardLeaves(command.getCarryForwardLeaves());
        }

        // Validate business rules
        employeeDomainService.validateEmployeeForPersistence(employee);

        // Save to database
        Employee savedEmployee = employeeRepository.save(employee);

        log.info("Successfully created employee with id: {}", savedEmployee.getId());
        return employeeMapper.toDto(savedEmployee);
    }

    /**
     * Get an employee by ID
     *
     * @param id the employee ID
     * @return the employee DTO
     * @throws EmployeeNotFoundException if employee not found
     */
    @Transactional(readOnly = true)
    public EmployeeDto findById(UUID id) {
        log.debug("Finding employee by id: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        return employeeMapper.toDto(employee);
    }

    /**
     * Get an employee by Slack ID
     *
     * @param slackId the Slack ID
     * @return Optional containing the employee DTO if found
     */
    @Transactional(readOnly = true)
    public Optional<EmployeeDto> findBySlackId(String slackId) {
        log.debug("Finding employee by Slack ID: {}", slackId);

        return employeeRepository.findBySlackId(slackId)
                .map(employeeMapper::toDto);
    }

    /**
     * Get an employee by Google ID
     *
     * @param googleId the Google ID
     * @return Optional containing the employee DTO if found
     */
    @Transactional(readOnly = true)
    public Optional<EmployeeDto> findByGoogleId(String googleId) {
        log.debug("Finding employee by Google ID: {}", googleId);

        return employeeRepository.findByGoogleId(googleId)
                .map(employeeMapper::toDto);
    }

    /**
     * Get all employees with pagination
     *
     * @param pageable pagination parameters
     * @return page of employee DTOs
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDto> findAll(Pageable pageable) {
        log.debug("Finding all employees with pagination: {}", pageable);

        return employeeRepository.findAll(pageable)
                .map(employeeMapper::toDto);
    }

    /**
     * Get all active employees with pagination
     *
     * @param pageable pagination parameters
     * @return page of active employee DTOs
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDto> findAllActive(Pageable pageable) {
        log.debug("Finding all active employees with pagination: {}", pageable);

        return employeeRepository.findAllActive(pageable)
                .map(employeeMapper::toDto);
    }

    /**
     * Get employees by active status with pagination
     *
     * @param active   the active status
     * @param pageable pagination parameters
     * @return page of employee DTOs
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDto> findByActiveStatus(Boolean active, Pageable pageable) {
        log.debug("Finding employees by active status: {} with pagination: {}", active, pageable);

        return employeeRepository.findByActiveStatus(active, pageable)
                .map(employeeMapper::toDto);
    }

    /**
     * Search employees by name with pagination
     *
     * @param name     the name to search for
     * @param pageable pagination parameters
     * @return page of matching employee DTOs
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDto> searchByName(String name, Pageable pageable) {
        log.debug("Searching employees by name: {} with pagination: {}", name, pageable);

        return employeeRepository.searchByName(name, pageable)
                .map(employeeMapper::toDto);
    }

    /**
     * Update an existing employee
     *
     * @param id      the employee ID
     * @param command the update command
     * @return the updated employee DTO
     * @throws EmployeeNotFoundException if employee not found
     */
    @Transactional
    public EmployeeDto update(UUID id, EmployeeCreateCommand command) {
        log.info("Updating employee with id: {}", id);

        // Find existing employee
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        // Update employee fields
        employee.update(
                command.getName(),
                command.getSlackId(),
                command.getGoogleId(),
                command.getSlackDisplayName(),
                command.getDateOfJoining()
        );

        // Update optional fields
        if (command.getActive() != null) {
            employee.setActive(command.getActive());
        }
        if (command.getCarryForwardLeaves() != null) {
            employee.setCarryForwardLeaves(command.getCarryForwardLeaves());
        }

        // Validate business rules
        employeeDomainService.validateEmployeeForPersistence(employee);

        // Save to database
        Employee updatedEmployee = employeeRepository.save(employee);

        log.info("Successfully updated employee with id: {}", id);
        return employeeMapper.toDto(updatedEmployee);
    }

    /**
     * Deactivate an employee (soft delete)
     *
     * @param id the employee ID
     * @throws EmployeeNotFoundException if employee not found
     */
    @Transactional
    public void deactivate(UUID id) {
        log.info("Deactivating employee with id: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        employee.deactivate();
        employeeRepository.save(employee);

        log.info("Successfully deactivated employee with id: {}", id);
    }

    /**
     * Activate an employee
     *
     * @param id the employee ID
     * @throws EmployeeNotFoundException if employee not found
     */
    @Transactional
    public void activate(UUID id) {
        log.info("Activating employee with id: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        employee.activate();
        employeeRepository.save(employee);

        log.info("Successfully activated employee with id: {}", id);
    }

    /**
     * Update carry forward leaves for an employee
     *
     * @param id   the employee ID
     * @param year the year
     * @param days the number of carry forward days
     * @throws EmployeeNotFoundException if employee not found
     */
    @Transactional
    public void updateCarryForwardLeaves(UUID id, Integer year, Integer days) {
        log.info("Updating carry forward leaves for employee id: {}, year: {}, days: {}", id, year, days);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        employee.updateCarryForwardLeaves(year, days);
        employeeRepository.save(employee);

        log.info("Successfully updated carry forward leaves for employee id: {}", id);
    }
}
