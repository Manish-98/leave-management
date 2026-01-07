package one.june.leave_management.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import one.june.leave_management.adapter.inbound.web.dto.CreateOptionalHolidayRequest;
import one.june.leave_management.adapter.inbound.web.dto.UpdateOptionalHolidayRequest;
import one.june.leave_management.application.leave.dto.OptionalHolidayDto;
import one.june.leave_management.application.leave.service.OptionalHolidayService;
import one.june.leave_management.domain.common.model.Region;
import one.june.leave_management.domain.leave.model.OptionalHoliday;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/optional-holidays")
@Tag(name = "Optional Holiday Management", description = "Admin APIs for managing optional holidays")
public class OptionalHolidayController {

    private static final Logger logger = LoggerFactory.getLogger(OptionalHolidayController.class);

    private final OptionalHolidayService optionalHolidayService;

    public OptionalHolidayController(OptionalHolidayService optionalHolidayService) {
        this.optionalHolidayService = optionalHolidayService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new optional holiday",
            description = "Creates a new optional holiday that users can select when applying for leave. " +
                    "The date must be unique. Returns the created holiday with its generated UUID.",
            tags = {"Optional Holiday Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Optional holiday successfully created",
                    content = @Content(schema = @Schema(implementation = OptionalHolidayDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data - validation failed or duplicate date"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<OptionalHolidayDto> createHoliday(
            @Parameter(
                    description = "Optional holiday details",
                    required = true,
                    schema = @Schema(implementation = CreateOptionalHolidayRequest.class)
            )
            @Valid @RequestBody CreateOptionalHolidayRequest request) {
        logger.info("Received request to create optional holiday: {}", request.getName());

        OptionalHoliday holiday = OptionalHoliday.builder()
                .date(request.getDate())
                .name(request.getName())
                .description(request.getDescription())
                .region(request.getRegion())
                .build();

        OptionalHolidayDto result = optionalHolidayService.createHoliday(holiday);

        logger.info("Successfully created optional holiday with id: {}", result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @Operation(
            summary = "Get all optional holidays",
            description = "Retrieves all optional holidays ordered by date ascending. " +
                    "Can be filtered by region using the 'region' query parameter. " +
                    "Returns a list of all holidays or holidays for a specific region.",
            tags = {"Optional Holiday Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Optional holidays successfully retrieved",
                    content = @Content(schema = @Schema(implementation = OptionalHolidayDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<List<OptionalHolidayDto>> getAllHolidays(
            @Parameter(
                    description = "Filter holidays by region (optional)",
                    example = "PUNE",
                    required = false
            )
            @RequestParam(required = false) Region region) {
        logger.info("Fetching optional holidays with region filter: {}", region);

        List<OptionalHolidayDto> result = region != null
                ? optionalHolidayService.getHolidaysByRegion(region)
                : optionalHolidayService.getAllHolidays();

        logger.info("Successfully fetched {} optional holidays", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get optional holiday by ID",
            description = "Retrieves a specific optional holiday by its UUID. " +
                    "Returns 404 if the holiday with the given ID does not exist.",
            tags = {"Optional Holiday Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Optional holiday successfully retrieved",
                    content = @Content(schema = @Schema(implementation = OptionalHolidayDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Optional holiday not found"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid UUID format"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<OptionalHolidayDto> getHolidayById(
            @Parameter(description = "Holiday UUID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        logger.info("Fetching optional holiday by id: {}", id);

        OptionalHolidayDto result = optionalHolidayService.getHolidayById(id);

        if (result == null) {
            logger.warn("Optional holiday not found with id: {}", id);
            throw new IllegalArgumentException("Optional holiday not found with id: " + id);
        }

        logger.info("Successfully fetched optional holiday: {}", result.getName());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update an existing optional holiday",
            description = "Updates an optional holiday by its UUID. All fields must be provided. " +
                    "Returns 404 if the holiday with the given ID does not exist.",
            tags = {"Optional Holiday Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Optional holiday successfully updated",
                    content = @Content(schema = @Schema(implementation = OptionalHolidayDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Optional holiday not found"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or UUID format"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<OptionalHolidayDto> updateHoliday(
            @Parameter(description = "Holiday UUID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Parameter(
                    description = "Updated optional holiday details",
                    required = true,
                    schema = @Schema(implementation = UpdateOptionalHolidayRequest.class)
            )
            @Valid @RequestBody UpdateOptionalHolidayRequest request) {
        logger.info("Received request to update optional holiday with id: {}", id);

        OptionalHoliday holiday = OptionalHoliday.builder()
                .date(request.getDate())
                .name(request.getName())
                .description(request.getDescription())
                .region(request.getRegion())
                .build();

        OptionalHolidayDto result = optionalHolidayService.updateHoliday(id, holiday);

        logger.info("Successfully updated optional holiday: {}", result.getName());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete an optional holiday",
            description = "Deletes an optional holiday by its UUID. This operation cannot be undone. " +
                    "Returns 204 No Content on success. Returns 404 if the holiday does not exist.",
            tags = {"Optional Holiday Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Optional holiday successfully deleted"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Optional holiday not found"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid UUID format"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<Void> deleteHoliday(
            @Parameter(description = "Holiday UUID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        logger.info("Received request to delete optional holiday with id: {}", id);

        // Check if holiday exists before deleting
        if (optionalHolidayService.getHolidayById(id) == null) {
            logger.warn("Optional holiday not found with id: {}", id);
            throw new IllegalArgumentException("Optional holiday not found with id: " + id);
        }

        optionalHolidayService.deleteHoliday(id);

        logger.info("Successfully deleted optional holiday with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
