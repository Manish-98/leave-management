package one.june.leave_management.application.leave.service;

import one.june.leave_management.adapter.inbound.web.csv.CsvLeaveParserStrategy;
import one.june.leave_management.adapter.inbound.web.csv.ParsedResult;
import one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.test.util.CsvTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk Upload Service Unit Tests")
class BulkUploadServiceTest {

    @Mock
    private CsvLeaveParserStrategy csvLeaveParserStrategy;

    @Mock
    private BulkUploadJobRepository bulkUploadJobRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BulkUploadService bulkUploadService;

    @BeforeEach
    void setUp() {
        bulkUploadService = new BulkUploadService(csvLeaveParserStrategy, bulkUploadJobRepository, eventPublisher);
    }

    @Test
    @DisplayName("Should successfully initiate bulk upload with valid CSV file")
    void shouldSuccessfullyInitiateBulkUpload() throws IOException {
        // Given
        List<CsvTestUtil.CsvLeaveRecord> records = List.of(
                CsvTestUtil.CsvLeaveRecord.builder()
                        .userId("user1")
                        .startDate("2024-01-01")
                        .endDate("2024-01-05")
                        .type("ANNUAL_LEAVE")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidCsvFile("test", records);

        List<LeaveIngestionCommand> commands = List.of(
                LeaveIngestionCommand.builder()
                        .userId("user1")
                        .sourceType(SourceType.BULK_UPLOAD)
                        .sourceId("csv-bulk-123-1")
                        .type(LeaveType.ANNUAL_LEAVE)
                        .status(LeaveStatus.APPROVED)
                        .durationType(LeaveDurationType.FULL_DAY)
                        .build()
        );

        // Create ParsedResult objects
        List<ParsedResult<LeaveIngestionCommand>> parsedResults = List.of(
                ParsedResult.success(commands.get(0), Map.of("userid", "user1", "startdate", "2024-01-01", "enddate", "2024-01-05", "type", "ANNUAL_LEAVE"), 1)
        );

        when(csvLeaveParserStrategy.parse(eq(file), any(String.class))).thenReturn(parsedResults);
        when(bulkUploadJobRepository.save(any(BulkUploadJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BulkUploadResponse response = bulkUploadService.initiateBulkUpload(file);

        // Then - Verify response
        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getTotalRecords()).isEqualTo(1);
        assertThat(response.getSuccessfulRecords()).isEqualTo(0);
        assertThat(response.getFailedRecords()).isEqualTo(0);
        assertThat(response.getResultAvailable()).isFalse();

        // Verify parser called
        verify(csvLeaveParserStrategy).parse(eq(file), any(String.class));

        // Verify job saved
        ArgumentCaptor<BulkUploadJob> jobCaptor = ArgumentCaptor.forClass(BulkUploadJob.class);
        verify(bulkUploadJobRepository).save(jobCaptor.capture());

        BulkUploadJob savedJob = jobCaptor.getValue();
        assertThat(savedJob.getStatus()).isEqualTo(BulkUploadJob.BulkUploadStatus.PROCESSING);
        assertThat(savedJob.getTotalRecords()).isEqualTo(1);
        assertThat(savedJob.getFileName()).isEqualTo("test.csv");
    }

    @Test
    @DisplayName("Should throw exception when file is empty")
    void shouldThrowWhenFileIsEmpty() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createEmptyCsvFile("empty.csv");

        // When & Then
        assertThatThrownBy(() -> bulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");

        verify(csvLeaveParserStrategy, never()).parse(any(), any());
        verify(bulkUploadJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when file is not CSV")
    void shouldThrowWhenFileIsNotCsv() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createNonCsvFile("test.txt");

        // When & Then
        assertThatThrownBy(() -> bulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only CSV files are allowed");

        verify(csvLeaveParserStrategy, never()).parse(any(), any());
        verify(bulkUploadJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when file exceeds size limit")
    void shouldThrowWhenFileExceedsSizeLimit() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createOversizedCsvFile("large.csv");

        // When & Then
        assertThatThrownBy(() -> bulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum limit of 10MB");

        verify(csvLeaveParserStrategy, never()).parse(any(), any());
        verify(bulkUploadJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when filename is null")
    void shouldThrowWhenFilenameIsNull() throws IOException {
        // Given
        MultipartFile file = new MockMultipartFile("file", new byte[0]);

        // When & Then
        assertThatThrownBy(() -> bulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");

        verify(csvLeaveParserStrategy, never()).parse(any(), any());
        verify(bulkUploadJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return job status for existing job")
    void shouldReturnJobStatusForExistingJob() {
        // Given
        UUID jobId = UUID.randomUUID();
        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .status(BulkUploadJob.BulkUploadStatus.PROCESSING)
                .totalRecords(10)
                .successfulRecords(5)
                .failedRecords(2)
                .resultFilePath("/path/to/result.csv")
                .build();

        when(bulkUploadJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));

        // When
        BulkUploadResponse response = bulkUploadService.getJobStatus(jobId);

        // Then
        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getTotalRecords()).isEqualTo(10);
        assertThat(response.getSuccessfulRecords()).isEqualTo(5);
        assertThat(response.getFailedRecords()).isEqualTo(2);
        assertThat(response.getResultAvailable()).isTrue();

        verify(bulkUploadJobRepository).findById(jobId);
    }

    @Test
    @DisplayName("Should throw exception when job not found")
    void shouldThrowWhenJobNotFound() {
        // Given
        UUID jobId = UUID.randomUUID();
        when(bulkUploadJobRepository.findById(jobId)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bulkUploadService.getJobStatus(jobId))
                .isInstanceOf(BulkUploadJobNotFoundException.class)
                .hasMessageContaining(jobId.toString());

        verify(bulkUploadJobRepository).findById(jobId);
    }

    @Test
    @DisplayName("Should return result file path when available")
    void shouldReturnResultFilePathWhenAvailable() {
        // Given
        UUID jobId = UUID.randomUUID();
        String expectedPath = "/path/to/result.csv";
        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .status(BulkUploadJob.BulkUploadStatus.COMPLETED)
                .resultFilePath(expectedPath)
                .build();

        when(bulkUploadJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));

        // When
        String actualPath = bulkUploadService.getResultFilePath(jobId);

        // Then
        assertThat(actualPath).isEqualTo(expectedPath);
        verify(bulkUploadJobRepository).findById(jobId);
    }

    @Test
    @DisplayName("Should throw exception when result file not available")
    void shouldThrowWhenResultFileNotAvailable() {
        // Given
        UUID jobId = UUID.randomUUID();
        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .status(BulkUploadJob.BulkUploadStatus.PROCESSING)
                .resultFilePath(null)
                .build();

        when(bulkUploadJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));

        // When & Then
        assertThatThrownBy(() -> bulkUploadService.getResultFilePath(jobId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Result file not available");

        verify(bulkUploadJobRepository).findById(jobId);
    }

    @Test
    @DisplayName("Should handle exception during CSV parsing")
    void shouldHandleExceptionDuringParsing() throws IOException {
        // Given
        List<CsvTestUtil.CsvLeaveRecord> records = List.of(
                CsvTestUtil.CsvLeaveRecord.builder()
                        .userId("user1")
                        .startDate("2024-01-01")
                        .endDate("2024-01-05")
                        .type("ANNUAL_LEAVE")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidCsvFile("test.csv", records);

        when(csvLeaveParserStrategy.parse(eq(file), any(String.class)))
                .thenThrow(new RuntimeException("Parse error"));

        // When & Then
        assertThatThrownBy(() -> bulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse CSV file");

        verify(bulkUploadJobRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}

