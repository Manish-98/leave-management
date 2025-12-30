package one.june.leave_management.application.employee.service;

import one.june.leave_management.adapter.inbound.web.csv.CsvEmployeeParserStrategy;
import one.june.leave_management.adapter.inbound.web.csv.ParsedResult;
import one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import one.june.leave_management.application.employee.command.EmployeeCreateCommand;
import one.june.leave_management.application.bulk.service.CsvResultService;
import one.june.leave_management.application.bulk.strategy.EmployeeBulkUploadStrategy;
import one.june.leave_management.common.event.EntityEvent;
import one.june.leave_management.common.exception.BulkUploadJobNotFoundException;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import org.springframework.context.ApplicationEventPublisher;
import one.june.leave_management.test.util.CsvTestUtil;
import org.apache.commons.lang3.IntegerRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeBulkUploadServiceTest {

    @Mock
    private CsvEmployeeParserStrategy csvEmployeeParserStrategy;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private BulkUploadJobRepository bulkUploadJobRepository;

    @Mock
    private BulkUploadRecordRepository bulkUploadRecordRepository;

    @Mock
    private CsvResultService csvResultService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EmployeeBulkUploadStrategy employeeBulkUploadStrategy;

    private EmployeeBulkUploadService employeeBulkUploadService;

    @BeforeEach
    void setUp() {
        employeeBulkUploadService = new EmployeeBulkUploadService(
                csvEmployeeParserStrategy,
                employeeService,
                bulkUploadJobRepository,
                bulkUploadRecordRepository,
                csvResultService,
                eventPublisher,
                employeeBulkUploadStrategy
        );
    }

    @Test
    @DisplayName("Should successfully initiate bulk upload with valid CSV file")
    void shouldSuccessfullyInitiateBulkUpload() throws IOException {
        // Given
        List<CsvTestUtil.CsvEmployeeRecord> records = Stream.of(0, 1, 2, 3, 4, 5, 6, 7).map(it ->
                CsvTestUtil.CsvEmployeeRecord.builder()
                        .name("John Doe " + it)
                        .slackId("U12345" + it)
                        .dateOfJoining("2020-01-15")
                        .build()
        ).toList();

        MultipartFile file = CsvTestUtil.createValidEmployeeCsvFile("test", records);

        List<EmployeeCreateCommand> commands = Stream.of(0, 1, 2, 3, 4, 5, 6, 7).map(it ->
                EmployeeCreateCommand.builder()
                        .name("John Doe " + it)
                        .slackId("U12345" + it)
                        .dateOfJoining(LocalDate.of(2020, 1, 15))
                        .active(true)
                        .build()
        ).toList();

        // Wrap commands in ParsedResult
        List<ParsedResult<EmployeeCreateCommand>> parsedResults = commands.stream()
                .map(cmd -> ParsedResult.success(cmd, Map.of("test", "metadata"), commands.indexOf(cmd) + 1))
                .collect(Collectors.toList());
when(csvEmployeeParserStrategy.parse(eq(file), any(String.class))).thenReturn(parsedResults);
        when(bulkUploadJobRepository.save(any(BulkUploadJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BulkUploadResponse response = employeeBulkUploadService.initiateBulkUpload(file);

        // Then - Verify response
        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getTotalRecords()).isEqualTo(8);
        assertThat(response.getSuccessfulRecords()).isEqualTo(0);
        assertThat(response.getFailedRecords()).isEqualTo(0);
        assertThat(response.getResultAvailable()).isFalse();

        // Verify parser called
        verify(csvEmployeeParserStrategy).parse(eq(file), any(String.class));

        // Verify job saved - check the first save which should have PROCESSING status
        ArgumentCaptor<BulkUploadJob> jobCaptor = ArgumentCaptor.forClass(BulkUploadJob.class);
        verify(bulkUploadJobRepository, atLeastOnce()).save(jobCaptor.capture());

        // Get the first saved job (which should have PROCESSING status)
        BulkUploadJob firstSavedJob = jobCaptor.getAllValues().get(0);
        assertThat(firstSavedJob.getStatus()).isEqualTo(BulkUploadJob.BulkUploadStatus.PROCESSING);
        assertThat(firstSavedJob.getTotalRecords()).isEqualTo(8);
        assertThat(firstSavedJob.getFileName()).isEqualTo("test.csv");
    }

    @Test
    @DisplayName("Should throw exception when file is empty")
    void shouldThrowWhenFileIsEmpty() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createEmptyCsvFile("empty.csv");

        // When & Then
        assertThatThrownBy(() -> employeeBulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");

        verify(csvEmployeeParserStrategy, never()).parse(any(), any());
        verify(bulkUploadJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when file is not CSV")
    void shouldThrowWhenFileIsNotCsv() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createNonCsvFile("test.txt");

        // When & Then
        assertThatThrownBy(() -> employeeBulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only CSV files are allowed");

        verify(csvEmployeeParserStrategy, never()).parse(any(), any());
        verify(bulkUploadJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when file exceeds size limit")
    void shouldThrowWhenFileExceedsSizeLimit() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createOversizedCsvFile("large.csv");

        // When & Then
        assertThatThrownBy(() -> employeeBulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum limit of 10MB");

        verify(csvEmployeeParserStrategy, never()).parse(any(), any());
        verify(bulkUploadJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when filename is null")
    void shouldThrowWhenFilenameIsNull() throws IOException {
        // Given
        MultipartFile file = new MockMultipartFile("file", new byte[0]);

        // When & Then
        assertThatThrownBy(() -> employeeBulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");

        verify(csvEmployeeParserStrategy, never()).parse(any(), any());
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
        BulkUploadResponse response = employeeBulkUploadService.getJobStatus(jobId);

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
        assertThatThrownBy(() -> employeeBulkUploadService.getJobStatus(jobId))
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
        String actualPath = employeeBulkUploadService.getResultFilePath(jobId);

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
        assertThatThrownBy(() -> employeeBulkUploadService.getResultFilePath(jobId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Result file not available");

        verify(bulkUploadJobRepository).findById(jobId);
    }

    @Test
    @DisplayName("Should trigger async processing when job created")
    void shouldTriggerAsyncProcessing() throws IOException {
        // Given
        List<CsvTestUtil.CsvEmployeeRecord> records = List.of(
                CsvTestUtil.CsvEmployeeRecord.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining("2020-01-15")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidEmployeeCsvFile("test", records);

        List<EmployeeCreateCommand> commands = List.of(
                EmployeeCreateCommand.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining(LocalDate.of(2020, 1, 15))
                        .build()
        );

        // Wrap commands in ParsedResult
        List<ParsedResult<EmployeeCreateCommand>> parsedResults = commands.stream()
                .map(cmd -> ParsedResult.success(cmd, Map.of("test", "metadata"), commands.indexOf(cmd) + 1))
                .collect(Collectors.toList());
when(csvEmployeeParserStrategy.parse(eq(file), any(String.class))).thenReturn(parsedResults);
        when(bulkUploadJobRepository.save(any(BulkUploadJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        employeeBulkUploadService.initiateBulkUpload(file);

        // Then - Verify job was saved with PROCESSING status
        ArgumentCaptor<BulkUploadJob> jobCaptor = ArgumentCaptor.forClass(BulkUploadJob.class);
        verify(bulkUploadJobRepository, atLeastOnce()).save(jobCaptor.capture());
        // Get the first saved job (which should have PROCESSING status)
        assertThat(jobCaptor.getAllValues().get(0).getStatus()).isEqualTo(BulkUploadJob.BulkUploadStatus.PROCESSING);
    }

    @Test
    @DisplayName("Should handle exception during CSV parsing")
    void shouldHandleExceptionDuringParsing() throws IOException {
        // Given
        List<CsvTestUtil.CsvEmployeeRecord> records = List.of(
                CsvTestUtil.CsvEmployeeRecord.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining("2020-01-15")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidEmployeeCsvFile("test.csv", records);

        when(csvEmployeeParserStrategy.parse(eq(file), any(String.class)))
                .thenThrow(new RuntimeException("Parse error"));

        // When & Then
        assertThatThrownBy(() -> employeeBulkUploadService.initiateBulkUpload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse CSV file");

        verify(bulkUploadJobRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should process employee with carry forward leaves")
    void shouldProcessEmployeeWithCarryForwardLeaves() throws IOException {
        // Given
        int currentYear = LocalDate.now().getYear();
        List<CsvTestUtil.CsvEmployeeRecord> records = List.of(
                CsvTestUtil.CsvEmployeeRecord.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining("2020-01-15")
                        .carryForwardLeaves("5")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidEmployeeCsvFile("test", records);

        List<EmployeeCreateCommand> commands = List.of(
                EmployeeCreateCommand.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining(LocalDate.of(2020, 1, 15))
                        .active(true)
                        .carryForwardLeaves(Map.of(currentYear, 5))
                        .build()
        );

        // Wrap commands in ParsedResult
        List<ParsedResult<EmployeeCreateCommand>> parsedResults = commands.stream()
                .map(cmd -> ParsedResult.success(cmd, Map.of("test", "metadata"), commands.indexOf(cmd) + 1))
                .collect(Collectors.toList());
when(csvEmployeeParserStrategy.parse(eq(file), any(String.class))).thenReturn(parsedResults);
        when(bulkUploadJobRepository.save(any(BulkUploadJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BulkUploadResponse response = employeeBulkUploadService.initiateBulkUpload(file);

        // Then
        assertThat(response.getTotalRecords()).isEqualTo(1);
        verify(csvEmployeeParserStrategy).parse(eq(file), any(String.class));
    }

    @Test
    @DisplayName("Should process employee with both slackId and googleId")
    void shouldProcessEmployeeWithBothExternalIds() throws IOException {
        // Given
        List<CsvTestUtil.CsvEmployeeRecord> records = List.of(
                CsvTestUtil.CsvEmployeeRecord.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .googleId("john@example.com")
                        .dateOfJoining("2020-01-15")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidEmployeeCsvFile("test", records);

        List<EmployeeCreateCommand> commands = List.of(
                EmployeeCreateCommand.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .googleId("john@example.com")
                        .dateOfJoining(LocalDate.of(2020, 1, 15))
                        .build()
        );

        // Wrap commands in ParsedResult
        List<ParsedResult<EmployeeCreateCommand>> parsedResults = commands.stream()
                .map(cmd -> ParsedResult.success(cmd, Map.of("test", "metadata"), commands.indexOf(cmd) + 1))
                .collect(Collectors.toList());
when(csvEmployeeParserStrategy.parse(eq(file), any(String.class))).thenReturn(parsedResults);
        when(bulkUploadJobRepository.save(any(BulkUploadJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BulkUploadResponse response = employeeBulkUploadService.initiateBulkUpload(file);

        // Then
        assertThat(response.getTotalRecords()).isEqualTo(1);
        verify(csvEmployeeParserStrategy).parse(eq(file), any(String.class));
    }

    @Test
    @DisplayName("Should process employee with active status false")
    void shouldProcessEmployeeWithActiveFalse() throws IOException {
        // Given
        List<CsvTestUtil.CsvEmployeeRecord> records = List.of(
                CsvTestUtil.CsvEmployeeRecord.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining("2020-01-15")
                        .active("false")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidEmployeeCsvFile("test", records);

        List<EmployeeCreateCommand> commands = List.of(
                EmployeeCreateCommand.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining(LocalDate.of(2020, 1, 15))
                        .active(false)
                        .build()
        );

        // Wrap commands in ParsedResult
        List<ParsedResult<EmployeeCreateCommand>> parsedResults = commands.stream()
                .map(cmd -> ParsedResult.success(cmd, Map.of("test", "metadata"), commands.indexOf(cmd) + 1))
                .collect(Collectors.toList());
when(csvEmployeeParserStrategy.parse(eq(file), any(String.class))).thenReturn(parsedResults);
        when(bulkUploadJobRepository.save(any(BulkUploadJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BulkUploadResponse response = employeeBulkUploadService.initiateBulkUpload(file);

        // Then
        assertThat(response.getTotalRecords()).isEqualTo(1);
        verify(csvEmployeeParserStrategy).parse(eq(file), any(String.class));
    }

    @Test
    @DisplayName("Should save job with correct filename")
    void shouldSaveJobWithCorrectFilename() throws IOException {
        // Given
        String filename = "employees-2024";
        List<CsvTestUtil.CsvEmployeeRecord> records = List.of(
                CsvTestUtil.CsvEmployeeRecord.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining("2020-01-15")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidEmployeeCsvFile(filename, records);

        List<EmployeeCreateCommand> commands = List.of(
                EmployeeCreateCommand.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining(LocalDate.of(2020, 1, 15))
                        .build()
        );

        // Wrap commands in ParsedResult
        List<ParsedResult<EmployeeCreateCommand>> parsedResults = commands.stream()
                .map(cmd -> ParsedResult.success(cmd, Map.of("test", "metadata"), commands.indexOf(cmd) + 1))
                .collect(Collectors.toList());
when(csvEmployeeParserStrategy.parse(eq(file), any(String.class))).thenReturn(parsedResults);
        when(bulkUploadJobRepository.save(any(BulkUploadJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        employeeBulkUploadService.initiateBulkUpload(file);

        // Then
        ArgumentCaptor<BulkUploadJob> jobCaptor = ArgumentCaptor.forClass(BulkUploadJob.class);
        verify(bulkUploadJobRepository, atLeastOnce()).save(jobCaptor.capture());

        BulkUploadJob savedJob = jobCaptor.getValue();
        // CsvTestUtil adds .csv extension, so originalFilename becomes filename + .csv
        assertThat(savedJob.getFileName()).isEqualTo(filename + ".csv");
    }

    @Test
    @DisplayName("Should initialize job with zero counters")
    void shouldInitializeJobWithZeroCounters() throws IOException {
        // Given
        List<CsvTestUtil.CsvEmployeeRecord> records = List.of(
                CsvTestUtil.CsvEmployeeRecord.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining("2020-01-15")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidEmployeeCsvFile("test", records);

        List<EmployeeCreateCommand> commands = List.of(
                EmployeeCreateCommand.builder()
                        .name("John Doe")
                        .slackId("U12345")
                        .dateOfJoining(LocalDate.of(2020, 1, 15))
                        .build()
        );

        // Wrap commands in ParsedResult
        List<ParsedResult<EmployeeCreateCommand>> parsedResults = commands.stream()
                .map(cmd -> ParsedResult.success(cmd, Map.of("test", "metadata"), commands.indexOf(cmd) + 1))
                .collect(Collectors.toList());
when(csvEmployeeParserStrategy.parse(eq(file), any(String.class))).thenReturn(parsedResults);
        when(bulkUploadJobRepository.save(any(BulkUploadJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BulkUploadResponse response = employeeBulkUploadService.initiateBulkUpload(file);

        // Then
        assertThat(response.getSuccessfulRecords()).isEqualTo(0);
        assertThat(response.getFailedRecords()).isEqualTo(0);
    }
}
