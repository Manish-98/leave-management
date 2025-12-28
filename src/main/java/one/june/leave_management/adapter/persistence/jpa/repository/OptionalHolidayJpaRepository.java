package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.adapter.persistence.jpa.entity.OptionalHolidayJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for OptionalHoliday entities.
 * Adapter implementation of the OptionalHolidayRepository port.
 */
@Repository
public interface OptionalHolidayJpaRepository extends JpaRepository<OptionalHolidayJpaEntity, UUID> {

    /**
     * Find all holidays ordered by date ascending.
     * @return list of holidays sorted by date
     */
    List<OptionalHolidayJpaEntity> findAllByOrderByDateAsc();

    /**
     * Find a holiday by its date.
     * @param date the date to search for
     * @return optional containing the holiday if found
     */
    Optional<OptionalHolidayJpaEntity> findByDate(LocalDate date);
}
