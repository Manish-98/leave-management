package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.adapter.persistence.jpa.entity.EmployeeJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for EmployeeJpaEntity.
 * Provides database access methods for employee operations.
 */
@Repository
public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {

    /**
     * Find an employee by Slack ID
     *
     * @param slackId the Slack ID
     * @return Optional containing the employee if found
     */
    Optional<EmployeeJpaEntity> findBySlackId(String slackId);

    /**
     * Find an employee by Google ID
     *
     * @param googleId the Google ID
     * @return Optional containing the employee if found
     */
    Optional<EmployeeJpaEntity> findByGoogleId(String googleId);

    /**
     * Find all employees with active status true
     *
     * @param pageable pagination parameters
     * @return page of active employees
     */
    Page<EmployeeJpaEntity> findByActiveTrue(Pageable pageable);

    /**
     * Find employees by active status
     *
     * @param active   the active status
     * @param pageable pagination parameters
     * @return page of employees matching the active status
     */
    Page<EmployeeJpaEntity> findByActive(Boolean active, Pageable pageable);

    /**
     * Search employees by name (case-insensitive partial match)
     *
     * @param name     the name to search for
     * @param pageable pagination parameters
     * @return page of employees matching the name
     */
    Page<EmployeeJpaEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

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
     *
     * @param slackId        the Slack ID
     * @param excludeEmployeeId the employee ID to exclude
     * @return true if an employee with the Slack ID exists (excluding the specified ID)
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EmployeeJpaEntity e " +
           "WHERE e.slackId = :slackId AND e.id != :excludeEmployeeId")
    boolean existsBySlackIdAndIdNot(@Param("slackId") String slackId,
                                     @Param("excludeEmployeeId") UUID excludeEmployeeId);

    /**
     * Check if an employee exists by Google ID, excluding a specific employee ID
     *
     * @param googleId        the Google ID
     * @param excludeEmployeeId the employee ID to exclude
     * @return true if an employee with the Google ID exists (excluding the specified ID)
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EmployeeJpaEntity e " +
           "WHERE e.googleId = :googleId AND e.id != :excludeEmployeeId")
    boolean existsByGoogleIdAndIdNot(@Param("googleId") String googleId,
                                      @Param("excludeEmployeeId") UUID excludeEmployeeId);
}
