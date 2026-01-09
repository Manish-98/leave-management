package one.june.leave_management.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.web.dto.EmployeeCreateRequest;
import one.june.leave_management.adapter.inbound.web.dto.EmployeeUpdateRequest;
import one.june.leave_management.application.employee.command.EmployeeCreateCommand;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.application.employee.service.EmployeeBulkUploadService;
import one.june.leave_management.application.employee.service.EmployeeService;
import one.june.leave_management.common.annotation.Auditable;
import one.june.leave_management.common.exception.ErrorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for Employee CRUD operations.
 * Provides endpoints for managing employee data.
 */
@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employee Management", description = "APIs for managing employee data")
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeBulkUploadService employeeBulkUploadService;

    public EmployeeController(EmployeeService employeeService,
                              EmployeeBulkUploadService employeeBulkUploadService) {
        this.employeeService = employeeService;
        this.employeeBulkUploadService = employeeBulkUploadService;
    }

    @PostMapping
    @Auditable("Employee creation endpoint")
    @Operation(
            summary = "Create a new employee",
            description = "Creates a new employee record. At least one external ID (Slack or Google) is required."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Employee created successfully",
                    content = @Content(schema = @Schema(implementation = EmployeeDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "External ID already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EmployeeDto> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        log.info("Received request to create employee: {}", request.getName());

        EmployeeCreateCommand command = EmployeeCreateCommand.builder()
                .name(request.getName())
                .slackId(request.getSlackId())
                .googleId(request.getGoogleId())
                .slackDisplayName(request.getSlackDisplayName())
                .dateOfJoining(request.getDateOfJoining())
                .region(request.getRegion())
                .active(request.getActive())
                .carryForwardLeaves(request.getCarryForwardLeaves())
                .build();

        EmployeeDto result = employeeService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    @Auditable("Employee fetch by ID endpoint")
    @Operation(
            summary = "Get employee by ID",
            description = "Retrieves an employee record by their unique identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee found",
                    content = @Content(schema = @Schema(implementation = EmployeeDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EmployeeDto> getEmployeeById(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable UUID id) {
        log.info("Fetching employee by id: {}", id);
        EmployeeDto result = employeeService.findById(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/slack/{slackId}")
    @Auditable("Employee fetch by Slack ID endpoint")
    @Operation(
            summary = "Get employee by Slack ID",
            description = "Retrieves an employee record by their Slack ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee found",
                    content = @Content(schema = @Schema(implementation = EmployeeDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EmployeeDto> getEmployeeBySlackId(
            @Parameter(description = "Slack ID", required = true)
            @PathVariable String slackId) {
        log.info("Fetching employee by Slack ID: {}", slackId);
        return employeeService.findBySlackId(slackId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/google/{googleId}")
    @Auditable("Employee fetch by Google ID endpoint")
    @Operation(
            summary = "Get employee by Google ID",
            description = "Retrieves an employee record by their Google ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee found",
                    content = @Content(schema = @Schema(implementation = EmployeeDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EmployeeDto> getEmployeeByGoogleId(
            @Parameter(description = "Google ID", required = true)
            @PathVariable String googleId) {
        log.info("Fetching employee by Google ID: {}", googleId);
        return employeeService.findByGoogleId(googleId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Auditable("Employee list endpoint")
    @Operation(
            summary = "Get all employees",
            description = "Retrieves a paginated list of all employees. Supports filtering by active status and name search."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employees retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    public ResponseEntity<Page<EmployeeDto>> getAllEmployees(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Filter by active status (true/false)")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Search by name (case-insensitive partial match)")
            @RequestParam(required = false) String name) {
        log.info("Fetching employees with filters - page: {}, size: {}, active: {}, name: {}",
                page, size, active, name);

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EmployeeDto> result;
        if (name != null && !name.trim().isEmpty()) {
            result = employeeService.searchByName(name, pageable);
        } else if (active != null) {
            result = employeeService.findByActiveStatus(active, pageable);
        } else {
            result = employeeService.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Auditable("Employee update endpoint")
    @Operation(
            summary = "Update an employee",
            description = "Updates an existing employee record. All fields will be updated with the provided values."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee updated successfully",
                    content = @Content(schema = @Schema(implementation = EmployeeDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "External ID conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EmployeeDto> updateEmployee(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeCreateRequest request) {
        log.info("Received request to update employee: {}", id);

        EmployeeCreateCommand command = EmployeeCreateCommand.builder()
                .id(id)
                .name(request.getName())
                .slackId(request.getSlackId())
                .googleId(request.getGoogleId())
                .slackDisplayName(request.getSlackDisplayName())
                .dateOfJoining(request.getDateOfJoining())
                .region(request.getRegion())
                .active(request.getActive())
                .carryForwardLeaves(request.getCarryForwardLeaves())
                .build();

        EmployeeDto result = employeeService.update(id, command);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @Auditable("Employee deactivate endpoint")
    @Operation(
            summary = "Deactivate an employee (soft delete)",
            description = "Deactivates an employee by setting their active status to false. The record is preserved in the database."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Employee deactivated successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deactivateEmployee(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable UUID id) {
        log.info("Deactivating employee: {}", id);
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // ========== Bulk Upload Endpoints ==========

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Auditable("Employee bulk upload endpoint")
    @Operation(
            summary = "Bulk upload employees from CSV file",
            description = "Uploads a CSV file containing employee data for bulk ingestion. " +
                    "Processing is asynchronous. Required columns: name, dateOfJoining, and (slackId OR googleId). " +
                    "Optional columns: slackDisplayName, active (default: true), carryForwardLeaves (default: 0, uses current year). " +
                    "Headers are case-insensitive and spaces are ignored."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Bulk upload job created successfully",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid CSV file format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse> bulkUploadEmployees(
            @Parameter(
                    description = "CSV file with employee data. Required columns: name, dateOfJoining, and (slackId OR googleId). " +
                            "Optional columns: slackDisplayName, active, carryForwardLeaves",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file) {
        log.info("Received employee bulk upload request for file: {}", file.getOriginalFilename());

        one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse response =
                employeeBulkUploadService.initiateBulkUpload(file);

        log.info("Employee bulk upload job {} initiated for file: {}", response.getJobId(), file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/bulk-upload/status/{jobId}")
    @Auditable("Employee bulk upload status endpoint")
    @Operation(
            summary = "Get employee bulk upload job status",
            description = "Retrieves the current status of a bulk upload job including progress counters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Job status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse> getBulkUploadStatus(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId) {
        log.info("Fetching employee bulk upload status for job: {}", jobId);

        try {
            UUID uuid = UUID.fromString(jobId);
            one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse response =
                    employeeBulkUploadService.getJobStatus(uuid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid job ID format: " + jobId, e);
        }
    }

    @GetMapping("/bulk-download/{jobId}")
    @Auditable("Employee bulk upload result download endpoint")
    @Operation(
            summary = "Download bulk upload result CSV",
            description = "Downloads the result CSV file containing all rows with their status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Result CSV file",
                    content = @Content(mediaType = "text/csv")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job or result file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<byte[]> downloadBulkUploadResult(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId) {
        log.info("Downloading employee bulk upload result for job: {}", jobId);

        try {
            UUID uuid = UUID.fromString(jobId);
            String resultFilePath = employeeBulkUploadService.getResultFilePath(uuid);

            java.io.File file = new java.io.File(resultFilePath);
            if (!file.exists()) {
                throw new one.june.leave_management.common.exception.BulkUploadJobNotFoundException(uuid);
            }

            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"employee-bulk-upload-result-" + jobId + ".csv\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                    .body(fileContent);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid job ID format: " + jobId, e);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read result file", e);
        }
    }
}
