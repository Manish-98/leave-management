package one.june.leave_management.domain.employee.port;

import one.june.leave_management.domain.employee.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for Employee domain operations.
 * This interface defines the contract for employee data access.
 */
public interface EmployeeRepository {
    /**
     * Save an employee (create or update)
     *
     * @param employee the employee to save
     * @return the saved employee
     */
    Employee save(Employee employee);

    /**
     * Find an employee by ID
     *
     * @param id the employee ID
     * @return Optional containing the employee if found
     */
    Optional<Employee> findById(UUID id);

    /**
     * Find an employee by Slack ID
     *
     * @param slackId the Slack ID
     * @return Optional containing the employee if found
     */
    Optional<Employee> findBySlackId(String slackId);

    /**
     * Find an employee by Google ID
     *
     * @param googleId the Google ID
     * @return Optional containing the employee if found
     */
    Optional<Employee> findByGoogleId(String googleId);

    /**
     * Find all employees with pagination
     *
     * @param pageable pagination parameters
     * @return page of employees
     */
    Page<Employee> findAll(Pageable pageable);

    /**
     * Find all active employees with pagination
     *
     * @param pageable pagination parameters
     * @return page of active employees
     */
    Page<Employee> findAllActive(Pageable pageable);

    /**
     * Find employees by active status with pagination
     *
     * @param active   the active status to filter by
     * @param pageable pagination parameters
     * @return page of employees matching the active status
     */
    Page<Employee> findByActiveStatus(Boolean active, Pageable pageable);

    /**
     * Search employees by name (case-insensitive partial match)
     *
     * @param name     the name to search for
     * @param pageable pagination parameters
     * @return page of employees matching the name
     */
    Page<Employee> searchByName(String name, Pageable pageable);

    /**
     * Check if an employee exists by Slack ID
     *
     * @param slackId the Slack ID
     * @return true if an employee with the Slack ID exists
     */
    boolean existsBySlackId(String slackId);

    /**
     * Check if an employee exists by Google ID
     *
     * @param googleId the Google ID
     * @return true if an employee with the Google ID exists
     */
    boolean existsByGoogleId(String googleId);

    /**
     * Check if an employee exists by Slack ID, excluding a specific employee ID
     * (useful for updates to avoid self-comparison)
     *
     * @param slackId        the Slack ID
     * @param excludeEmployeeId the employee ID to exclude
     * @return true if an employee with the Slack ID exists (excluding the specified ID)
     */
    boolean existsBySlackIdAndIdNot(String slackId, UUID excludeEmployeeId);

    /**
     * Check if an employee exists by Google ID, excluding a specific employee ID
     * (useful for updates to avoid self-comparison)
     *
     * @param googleId        the Google ID
     * @param excludeEmployeeId the employee ID to exclude
     * @return true if an employee with the Google ID exists (excluding the specified ID)
     */
    boolean existsByGoogleIdAndIdNot(String googleId, UUID excludeEmployeeId);

    /**
     * Check if an employee exists by ID
     *
     * @param id the employee ID
     * @return true if an employee with the ID exists
     */
    boolean existsById(UUID id);

    /**
     * Delete an employee by ID
     *
     * @param id the employee ID
     */
    void deleteById(UUID id);
}
