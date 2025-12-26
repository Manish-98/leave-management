package one.june.leave_management.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import one.june.leave_management.adapter.inbound.web.dto.LeaveFetchQuery;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.common.annotation.Auditable;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.common.model.Quarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
@Tag(name = "Leave Management", description = "APIs for managing leave requests and queries")
public class LeaveController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    private final LeaveService leaveService;
    private final LeaveMapper leaveMapper;

    public LeaveController(LeaveService leaveService,
                           LeaveMapper leaveMapper) {
        this.leaveService = leaveService;
        this.leaveMapper = leaveMapper;
    }

    @PostMapping("/ingest")
    @Auditable("Leave ingestion endpoint")
    @Operation(
            summary = "Create a new leave request",
            description = "Ingests a new leave request into the system. Supports creating leaves from various sources " +
                    "like web, Slack, calendar, or Kimai. The leave will be validated and stored with the requested status.",
            tags = {"Leave Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Leave request successfully created",
                    content = @Content(schema = @Schema(implementation = LeaveDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data - validation failed",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            )
    })
    public ResponseEntity<LeaveDto> ingestLeave(
            @Parameter(
                    description = "Leave request details with user information, dates, type, and duration",
                    required = true,
                    schema = @Schema(implementation = LeaveIngestionRequest.class)
            )
            @Valid @RequestBody LeaveIngestionRequest request) {
        logger.info("Received leave ingestion request: {}", request);
        LeaveIngestionCommand command = leaveMapper.toCommand(request, request.getSourceType(), request.getSourceId());
        LeaveDto result = leaveService.ingest(command);
        logger.info("Successfully ingested leave with id: {}", result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @Auditable("Fetch leaves endpoint")
    @Operation(
            summary = "Fetch leave requests with optional filters",
            description = "Retrieves a paginated list of leave requests. Supports filtering by user ID, year, and quarter. " +
                    "Results are sorted and paginated. Quarter filter requires year to be specified.",
            tags = {"Leave Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Leave requests successfully retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters - e.g., quarter specified without year",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            )
    })
    public ResponseEntity<Page<LeaveDto>> fetchLeaves(
            @Parameter(description = "Filter by user ID (optional)", example = "user123")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by year (optional, required when using quarter)", example = "2024")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by quarter (optional, requires year to be specified)", example = "Q1")
            @RequestParam(required = false) Quarter quarter,
            @Parameter(description = "Pagination and sorting parameters", hidden = true)
            @PageableDefault(size = 20) Pageable pageable) {
        logger.info("Fetching leaves with filters - userId: {}, year: {}, quarter: {}, pageable: {}",
                    userId, year, quarter, pageable);

        // Validate that quarter is only used with year
        if (quarter != null && year == null) {
            throw new IllegalArgumentException("Quarter filter requires year parameter to be specified");
        }

        LeaveFetchQuery query = LeaveFetchQuery.builder()
                .userId(userId)
                .year(year)
                .quarter(quarter)
                .build();

        Page<LeaveDto> result = leaveService.fetchLeaves(query, pageable);

        logger.info("Successfully fetched {} leaves (page {} of {})",
                    result.getNumberOfElements(),
                    result.getNumber() + 1,
                    result.getTotalPages());

        return ResponseEntity.ok(result);
    }
}