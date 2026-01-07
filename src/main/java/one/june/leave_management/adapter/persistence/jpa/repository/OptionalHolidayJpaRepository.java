package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.adapter.persistence.jpa.entity.OptionalHolidayJpaEntity;
import one.june.leave_management.domain.common.model.Region;
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

    /**
     * Find all holidays by region.
     * @param region the region to filter by
     * @return list of holidays in the specified region
     */
    List<OptionalHolidayJpaEntity> findByRegion(Region region);

    /**
     * Find all holidays by region ordered by date ascending.
     * @param region the region to filter by
     * @return list of holidays in the specified region sorted by date
     */
    List<OptionalHolidayJpaEntity> findByRegionOrderByDateAsc(Region region);
}
