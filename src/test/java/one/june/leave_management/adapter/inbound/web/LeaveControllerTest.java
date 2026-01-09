package one.june.leave_management.adapter.inbound.web;

import one.june.leave_management.adapter.inbound.web.dto.LeaveFetchQuery;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.BulkUploadService;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.common.model.Quarter;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Controller Unit Tests")
class LeaveControllerTest {

    @Mock
    private LeaveService leaveService;

    @Mock
    private LeaveMapper leaveMapper;

    @Mock
    private BulkUploadService bulkUploadService;

    private LeaveController controller;

    @BeforeEach
    void setUp() {
        controller = new LeaveController(leaveService, leaveMapper, bulkUploadService);
    }

    // Helper method to create test employee
    private EmployeeDto createTestEmployee(String slackId, String name) {
        return EmployeeDto.builder()
                .id(UUID.randomUUID())
                .name(name)
                .slackId(slackId)
                .googleId(null)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("POST /api/leaves/ingest - Ingest Leave Tests")
    class IngestLeaveTests {

        @Test
        @DisplayName("Should ingest leave successfully and return 201 CREATED")
        void shouldIngestLeaveSuccessfully() {
            // Given
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .userId("user-123")
                    .dateRange(one.june.leave_management.common.model.DateRange.builder()
                            .startDate(LocalDate.of(2024, 1, 1))
                            .endDate(LocalDate.of(2024, 1, 5))
                            .build())
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("web-123")
                    .userId("user-123")
                    .dateRange(one.june.leave_management.common.model.DateRange.builder()
                            .startDate(LocalDate.of(2024, 1, 1))
                            .endDate(LocalDate.of(2024, 1, 5))
                            .build())
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            EmployeeDto testEmployee = createTestEmployee("user-123", "Test User");

            LeaveDto expectedDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(testEmployee)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            when(leaveMapper.toCommand(request, request.getSourceType(), request.getSourceId()))
                    .thenReturn(command);
            when(leaveService.ingest(command)).thenReturn(expectedDto);

            // When
            ResponseEntity<LeaveDto> response = controller.ingestLeave(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(expectedDto.getId());
            assertThat(response.getBody().getEmployee().getSlackId()).isEqualTo("user-123");
            assertThat(response.getBody().getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(response.getBody().getStatus()).isEqualTo(LeaveStatus.REQUESTED);

            verify(leaveMapper).toCommand(request, request.getSourceType(), request.getSourceId());
            verify(leaveService).ingest(command);
        }

        @Test
        @DisplayName("Should map request to command correctly")
        void shouldMapRequestToCommandCorrectly() {
            // Given
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-456")
                    .userId("user-456")
                    .dateRange(one.june.leave_management.common.model.DateRange.builder()
                            .startDate(LocalDate.of(2024, 6, 1))
                            .endDate(LocalDate.of(2024, 6, 3))
                            .build())
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .status(LeaveStatus.APPROVED)
                    .durationType(LeaveDurationType.FIRST_HALF)
                    .build();

            LeaveIngestionCommand expectedCommand = LeaveIngestionCommand.builder()
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-456")
                    .userId("user-456")
                    .dateRange(one.june.leave_management.common.model.DateRange.builder()
                            .startDate(LocalDate.of(2024, 6, 1))
                            .endDate(LocalDate.of(2024, 6, 3))
                            .build())
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .status(LeaveStatus.APPROVED)
                    .durationType(LeaveDurationType.FIRST_HALF)
                    .build();

            EmployeeDto testEmployee = createTestEmployee("user-456", "Test User 456");

            LeaveDto expectedDto = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(testEmployee)
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .status(LeaveStatus.APPROVED)
                    .durationType(LeaveDurationType.FIRST_HALF)
                    .build();

            when(leaveMapper.toCommand(request, request.getSourceType(), request.getSourceId()))
                    .thenReturn(expectedCommand);
            when(leaveService.ingest(expectedCommand)).thenReturn(expectedDto);

            // When
            ResponseEntity<LeaveDto> response = controller.ingestLeave(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(leaveMapper).toCommand(request, SourceType.SLACK, "slack-456");
            verify(leaveService).ingest(expectedCommand);
        }

        @Test
        @DisplayName("Should handle leave with all duration types")
        void shouldHandleAllDurationTypes() {
            // Given
            for (LeaveDurationType durationType : LeaveDurationType.values()) {
                LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                        .sourceType(SourceType.WEB)
                        .sourceId("web-" + durationType.name())
                        .userId("user-duration")
                        .dateRange(one.june.leave_management.common.model.DateRange.builder()
                                .startDate(LocalDate.of(2024, 1, 1))
                                .endDate(LocalDate.of(2024, 1, 1))
                                .build())
                        .type(LeaveType.ANNUAL_LEAVE)
                        .status(LeaveStatus.REQUESTED)
                        .durationType(durationType)
                        .build();

                LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                        .sourceType(request.getSourceType())
                        .sourceId(request.getSourceId())
                        .userId(request.getUserId())
                        .dateRange(request.getDateRange())
                        .type(request.getType())
                        .status(request.getStatus())
                        .durationType(request.getDurationType())
                        .build();

                EmployeeDto testEmployee = createTestEmployee("user-duration", "Duration User");

                LeaveDto expectedDto = LeaveDto.builder()
                        .id(UUID.randomUUID())
                        .employee(testEmployee)
                        .durationType(durationType)
                        .dateRange(request.getDateRange())
                        .type(request.getType())
                        .status(request.getStatus())
                        .build();

                when(leaveMapper.toCommand(request, request.getSourceType(), request.getSourceId())).thenReturn(command);
                when(leaveService.ingest(command)).thenReturn(expectedDto);

                // When
                ResponseEntity<LeaveDto> response = controller.ingestLeave(request);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody().getDurationType()).isEqualTo(durationType);
            }
        }
    }

    @Nested
    @DisplayName("GET /api/leaves - Fetch Leaves Tests")
    class FetchLeavesTests {

        @Test
        @DisplayName("Should fetch leaves without filters and return 200 OK")
        void shouldFetchLeavesWithoutFilters() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            LeaveFetchQuery query = LeaveFetchQuery.builder().build();

            EmployeeDto employee1 = createTestEmployee("user-1", "User 1");
            EmployeeDto employee2 = createTestEmployee("user-2", "User 2");

            LeaveDto leave1 = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(employee1)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .build();
            LeaveDto leave2 = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(employee2)
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .build();

            Page<LeaveDto> expectedPage = new PageImpl<>(List.of(leave1, leave2), pageable, 2);
            when(leaveService.fetchLeaves(query, pageable)).thenReturn(expectedPage);

            // When
            ResponseEntity<Page<LeaveDto>> response = controller.fetchLeaves(null, null, null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(2);
            assertThat(response.getBody().getTotalElements()).isEqualTo(2);

            verify(leaveService).fetchLeaves(query, pageable);
        }

        @Test
        @DisplayName("Should fetch leaves with userId filter")
        void shouldFetchLeavesWithUserIdFilter() {
            // Given
            String userId = "user-123";
            Pageable pageable = PageRequest.of(0, 20);
            LeaveFetchQuery query = LeaveFetchQuery.builder().userId(userId).build();

            EmployeeDto employee = createTestEmployee(userId, "User 123");

            LeaveDto leave = LeaveDto.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .build();

            Page<LeaveDto> expectedPage = new PageImpl<>(List.of(leave), pageable, 1);
            when(leaveService.fetchLeaves(query, pageable)).thenReturn(expectedPage);

            // When
            ResponseEntity<Page<LeaveDto>> response = controller.fetchLeaves(userId, null, null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).getEmployee().getSlackId()).isEqualTo(userId);

            verify(leaveService).fetchLeaves(query, pageable);
        }

        @Test
        @DisplayName("Should fetch leaves with year and quarter filters")
        void shouldFetchLeavesWithYearAndQuarterFilters() {
            // Given
            Integer year = 2024;
            Quarter quarter = Quarter.Q1;
            Pageable pageable = PageRequest.of(0, 20);
            LeaveFetchQuery query = LeaveFetchQuery.builder()
                    .year(year)
                    .quarter(quarter)
                    .build();

            Page<LeaveDto> expectedPage = new PageImpl<>(List.of(), pageable, 0);
            when(leaveService.fetchLeaves(query, pageable)).thenReturn(expectedPage);

            // When
            ResponseEntity<Page<LeaveDto>> response = controller.fetchLeaves(null, year, quarter, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(leaveService).fetchLeaves(query, pageable);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when quarter provided without year")
        void shouldThrowExceptionWhenQuarterWithoutYear() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);

            // When & Then
            assertThatThrownBy(() ->
                    controller.fetchLeaves(null, null, Quarter.Q1, pageable)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quarter filter requires year parameter to be specified");

            verifyNoInteractions(leaveService);
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            // Given
            Pageable pageable = PageRequest.of(1, 10);
            LeaveFetchQuery query = LeaveFetchQuery.builder().build();

            List<LeaveDto> leaves = List.of(
                    LeaveDto.builder().id(UUID.randomUUID()).build(),
                    LeaveDto.builder().id(UUID.randomUUID()).build(),
                    LeaveDto.builder().id(UUID.randomUUID()).build()
            );
            Page<LeaveDto> expectedPage = new PageImpl<>(leaves, pageable, 25);

            when(leaveService.fetchLeaves(query, pageable)).thenReturn(expectedPage);

            // When
            ResponseEntity<Page<LeaveDto>> response = controller.fetchLeaves(null, null, null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getNumber()).isEqualTo(1);
            assertThat(response.getBody().getSize()).isEqualTo(10);
            assertThat(response.getBody().getTotalPages()).isEqualTo(3);
            assertThat(response.getBody().getTotalElements()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("POST /api/leaves/bulk-upload - Bulk Upload Tests")
    class BulkUploadTests {

        @Test
        @DisplayName("Should accept bulk upload file and return 202 ACCEPTED")
        void shouldAcceptBulkUploadFile() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "leaves.csv",
                    "text/csv",
                    "userId,startDate,endDate,type\ntest-user,2024-01-01,2024-01-05,ANNUAL_LEAVE".getBytes()
            );

            UUID jobId = UUID.randomUUID();
            BulkUploadResponse expectedResponse = BulkUploadResponse.builder()
                    .jobId(jobId)
                    .status("PROCESSING")
                    .build();

            when(bulkUploadService.initiateBulkUpload(any(MultipartFile.class)))
                    .thenReturn(expectedResponse);

            // When
            ResponseEntity<BulkUploadResponse> response = controller.bulkUploadLeaves(file);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getJobId()).isEqualTo(jobId);
            assertThat(response.getBody().getStatus()).isEqualTo("PROCESSING");

            verify(bulkUploadService).initiateBulkUpload(file);
        }

        @Test
        @DisplayName("Should pass file to bulk upload service")
        void shouldPassFileToBulkUploadService() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.csv",
                    "text/csv",
                    "csv data".getBytes()
            );

            BulkUploadResponse expectedResponse = BulkUploadResponse.builder()
                    .jobId(UUID.randomUUID())
                    .build();

            when(bulkUploadService.initiateBulkUpload(file)).thenReturn(expectedResponse);

            // When
            controller.bulkUploadLeaves(file);

            // Then
            verify(bulkUploadService).initiateBulkUpload(file);
        }
    }

    @Nested
    @DisplayName("GET /api/leaves/bulk-upload/status/{jobId} - Bulk Upload Status Tests")
    class BulkUploadStatusTests {

        @Test
        @DisplayName("Should return bulk upload status for valid job ID")
        void shouldReturnBulkUploadStatus() {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadResponse expectedResponse = BulkUploadResponse.builder()
                    .jobId(jobId)
                    .status("PROCESSING")
                    .totalRecords(100)
                    .successfulRecords(50)
                    .failedRecords(0)
                    .build();

            when(bulkUploadService.getJobStatus(jobId)).thenReturn(expectedResponse);

            // When
            ResponseEntity<BulkUploadResponse> response = controller.getBulkUploadStatus(jobId.toString());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getJobId()).isEqualTo(jobId);
            assertThat(response.getBody().getStatus()).isEqualTo("PROCESSING");
            assertThat(response.getBody().getTotalRecords()).isEqualTo(100);
            assertThat(response.getBody().getSuccessfulRecords()).isEqualTo(50);

            verify(bulkUploadService).getJobStatus(jobId);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid UUID format")
        void shouldThrowExceptionForInvalidUuidFormat() {
            // Given
            String invalidJobId = "invalid-uuid-format";

            // When & Then
            assertThatThrownBy(() -> controller.getBulkUploadStatus(invalidJobId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid job ID format");

            verifyNoInteractions(bulkUploadService);
        }

        @Test
        @DisplayName("Should handle completed job status")
        void shouldHandleCompletedJobStatus() {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadResponse expectedResponse = BulkUploadResponse.builder()
                    .jobId(jobId)
                    .status("COMPLETED")
                    .totalRecords(10)
                    .successfulRecords(10)
                    .failedRecords(0)
                    .resultAvailable(true)
                    .build();

            when(bulkUploadService.getJobStatus(jobId)).thenReturn(expectedResponse);

            // When
            ResponseEntity<BulkUploadResponse> response = controller.getBulkUploadStatus(jobId.toString());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo("COMPLETED");
            assertThat(response.getBody().getSuccessfulRecords()).isEqualTo(10);
            assertThat(response.getBody().getResultAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/leaves/bulk-download/{jobId} - Bulk Download Tests")
    class BulkDownloadTests {

        @Test
        @DisplayName("Should download result file for valid job ID")
        void shouldDownloadResultFile() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();

            // Create a temporary file for testing with actual content
            File tempFile = File.createTempFile("test-result", ".csv");
            tempFile.deleteOnExit();
            java.nio.file.Files.writeString(tempFile.toPath(), "test,data\n1,value\n");

            when(bulkUploadService.getResultFilePath(jobId)).thenReturn(tempFile.getAbsolutePath());

            // When
            ResponseEntity<Resource> response = controller.downloadBulkUploadResult(jobId.toString());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
            assertThat(response.getHeaders().getContentDisposition()).isNotNull();
            assertThat(response.getHeaders().getContentLength()).isGreaterThan(0);

            verify(bulkUploadService).getResultFilePath(jobId);
        }

        @Test
        @DisplayName("Should throw BulkUploadJobNotFoundException when file does not exist")
        void shouldThrowExceptionWhenFileDoesNotExist() {
            // Given
            UUID jobId = UUID.randomUUID();
            String nonExistentPath = "/tmp/non-existent-file-" + UUID.randomUUID() + ".csv";

            when(bulkUploadService.getResultFilePath(jobId)).thenReturn(nonExistentPath);

            // When & Then
            assertThatThrownBy(() -> controller.downloadBulkUploadResult(jobId.toString()))
                    .isInstanceOf(BulkUploadJobNotFoundException.class);

            verify(bulkUploadService).getResultFilePath(jobId);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid UUID format")
        void shouldThrowExceptionForInvalidUuidFormatInDownload() {
            // Given
            String invalidJobId = "not-a-uuid";

            // When & Then
            assertThatThrownBy(() -> controller.downloadBulkUploadResult(invalidJobId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid job ID format");

            verifyNoInteractions(bulkUploadService);
        }

        @Test
        @DisplayName("Should set correct headers for file download")
        void shouldSetCorrectHeadersForFileDownload() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            File tempFile = File.createTempFile("test-download", ".csv");
            tempFile.deleteOnExit();

            when(bulkUploadService.getResultFilePath(jobId)).thenReturn(tempFile.getAbsolutePath());

            // When
            ResponseEntity<Resource> response = controller.downloadBulkUploadResult(jobId.toString());

            // Then
            assertThat(response.getHeaders().getContentType()).isNotNull();
            assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
            assertThat(response.getHeaders().getContentDisposition()).isNotNull();
            assertThat(response.getHeaders().getContentDisposition().toString()).contains("attachment");
            assertThat(response.getHeaders().getContentLength()).isEqualTo(tempFile.length());
        }
    }
}
