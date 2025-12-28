package one.june.leave_management.domain.leave.port;

import one.june.leave_management.domain.leave.model.OptionalHoliday;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for OptionalHoliday domain operations.
 * Port in the hexagonal architecture pattern.
 */
public interface OptionalHolidayRepository {
    OptionalHoliday save(OptionalHoliday holiday);
    List<OptionalHoliday> findAll();
    List<OptionalHoliday> findAllByOrderByDateAsc();
    Optional<OptionalHoliday> findById(UUID id);
    Optional<OptionalHoliday> findByDate(java.time.LocalDate date);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
