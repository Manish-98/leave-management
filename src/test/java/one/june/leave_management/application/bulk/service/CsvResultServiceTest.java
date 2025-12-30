package one.june.leave_management.application.bulk.service;

import one.june.leave_management.adapter.inbound.web.config.FileStorageConfig;
import one.june.leave_management.application.bulk.strategy.BulkUploadStrategy;
import one.june.leave_management.application.bulk.strategy.BulkUploadStrategyRegistry;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CSV Result Service Unit Tests")
class CsvResultServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageConfig fileStorageConfig;
    private BulkUploadRecordRepository bulkUploadRecordRepository;
    private BulkUploadJobRepository bulkUploadJobRepository;
    private BulkUploadStrategyRegistry strategyRegistry;
    private CsvResultService csvResultService;

    private BulkUploadStrategy leaveStrategy;

    @BeforeEach
    void setUp() {
        fileStorageConfig = mock(FileStorageConfig.class);
        bulkUploadRecordRepository = mock(BulkUploadRecordRepository.class);
        bulkUploadJobRepository = mock(BulkUploadJobRepository.class);
        strategyRegistry = mock(BulkUploadStrategyRegistry.class);

        when(fileStorageConfig.getBulkUploadResultPath()).thenReturn(tempDir);

        // Mock the strategy for LEAVE type
        leaveStrategy = mock(BulkUploadStrategy.class);
        when(leaveStrategy.getResultHeaders()).thenReturn(new String[]{"userId", "startDate", "endDate", "type", "durationType", "status"});
        when(strategyRegistry.getStrategy(BulkUploadType.LEAVE)).thenReturn(leaveStrategy);

        // Mock generateResultRow to return a CSV row based on metadata
        when(leaveStrategy.generateResultRow(any())).thenAnswer(invocation -> {
            BulkUploadRecord record = invocation.getArgument(0);
            Map<String, String> metadata = record.getMetadata();
            String status = record.getStatus().toString();
            String errorMessage = record.getErrorMessage();

            StringBuilder row = new StringBuilder();
            row.append(metadata.getOrDefault("userid", ""));
            row.append(",").append(metadata.getOrDefault("startdate", ""));
            row.append(",").append(metadata.getOrDefault("enddate", ""));
            row.append(",").append(metadata.getOrDefault("type", ""));
            row.append(",").append(metadata.getOrDefault("durationtype", ""));
            row.append(",").append(status);
            if (errorMessage != null && !errorMessage.isEmpty()) {
                row.append(",").append(errorMessage);
            }
            return row.toString();
        });

        csvResultService = new CsvResultService(fileStorageConfig, bulkUploadRecordRepository,
                bulkUploadJobRepository, strategyRegistry);
    }

    @Test
    @DisplayName("Should generate result file with successful records")
    void shouldGenerateResultFileWithSuccessfulRecords() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .status(BulkUploadJob.BulkUploadStatus.COMPLETED)
                .totalRecords(2)
                .successfulRecords(2)
                .failedRecords(0)
                .build();

        BulkUploadRecord record1 = BulkUploadRecord.builder()
                .id(1L)
                .job(job)
                .rowNumber(2)
                .metadata(createTestMetadata("user1"))
                .status(BulkUploadRecord.BulkRecordStatus.SUCCESS)
                .build();


        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>(List.of(record1)));

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).isNotNull();
        Path resultFile = Path.of(resultPath);
        assertThat(Files.exists(resultFile)).isTrue();

        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines).hasSize(2); // header + 1 data row
        assertThat(lines.get(0)).contains("userId");
        assertThat(lines.get(0)).contains("startDate");
        assertThat(lines.get(0)).contains("status");
        assertThat(lines.get(1)).contains("user1");
        assertThat(lines.get(1)).contains("2024-01-01");
        assertThat(lines.get(1)).contains("ANNUAL_LEAVE");
        assertThat(lines.get(1)).contains("SUCCESS");
    }

    @Test
    @DisplayName("Should generate result file with failed records")
    void shouldGenerateResultFileWithFailedRecords() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .status(BulkUploadJob.BulkUploadStatus.FAILED)
                .totalRecords(1)
                .successfulRecords(0)
                .failedRecords(1)
                .build();

        BulkUploadRecord record1 = BulkUploadRecord.builder()
                .id(1L)
                .job(job)
                .rowNumber(2)
                .metadata(createTestMetadata("user1"))
                .status(BulkUploadRecord.BulkRecordStatus.ERROR)
                .errorMessage("Invalid date format")
                .build();

        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>(List.of(record1)));

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).isNotNull();
        Path resultFile = Path.of(resultPath);
        assertThat(Files.exists(resultFile)).isTrue();

        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines).hasSize(2); // header + 1 data row
        assertThat(lines.get(1)).contains("user1");
        assertThat(lines.get(1)).contains("ERROR");
        assertThat(lines.get(1)).contains("Invalid date format");
    }

    @Test
    @DisplayName("Should generate result file with mixed records")
    void shouldGenerateResultFileWithMixedRecords() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .status(BulkUploadJob.BulkUploadStatus.COMPLETED)
                .totalRecords(3)
                .successfulRecords(2)
                .failedRecords(1)
                .build();

        BulkUploadRecord record1 = BulkUploadRecord.builder()
                .id(1L)
                .job(job)
                .rowNumber(2)
                .metadata(createTestMetadata("user1"))
                .status(BulkUploadRecord.BulkRecordStatus.SUCCESS)
                .build();

        BulkUploadRecord record2 = BulkUploadRecord.builder()
                .id(2L)
                .job(job)
                .rowNumber(3)
                .metadata(createTestMetadata("user2"))
                .status(BulkUploadRecord.BulkRecordStatus.ERROR)
                .errorMessage("Invalid leave type")
                .build();


        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>(List.of(record1, record2)));

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).isNotNull();
        Path resultFile = Path.of(resultPath);
        assertThat(Files.exists(resultFile)).isTrue();

        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines).hasSize(3); // header + 2 data rows
        assertThat(lines.get(1)).contains("user1");
        assertThat(lines.get(1)).contains("SUCCESS");
        assertThat(lines.get(2)).contains("user2");
        assertThat(lines.get(2)).contains("ERROR");
    }

    @Test
    @DisplayName("Should generate result file with empty records list")
    void shouldGenerateResultFileWithEmptyRecords() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .status(BulkUploadJob.BulkUploadStatus.COMPLETED)
                .totalRecords(0)
                .successfulRecords(0)
                .failedRecords(0)
                .build();

        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>());

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).isNotNull();
        Path resultFile = Path.of(resultPath);
        assertThat(Files.exists(resultFile)).isTrue();

        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines).hasSize(1); // only header
        assertThat(lines.get(0)).contains("userId");
        assertThat(lines.get(0)).contains("startDate");
        assertThat(lines.get(0)).contains("status");
    }

    @Test
    @DisplayName("Should create file in configured directory")
    void shouldCreateFileInConfiguredDirectory() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .build();

        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>());

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).contains(tempDir.toString());
        assertThat(Files.exists(Path.of(resultPath))).isTrue();
    }

    @Test
    @DisplayName("Should include job ID in result file name")
    void shouldIncludeJobIdInFileName() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .build();

        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>());

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).contains(jobId.toString());
        assertThat(resultPath).contains("bulk-upload-result-");
    }

    @Test
    @DisplayName("Should handle missing leave for successful record")
    void shouldHandleMissingLeaveForSuccessfulRecord() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .build();

        // Create metadata with missing leave data (only has userid)
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userid", "user1");

        BulkUploadRecord record1 = BulkUploadRecord.builder()
                .id(1L)
                .job(job)
                .rowNumber(2)
                .metadata(metadata)
                .status(BulkUploadRecord.BulkRecordStatus.SUCCESS)
                .build();

        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>(List.of(record1)));

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).isNotNull();
        Path resultFile = Path.of(resultPath);
        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines.get(1)).contains("user1");
        // Missing fields should show as empty strings (our mock returns empty for missing metadata)
        assertThat(lines.get(1)).contains("SUCCESS");
    }

    @Test
    @DisplayName("Should handle error record without error message")
    void shouldHandleErrorRecordWithoutErrorMessage() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .build();

        BulkUploadRecord record1 = BulkUploadRecord.builder()
                .id(1L)
                .job(job)
                .rowNumber(2)
                .metadata(createTestMetadata("user1"))
                .status(BulkUploadRecord.BulkRecordStatus.ERROR)
                .errorMessage(null)
                .build();

        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>(List.of(record1)));

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).isNotNull();
        Path resultFile = Path.of(resultPath);
        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines.get(1)).contains("user1");
        assertThat(lines.get(1)).contains("ERROR");
    }

    @Test
    @DisplayName("Should handle half-day leave duration type")
    void shouldHandleHalfDayLeaveDurationType() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .build();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("userid", "user1");
        metadata.put("startdate", "2024-01-01");
        metadata.put("enddate", "2024-01-05");
        metadata.put("type", "ANNUAL_LEAVE");
        metadata.put("durationtype", "FIRST_HALF");

        BulkUploadRecord record1 = BulkUploadRecord.builder()
                .id(1L)
                .job(job)
                .rowNumber(2)
                .metadata(metadata)
                .status(BulkUploadRecord.BulkRecordStatus.SUCCESS)
                .build();


        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>(List.of(record1)));

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).isNotNull();
        Path resultFile = Path.of(resultPath);
        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines.get(1)).contains("FIRST_HALF");
    }

    @Test
    @DisplayName("Should handle optional holiday leave type")
    void shouldHandleOptionalHolidayLeaveType() throws IOException {
        // Given
        UUID jobId = UUID.randomUUID();

        BulkUploadJob job = BulkUploadJob.builder()
                .id(jobId)
                .type(BulkUploadType.LEAVE)
                .fileName("test.csv")
                .build();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("userid", "user1");
        metadata.put("startdate", "2024-01-01");
        metadata.put("enddate", "2024-01-05");
        metadata.put("type", "OPTIONAL_HOLIDAY");
        metadata.put("durationtype", "FULL_DAY");

        BulkUploadRecord record1 = BulkUploadRecord.builder()
                .id(1L)
                .job(job)
                .rowNumber(2)
                .metadata(metadata)
                .status(BulkUploadRecord.BulkRecordStatus.SUCCESS)
                .build();


        when(bulkUploadRecordRepository.findByJobId(jobId)).thenReturn(new ArrayList<>(List.of(record1)));

        // When
        String resultPath = csvResultService.generateResultFile(job);

        // Then
        assertThat(resultPath).isNotNull();
        Path resultFile = Path.of(resultPath);
        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines.get(1)).contains("OPTIONAL_HOLIDAY");
    }

    private Map<String, String> createTestMetadata(String userId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userid", userId);
        metadata.put("startdate", "2024-01-01");
        metadata.put("enddate", "2024-01-05");
        metadata.put("type", "ANNUAL_LEAVE");
        metadata.put("durationtype", "FULL_DAY");
        return metadata;
    }
}
