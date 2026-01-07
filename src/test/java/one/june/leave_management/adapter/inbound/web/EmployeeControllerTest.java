package one.june.leave_management.adapter.inbound.web;

import one.june.leave_management.adapter.inbound.web.dto.EmployeeCreateRequest;
import one.june.leave_management.application.employee.command.EmployeeCreateCommand;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.application.employee.service.EmployeeBulkUploadService;
import one.june.leave_management.application.employee.service.EmployeeService;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.common.exception.DuplicateExternalIdException;
import one.june.leave_management.common.exception.EmployeeNotFoundException;
import one.june.leave_management.domain.common.model.Region;
import one.june.leave_management.test.builder.EmployeeTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmployeeController.
 * Tests the REST API layer for employee CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeController Tests")
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private EmployeeBulkUploadService employeeBulkUploadService;

    private EmployeeController controller;

    private EmployeeDto testEmployeeDto;
    private UUID testEmployeeId;

    @BeforeEach
    void setUp() {
        controller = new EmployeeController(employeeService, employeeBulkUploadService);
        testEmployeeId = UUID.randomUUID();
        testEmployeeDto = EmployeeTestDataBuilder.defaultDto()
                .id(testEmployeeId)
                .build();
    }

    // ==================== Create Employee Endpoint Tests ====================

    @Nested
    @DisplayName("POST /api/employees - Create Employee Tests")
    class CreateEmployeeTests {

        @Test
        @DisplayName("Should create employee and return 201")
        void shouldCreateEmployeeSuccessfully() {
            // Given
            EmployeeCreateRequest request = new EmployeeCreateRequest(
                    testEmployeeDto.getName(),
                    testEmployeeDto.getSlackId(),
                    testEmployeeDto.getGoogleId(),
                    testEmployeeDto.getSlackDisplayName(),
                    testEmployeeDto.getDateOfJoining(),
                    testEmployeeDto.getActive(),
                    Region.PUNE,
                    testEmployeeDto.getCarryForwardLeaves()
            );

            when(employeeService.create(any(EmployeeCreateCommand.class))).thenReturn(testEmployeeDto);

            // When
            ResponseEntity<EmployeeDto> response = controller.createEmployee(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(testEmployeeId);
            assertThat(response.getBody().getName()).isEqualTo(testEmployeeDto.getName());

            verify(employeeService).create(any(EmployeeCreateCommand.class));
        }

        @Test
        @DisplayName("Should return 409 when creating employee with duplicate Slack ID")
        void shouldReturnConflictWhenDuplicateSlackId() {
            // Given
            EmployeeCreateRequest request = new EmployeeCreateRequest(
                    "Test User",
                    "U12345",
                    "test@example.com",
                    "Test",
                    java.time.LocalDate.of(2020, 1, 1),
                    true,
                    Region.PUNE,
                    new HashMap<>()
            );

            when(employeeService.create(any(EmployeeCreateCommand.class)))
                    .thenThrow(new DuplicateExternalIdException("slackId", "U12345", null));

            // When & Then
            assertThatThrownBy(() -> controller.createEmployee(request))
                    .isInstanceOf(DuplicateExternalIdException.class)
                    .hasMessageContaining("slackId");

            verify(employeeService).create(any(EmployeeCreateCommand.class));
        }

        @Test
        @DisplayName("Should return 409 when creating employee with duplicate Google ID")
        void shouldReturnConflictWhenDuplicateGoogleId() {
            // Given
            EmployeeCreateRequest request = new EmployeeCreateRequest(
                    "Test User",
                    "U12345",
                    "test@example.com",
                    "Test",
                    java.time.LocalDate.of(2020, 1, 1),
                    true,
                    Region.PUNE,
                    new HashMap<>()
            );

            when(employeeService.create(any(EmployeeCreateCommand.class)))
                    .thenThrow(new DuplicateExternalIdException("googleId", "test@example.com", null));

            // When & Then
            assertThatThrownBy(() -> controller.createEmployee(request))
                    .isInstanceOf(DuplicateExternalIdException.class)
                    .hasMessageContaining("googleId");

            verify(employeeService).create(any(EmployeeCreateCommand.class));
        }
    }

    // ==================== Get Employee Endpoint Tests ====================

    @Nested
    @DisplayName("GET /api/employees/{id} - Get Employee by ID Tests")
    class GetEmployeeByIdTests {

        @Test
        @DisplayName("Should get employee by ID and return 200")
        void shouldGetEmployeeByIdSuccessfully() {
            // Given
            when(employeeService.findById(testEmployeeId)).thenReturn(testEmployeeDto);

            // When
            ResponseEntity<EmployeeDto> response = controller.getEmployeeById(testEmployeeId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(testEmployeeId);

            verify(employeeService).findById(testEmployeeId);
        }

        @Test
        @DisplayName("Should return 404 when getting non-existent employee by ID")
        void shouldReturnNotFoundWhenEmployeeDoesNotExist() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(employeeService.findById(nonExistentId))
                    .thenThrow(new EmployeeNotFoundException(nonExistentId));

            // When & Then
            assertThatThrownBy(() -> controller.getEmployeeById(nonExistentId))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(employeeService).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("GET /api/employees/slack/{slackId} - Get Employee by Slack ID Tests")
    class GetEmployeeBySlackIdTests {

        @Test
        @DisplayName("Should get employee by Slack ID and return 200")
        void shouldGetEmployeeBySlackIdSuccessfully() {
            // Given
            String slackId = "U12345";
            when(employeeService.findBySlackId(slackId)).thenReturn(Optional.of(testEmployeeDto));

            // When
            ResponseEntity<EmployeeDto> response = controller.getEmployeeBySlackId(slackId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(testEmployeeId);

            verify(employeeService).findBySlackId(slackId);
        }

        @Test
        @DisplayName("Should return 404 when getting non-existent Slack ID")
        void shouldReturnNotFoundWhenSlackIdDoesNotExist() {
            // Given
            String slackId = "NONEXISTENT";
            when(employeeService.findBySlackId(slackId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<EmployeeDto> response = controller.getEmployeeBySlackId(slackId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(employeeService).findBySlackId(slackId);
        }
    }

    @Nested
    @DisplayName("GET /api/employees/google/{googleId} - Get Employee by Google ID Tests")
    class GetEmployeeByGoogleIdTests {

        @Test
        @DisplayName("Should get employee by Google ID and return 200")
        void shouldGetEmployeeByGoogleIdSuccessfully() {
            // Given
            String googleId = "john.doe@example.com";
            when(employeeService.findByGoogleId(googleId)).thenReturn(Optional.of(testEmployeeDto));

            // When
            ResponseEntity<EmployeeDto> response = controller.getEmployeeByGoogleId(googleId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(testEmployeeId);

            verify(employeeService).findByGoogleId(googleId);
        }

        @Test
        @DisplayName("Should return 404 when getting non-existent Google ID")
        void shouldReturnNotFoundWhenGoogleIdDoesNotExist() {
            // Given
            String googleId = "nonexistent@example.com";
            when(employeeService.findByGoogleId(googleId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<EmployeeDto> response = controller.getEmployeeByGoogleId(googleId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(employeeService).findByGoogleId(googleId);
        }
    }

    // ==================== List Employees Endpoint Tests ====================

    @Nested
    @DisplayName("GET /api/employees - Get All Employees Tests")
    class GetAllEmployeesTests {

        @Test
        @DisplayName("Should get all employees with default pagination")
        void shouldGetAllEmployeesWithDefaultPagination() {
            // Given
            Page<EmployeeDto> page = new PageImpl<>(List.of(testEmployeeDto));
            when(employeeService.findAll(any(Pageable.class))).thenReturn(page);

            // When
            ResponseEntity<Page<EmployeeDto>> response = controller.getAllEmployees(0, 20, "name", "asc", null, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);

            verify(employeeService).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter employees by active status")
        void shouldFilterEmployeesByActiveStatus() {
            // Given
            Page<EmployeeDto> page = new PageImpl<>(List.of(testEmployeeDto));
            when(employeeService.findByActiveStatus(eq(true), any(Pageable.class))).thenReturn(page);

            // When
            ResponseEntity<Page<EmployeeDto>> response = controller.getAllEmployees(0, 20, "name", "asc", true, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(employeeService).findByActiveStatus(eq(true), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search employees by name")
        void shouldSearchEmployeesByName() {
            // Given
            String name = "John";
            Page<EmployeeDto> page = new PageImpl<>(List.of(testEmployeeDto));
            when(employeeService.searchByName(eq(name), any(Pageable.class))).thenReturn(page);

            // When
            ResponseEntity<Page<EmployeeDto>> response = controller.getAllEmployees(0, 20, "name", "asc", null, name);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(employeeService).searchByName(eq(name), any(Pageable.class));
        }
    }

    // ==================== Update Employee Endpoint Tests ====================

    @Nested
    @DisplayName("PUT /api/employees/{id} - Update Employee Tests")
    class UpdateEmployeeTests {

        @Test
        @DisplayName("Should update employee and return 200")
        void shouldUpdateEmployeeSuccessfully() {
            // Given
            EmployeeDto updatedDto = EmployeeTestDataBuilder.defaultDto()
                    .id(testEmployeeId)
                    .name("Updated Name")
                    .build();

            EmployeeCreateRequest request = new EmployeeCreateRequest(
                    "Updated Name",
                    testEmployeeDto.getSlackId(),
                    testEmployeeDto.getGoogleId(),
                    testEmployeeDto.getSlackDisplayName(),
                    testEmployeeDto.getDateOfJoining(),
                    testEmployeeDto.getActive(),
                    Region.PUNE,
                    testEmployeeDto.getCarryForwardLeaves()
            );

            when(employeeService.update(eq(testEmployeeId), any(EmployeeCreateCommand.class))).thenReturn(updatedDto);

            // When
            ResponseEntity<EmployeeDto> response = controller.updateEmployee(testEmployeeId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getName()).isEqualTo("Updated Name");

            verify(employeeService).update(eq(testEmployeeId), any(EmployeeCreateCommand.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent employee")
        void shouldReturnNotFoundWhenUpdatingNonExistentEmployee() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            EmployeeCreateRequest request = new EmployeeCreateRequest(
                    "Test",
                    "U12345",
                    "test@example.com",
                    "Test",
                    java.time.LocalDate.of(2020, 1, 1),
                    true,
                    Region.PUNE,
                    new HashMap<>()
            );

            when(employeeService.update(eq(nonExistentId), any(EmployeeCreateCommand.class)))
                    .thenThrow(new EmployeeNotFoundException(nonExistentId));

            // When & Then
            assertThatThrownBy(() -> controller.updateEmployee(nonExistentId, request))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(employeeService).update(eq(nonExistentId), any(EmployeeCreateCommand.class));
        }

        @Test
        @DisplayName("Should return 409 when updating with duplicate external ID")
        void shouldReturnConflictWhenUpdatingWithDuplicateExternalId() {
            // Given
            EmployeeCreateRequest request = new EmployeeCreateRequest(
                    "Test",
                    "U12345",
                    "test@example.com",
                    "Test",
                    java.time.LocalDate.of(2020, 1, 1),
                    true,
                    Region.PUNE,
                    new HashMap<>()
            );

            when(employeeService.update(eq(testEmployeeId), any(EmployeeCreateCommand.class)))
                    .thenThrow(new DuplicateExternalIdException("slackId", "U12345", testEmployeeId));

            // When & Then
            assertThatThrownBy(() -> controller.updateEmployee(testEmployeeId, request))
                    .isInstanceOf(DuplicateExternalIdException.class);

            verify(employeeService).update(eq(testEmployeeId), any(EmployeeCreateCommand.class));
        }
    }

    // ==================== Deactivate Employee Endpoint Tests ====================

    @Nested
    @DisplayName("DELETE /api/employees/{id} - Deactivate Employee Tests")
    class DeactivateEmployeeTests {

        @Test
        @DisplayName("Should deactivate employee and return 204")
        void shouldDeactivateEmployeeSuccessfully() {
            // Given
            doNothing().when(employeeService).deactivate(testEmployeeId);

            // When
            ResponseEntity<Void> response = controller.deactivateEmployee(testEmployeeId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            verify(employeeService).deactivate(testEmployeeId);
        }

        @Test
        @DisplayName("Should return 404 when deactivating non-existent employee")
        void shouldReturnNotFoundWhenDeactivatingNonExistentEmployee() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            doThrow(new EmployeeNotFoundException(nonExistentId))
                    .when(employeeService).deactivate(nonExistentId);

            // When & Then
            assertThatThrownBy(() -> controller.deactivateEmployee(nonExistentId))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(employeeService).deactivate(nonExistentId);
        }
    }

    // ==================== Bulk Upload Endpoint Tests ====================

    @Nested
    @DisplayName("POST /api/employees/bulk-upload - Bulk Upload Tests")
    class BulkUploadTests {

        @Test
        @DisplayName("Should initiate bulk upload and return 202")
        void shouldInitiateBulkUploadSuccessfully() {
            // Given
            MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "test,data".getBytes());

            one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse response =
                    one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse.builder()
                            .jobId(testEmployeeId)
                            .status("Processing")
                            .totalRecords(0)
                            .successfulRecords(0)
                            .failedRecords(0)
                            .resultAvailable(false)
                            .build();

            when(employeeBulkUploadService.initiateBulkUpload(any(MultipartFile.class))).thenReturn(response);

            // When
            ResponseEntity<one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse> result =
                    controller.bulkUploadEmployees(file);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getJobId()).isEqualTo(testEmployeeId);

            verify(employeeBulkUploadService).initiateBulkUpload(any(MultipartFile.class));
        }

        @Test
        @DisplayName("Should get bulk upload status and return 200")
        void shouldGetBulkUploadStatusSuccessfully() {
            // Given
            one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse response =
                    one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse.builder()
                            .jobId(testEmployeeId)
                            .status("Completed")
                            .totalRecords(10)
                            .successfulRecords(10)
                            .failedRecords(0)
                            .resultAvailable(true)
                            .build();

            when(employeeBulkUploadService.getJobStatus(testEmployeeId)).thenReturn(response);

            // When
            ResponseEntity<one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse> result =
                    controller.getBulkUploadStatus(testEmployeeId.toString());

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getStatus()).isEqualTo("Completed");

            verify(employeeBulkUploadService).getJobStatus(testEmployeeId);
        }

        @Test
        @DisplayName("Should return 404 when getting status for non-existent job")
        void shouldReturnNotFoundWhenGettingStatusForNonExistentJob() {
            // Given
            UUID nonExistentJobId = UUID.randomUUID();
            when(employeeBulkUploadService.getJobStatus(nonExistentJobId))
                    .thenThrow(new BulkUploadJobNotFoundException(nonExistentJobId));

            // When & Then
            assertThatThrownBy(() -> controller.getBulkUploadStatus(nonExistentJobId.toString()))
                    .isInstanceOf(BulkUploadJobNotFoundException.class);

            verify(employeeBulkUploadService).getJobStatus(nonExistentJobId);
        }

        @Test
        @DisplayName("Should download bulk upload result and return 200")
        void shouldDownloadBulkUploadResultSuccessfully() {
            // Given
            String resultFilePath = "/tmp/test-result.csv";
            when(employeeBulkUploadService.getResultFilePath(testEmployeeId)).thenReturn(resultFilePath);

            // Create a temporary file for testing
            java.io.File tempFile = new java.io.File(resultFilePath);
            try {
                tempFile.createNewFile();
                java.nio.file.Files.write(tempFile.toPath(), "test,data".getBytes());

                // When
                ResponseEntity<byte[]> result = controller.downloadBulkUploadResult(testEmployeeId.toString());

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
            } catch (Exception e) {
                // Clean up on error
                if (tempFile.exists()) tempFile.delete();
            } finally {
                // Clean up
                if (tempFile.exists()) tempFile.delete();
            }

            verify(employeeBulkUploadService).getResultFilePath(testEmployeeId);
        }

        @Test
        @DisplayName("Should return 404 when downloading result for non-existent job")
        void shouldReturnNotFoundWhenDownloadingResultForNonExistentJob() {
            // Given
            UUID nonExistentJobId = UUID.randomUUID();
            when(employeeBulkUploadService.getResultFilePath(nonExistentJobId))
                    .thenThrow(new BulkUploadJobNotFoundException(nonExistentJobId));

            // When & Then
            assertThatThrownBy(() -> controller.downloadBulkUploadResult(nonExistentJobId.toString()))
                    .isInstanceOf(BulkUploadJobNotFoundException.class);

            verify(employeeBulkUploadService).getResultFilePath(nonExistentJobId);
        }
    }
}
