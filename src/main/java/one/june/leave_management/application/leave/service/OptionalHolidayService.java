package one.june.leave_management.application.leave.service;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.elements.SlackOption;
import one.june.leave_management.application.leave.dto.OptionalHolidayDto;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import one.june.leave_management.domain.leave.port.OptionalHolidayRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing optional holidays.
 * Provides methods to fetch holidays and convert them to Slack options.
 */
@Service
@Slf4j
public class OptionalHolidayService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OptionalHolidayRepository optionalHolidayRepository;

    public OptionalHolidayService(OptionalHolidayRepository optionalHolidayRepository) {
        this.optionalHolidayRepository = optionalHolidayRepository;
    }

    /**
     * Get all optional holidays ordered by date.
     * @return list of optional holiday DTOs
     */
    public List<OptionalHolidayDto> getAllHolidays() {
        log.debug("Fetching all optional holidays");

        List<OptionalHoliday> holidays = optionalHolidayRepository.findAllByOrderByDateAsc();

        return holidays.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all optional holidays as Slack options for dropdown.
     * Format: "YYYY-MM-DD - Holiday Name"
     * Value: Holiday ID (as string for Slack)
     * @return list of SlackOption objects
     */
    public List<SlackOption> getAllHolidaysAsSlackOptions() {
        log.debug("Fetching all optional holidays as Slack options");

        List<OptionalHoliday> holidays = optionalHolidayRepository.findAllByOrderByDateAsc();

        return holidays.stream()
                .map(holiday -> {
                    String displayText = String.format("%s - %s",
                            holiday.getDate().format(DATE_FORMATTER),
                            holiday.getName());
                    String value = holiday.getId().toString();
                    return SlackOption.of(displayText, value);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get optional holiday by ID.
     * @param id the holiday ID
     * @return optional containing the holiday DTO if found
     */
    public OptionalHolidayDto getHolidayById(UUID id) {
        log.debug("Fetching optional holiday by id: {}", id);

        return optionalHolidayRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Get optional holiday by ID (returns domain entity).
     * @param id the holiday ID
     * @return Optional containing the holiday domain entity if found
     */
    public Optional<OptionalHoliday> findById(UUID id) {
        log.debug("Fetching optional holiday domain entity by id: {}", id);
        return optionalHolidayRepository.findById(id);
    }

    /**
     * Create a new optional holiday.
     * @param holiday the holiday to create
     * @return the created holiday DTO
     */
    public OptionalHolidayDto createHoliday(OptionalHoliday holiday) {
        log.debug("Creating new optional holiday: {}", holiday.getName());

        OptionalHoliday saved = optionalHolidayRepository.save(holiday);
        return toDto(saved);
    }

    /**
     * Update an existing optional holiday.
     * @param id the ID of the holiday to update
     * @param updatedHoliday the updated holiday data
     * @return the updated holiday DTO
     * @throws IllegalArgumentException if holiday not found
     */
    public OptionalHolidayDto updateHoliday(UUID id, OptionalHoliday updatedHoliday) {
        log.debug("Updating optional holiday with id: {}", id);

        if (!optionalHolidayRepository.existsById(id)) {
            throw new IllegalArgumentException("Optional holiday not found with id: " + id);
        }

        // Preserve the ID and update other fields
        OptionalHoliday holidayWithId = OptionalHoliday.builder()
                .id(id)
                .date(updatedHoliday.getDate())
                .name(updatedHoliday.getName())
                .description(updatedHoliday.getDescription())
                .build();

        OptionalHoliday saved = optionalHolidayRepository.save(holidayWithId);
        return toDto(saved);
    }

    /**
     * Delete an optional holiday by ID.
     * @param id the ID of the holiday to delete
     */
    public void deleteHoliday(UUID id) {
        log.debug("Deleting optional holiday with id: {}", id);
        optionalHolidayRepository.deleteById(id);
    }

    /**
     * Convert domain entity to DTO.
     * @param holiday the domain entity
     * @return the DTO
     */
    private OptionalHolidayDto toDto(OptionalHoliday holiday) {
        return OptionalHolidayDto.builder()
                .id(holiday.getId())
                .date(holiday.getDate())
                .name(holiday.getName())
                .description(holiday.getDescription())
                .build();
    }
}
