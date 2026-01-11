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
import one.june.leave_management.application.leave.service.BulkUploadService;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse;
import one.june.leave_management.common.annotation.Auditable;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.common.mapper.LeaveMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/leaves")
@Tag(name = "Leave Management", description = "APIs for managing leave requests and queries")
public class LeaveController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    private final LeaveService leaveService;
    private final LeaveMapper leaveMapper;
    private final BulkUploadService bulkUploadService;

    public LeaveController(LeaveService leaveService,
                           LeaveMapper leaveMapper,
                           BulkUploadService bulkUploadService) {
        this.leaveService = leaveService;
        this.leaveMapper = leaveMapper;
        this.bulkUploadService = bulkUploadService;
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
            description = "Retrieves a paginated list of leave requests. Supports filtering by user name and date range. " +
                    "Results are sorted by startDate DESC (most recent first) and paginated.\n\n" +
                    "**Date Range Filtering:**\n" +
                    "- If startDate and endDate are provided, both must be specified together\n" +
                    "- Returns leaves that overlap with the specified date range (not just leaves completely within the range)\n" +
                    "- Example: A leave from Jan 1-10 will be included when querying for Jan 5-15\n\n" +
                    "**Duration Calculation:**\n" +
                    "- When a date range is provided, `durationInDays` shows the duration within that range only\n" +
                    "- The actual leave start/end dates are never modified\n" +
                    "- Example: A 10-day leave (Mar 1-10) queried for range Mar 1-7 shows durationInDays=7.0\n" +
                    "- Half-day leaves return 0.5 if they overlap with the range\n\n" +
                    "**User Name Filtering:**\n" +
                    "- Searches employees by name or slack display name (partial match, case-insensitive)\n" +
                    "- Returns leaves for all matching employees\n" +
                    "- Example: `?userName=John` matches both \"John Doe\" and \"Johnny Smith\"\n\n" +
                    "**Examples:**\n" +
                    "- `?startDate=2024-01-01&endDate=2024-03-31` - All leaves in Q1 2024\n" +
                    "- `?userName=John&startDate=2024-01-01&endDate=2024-12-31` - John's leaves in 2024",
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
                    description = "Invalid request parameters - e.g., only one of startDate/endDate provided, or startDate > endDate",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            )
    })
    public ResponseEntity<Page<LeaveDto>> fetchLeaves(
            @Parameter(
                    description = "Filter by user name or slack display name (optional, partial match, case-insensitive)",
                    example = "John"
            )
            @RequestParam(required = false) String userName,
            @Parameter(
                    description = "Filter by start date (optional, format: yyyy-MM-dd). " +
                            "If provided, endDate must also be specified. " +
                            "Leaves overlapping with this date will be included.",
                    example = "2024-01-01"
            )
            @RequestParam(required = false) java.time.LocalDate startDate,
            @Parameter(
                    description = "Filter by end date (optional, format: yyyy-MM-dd). " +
                            "If provided, startDate must also be specified. " +
                            "Leaves overlapping with this date will be included.",
                    example = "2024-12-31"
            )
            @RequestParam(required = false) java.time.LocalDate endDate,
            @Parameter(
                    description = "Pagination and sorting parameters (e.g., ?page=0&size=20&sort=startDate,desc)",
                    hidden = true
            )
            @PageableDefault(size = 20) Pageable pageable) {
        logger.info("Fetching leaves with filters - userName: {}, startDate: {}, endDate: {}, pageable: {}",
                    userName, startDate, endDate, pageable);

        LeaveFetchQuery query = LeaveFetchQuery.builder()
                .userName(userName)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // Validate query parameters
        query.validate();

        Page<LeaveDto> result = leaveService.fetchLeaves(query, pageable);

        logger.info("Successfully fetched {} leaves (page {} of {})",
                    result.getNumberOfElements(),
                    result.getNumber() + 1,
                    result.getTotalPages());

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Auditable("Bulk upload leaves endpoint")
    @Operation(
            summary = "Bulk upload leaves from CSV file",
            description = "Uploads a CSV file containing leave data for bulk ingestion. All leaves will be set to APPROVED status. " +
                    "Processing is asynchronous. The CSV file should contain columns: userId, startDate, endDate, type, durationType (optional). " +
                    "startDate and endDate must be in yyyy-MM-dd format. type can be ANNUAL_LEAVE or OPTIONAL_HOLIDAY. " +
                    "durationType can be FULL_DAY, FIRST_HALF, or SECOND_HALF (defaults to FULL_DAY if not provided). " +
                    "Maximum file size is 10MB.",
            tags = {"Leave Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "CSV file accepted for processing. Returns job ID to track progress.",
                    content = @Content(schema = @Schema(implementation = BulkUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file format or CSV validation failed",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            )
    })
    public ResponseEntity<BulkUploadResponse> bulkUploadLeaves(
            @Parameter(
                    description = "CSV file with leave data. Required columns: userId, startDate, endDate, type. Optional column: durationType",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file) {
        logger.info("Received bulk upload request for file: {}", file.getOriginalFilename());

        BulkUploadResponse response = bulkUploadService.initiateBulkUpload(file);

        logger.info("Bulk upload job {} initiated for file: {}", response.getJobId(), file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/bulk-upload/status/{jobId}")
    @Auditable("Bulk upload status endpoint")
    @Operation(
            summary = "Get bulk upload job status",
            description = "Retrieves the current status of a bulk upload job including progress counters. " +
                    "Use this to check if processing is complete and if result file is available for download.",
            tags = {"Leave Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Job status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BulkUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job not found",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            )
    })
    public ResponseEntity<BulkUploadResponse> getBulkUploadStatus(
            @Parameter(description = "Job ID returned from bulk upload endpoint", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String jobId) {
        logger.info("Fetching bulk upload status for job: {}", jobId);

        try {
            java.util.UUID uuid = java.util.UUID.fromString(jobId);
            BulkUploadResponse response = bulkUploadService.getJobStatus(uuid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid job ID format: " + jobId, e);
        }
    }

    @GetMapping("/bulk-download/{jobId}")
    @Auditable("Bulk upload result download endpoint")
    @Operation(
            summary = "Download bulk upload result CSV",
            description = "Downloads the result CSV file for a completed bulk upload job. " +
                    "The result file contains all original columns plus a 'status' column showing SUCCESS or ERROR details. " +
                    "Job must be completed and result file available.",
            tags = {"Leave Management"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Result file downloaded successfully",
                    content = @Content(mediaType = "text/csv")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job not found or result file not available",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.common.exception.ErrorResponse.class))
            )
    })
    public ResponseEntity<Resource> downloadBulkUploadResult(
            @Parameter(description = "Job ID returned from bulk upload endpoint", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String jobId) {
        logger.info("Downloading bulk upload result for job: {}", jobId);

        try {
            java.util.UUID uuid = java.util.UUID.fromString(jobId);
            String resultFilePath = bulkUploadService.getResultFilePath(uuid);

            java.io.File file = new java.io.File(resultFilePath);
            if (!file.exists()) {
                throw new BulkUploadJobNotFoundException(uuid);
            }

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(file.length())
                    .body(resource);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid job ID format: " + jobId, e);
        }
    }
}