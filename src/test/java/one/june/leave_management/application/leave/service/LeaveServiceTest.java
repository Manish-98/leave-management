package one.june.leave_management.application.leave.service;

import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.domain.leave.model.*;
import one.june.leave_management.domain.leave.service.LeaveDomainService;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.LeaveSourceRefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LeaveService}
 * Tests the service layer methods with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveService Unit Tests")
class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private LeaveSourceRefRepository leaveSourceRefRepository;

    @Mock
    private OutboundSyncService outboundSyncService;

    @Mock
    private LeaveDomainService leaveDomainService;

    @Mock
    private BulkUploadRecordRepository bulkUploadRecordRepository;

    @Mock
    private BulkUploadJobRepository bulkUploadJobRepository;

    @Mock
    private CsvResultService csvResultService;

    @Mock
    private LeaveMapper leaveMapper;

    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        // Initialize LeaveService with all mocked dependencies
        leaveService = new LeaveService(
                leaveRepository,
                leaveSourceRefRepository,
                outboundSyncService,
                leaveDomainService,
                leaveMapper,
                bulkUploadRecordRepository,
                bulkUploadJobRepository,
                csvResultService
        );
    }

    @Nested
    @DisplayName("BulkIngestAsync Tests")
    class BulkIngestAsyncTests {

        @Test
        @DisplayName("Should process all commands successfully")
        void shouldProcessAllCommandsSuccessfully() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            List<LeaveIngestionCommand> commands = List.of(
                    createValidCommand("user1"),
                    createValidCommand("user2"),
                    createValidCommand("user3")
            );

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(any(), any()))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                if (leave.getId() == null) {
                    leave.setId(UUID.randomUUID());
                }
                return leave;
            });
            when(leaveMapper.toDto(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                LeaveDto dto = new LeaveDto();
                dto.setId(leave.getId());
                return dto;
            });
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, commands);

            // Then
            verify(bulkUploadJobRepository).findById(jobId);
            verify(bulkUploadRecordRepository, times(3)).save(any(BulkUploadRecord.class));
            verify(bulkUploadJobRepository, times(2)).save(job);
            assertThat(job.getStatus()).isEqualTo(BulkUploadJob.BulkUploadStatus.COMPLETED);
            assertThat(job.getSuccessfulRecords()).isEqualTo(3);
            assertThat(job.getFailedRecords()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should create success records for valid commands")
        void shouldCreateSuccessRecordsForValidCommands() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            LeaveIngestionCommand command = createValidCommand("user1");

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(any(), any()))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                if (leave.getId() == null) {
                    leave.setId(UUID.randomUUID());
                }
                return leave;
            });
            when(leaveMapper.toDto(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                LeaveDto dto = new LeaveDto();
                dto.setId(leave.getId());
                return dto;
            });
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, List.of(command));

            // Then
            ArgumentCaptor<BulkUploadRecord> recordCaptor = ArgumentCaptor.forClass(BulkUploadRecord.class);
            verify(bulkUploadRecordRepository).save(recordCaptor.capture());

            BulkUploadRecord savedRecord = recordCaptor.getValue();
            assertThat(savedRecord).isNotNull();
            assertThat(savedRecord.getStatus()).isEqualTo(BulkUploadRecord.BulkRecordStatus.SUCCESS);
            assertThat(savedRecord.getUserId()).isEqualTo("user1");
            assertThat(savedRecord.getLeaveId()).isNotNull();
            assertThat(savedRecord.getRowNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should create failure records for invalid commands")
        void shouldCreateFailureRecordsForInvalidCommands() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            LeaveIngestionCommand command = createValidCommand("user1");

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);
            doThrow(new RuntimeException("Invalid date range"))
                    .when(leaveDomainService).validateNoOverlappingLeaves(any());
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, List.of(command));

            // Then
            ArgumentCaptor<BulkUploadRecord> recordCaptor = ArgumentCaptor.forClass(BulkUploadRecord.class);
            verify(bulkUploadRecordRepository).save(recordCaptor.capture());

            BulkUploadRecord savedRecord = recordCaptor.getValue();
            assertThat(savedRecord).isNotNull();
            assertThat(savedRecord.getStatus()).isEqualTo(BulkUploadRecord.BulkRecordStatus.ERROR);
            assertThat(savedRecord.getUserId()).isEqualTo("user1");
            assertThat(savedRecord.getErrorMessage()).isEqualTo("Invalid date range");
        }

        @Test
        @DisplayName("Should increment success counter")
        void shouldIncrementSuccessCounter() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            List<LeaveIngestionCommand> commands = List.of(
                    createValidCommand("user1"),
                    createValidCommand("user2")
            );

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(any(), any()))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                if (leave.getId() == null) {
                    leave.setId(UUID.randomUUID());
                }
                return leave;
            });
            when(leaveMapper.toDto(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                LeaveDto dto = new LeaveDto();
                dto.setId(leave.getId());
                return dto;
            });
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, commands);

            // Then
            assertThat(job.getSuccessfulRecords()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should increment failure counter")
        void shouldIncrementFailureCounter() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            List<LeaveIngestionCommand> commands = List.of(
                    createValidCommand("user1"),
                    createValidCommand("user2")
            );

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);
            doThrow(new RuntimeException("Error"))
                    .when(leaveDomainService).validateNoOverlappingLeaves(any());
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, commands);

            // Then
            assertThat(job.getFailedRecords()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should mark job as completed")
        void shouldMarkJobAsCompleted() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            List<LeaveIngestionCommand> commands = List.of(createValidCommand("user1"));

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(any(), any()))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                if (leave.getId() == null) {
                    leave.setId(UUID.randomUUID());
                }
                return leave;
            });
            when(leaveMapper.toDto(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                LeaveDto dto = new LeaveDto();
                dto.setId(leave.getId());
                return dto;
            });
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, commands);

            // Then
            assertThat(job.getStatus()).isEqualTo(BulkUploadJob.BulkUploadStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should re-fetch job in new transaction context")
        void shouldRefetchJobInNewTransactionContext() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            List<LeaveIngestionCommand> commands = List.of(createValidCommand("user1"));

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(any(), any()))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                if (leave.getId() == null) {
                    leave.setId(UUID.randomUUID());
                }
                return leave;
            });
            when(leaveMapper.toDto(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                LeaveDto dto = new LeaveDto();
                dto.setId(leave.getId());
                return dto;
            });
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, commands);

            // Then
            verify(bulkUploadJobRepository).findById(jobId);
        }

        @Test
        @DisplayName("Should handle individual command failures gracefully")
        void shouldHandleIndividualCommandFailuresGracefully() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            List<LeaveIngestionCommand> commands = List.of(
                    createValidCommand("user1"),
                    createValidCommand("user2"),
                    createValidCommand("user3")
            );

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(any(), any()))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                if (leave.getId() == null) {
                    leave.setId(UUID.randomUUID());
                }
                return leave;
            });
            when(leaveMapper.toDto(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                LeaveDto dto = new LeaveDto();
                dto.setId(leave.getId());
                return dto;
            });
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);

            // Make the second command fail
            doNothing()
                    .doThrow(new RuntimeException("Validation failed"))
                    .doNothing()
                    .when(leaveDomainService).validateNoOverlappingLeaves(any());
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, commands);

            // Then
            verify(bulkUploadRecordRepository, times(3)).save(any(BulkUploadRecord.class));
            assertThat(job.getSuccessfulRecords()).isEqualTo(2);
            assertThat(job.getFailedRecords()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should save updated job to repository")
        void shouldSaveUpdatedJobToRepository() throws IOException {
            // Given
            UUID jobId = UUID.randomUUID();
            BulkUploadJob job = createTestJob(jobId);
            List<LeaveIngestionCommand> commands = List.of(createValidCommand("user1"));

            when(bulkUploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(any(), any()))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                if (leave.getId() == null) {
                    leave.setId(UUID.randomUUID());
                }
                return leave;
            });
            when(leaveMapper.toDto(any(Leave.class))).thenAnswer(invocation -> {
                Leave leave = invocation.getArgument(0);
                LeaveDto dto = new LeaveDto();
                dto.setId(leave.getId());
                return dto;
            });
            when(bulkUploadRecordRepository.save(any(BulkUploadRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(bulkUploadJobRepository.save(job)).thenReturn(job);
            doReturn("/result/result.csv").when(csvResultService).generateResultFile(any());

            // When
            leaveService.bulkIngestAsync(job, commands);

            // Then
            verify(bulkUploadJobRepository, times(2)).save(job);
        }
    }

    @Nested
    @DisplayName("FindOrCreateLeaveFromSourceRef Tests")
    class FindOrCreateLeaveFromSourceRefTests {

        @Test
        @DisplayName("Should return existing leave when found by ID")
        void shouldReturnExistingLeaveWhenFoundById() {
            // Given
            UUID leaveId = UUID.randomUUID();
            LeaveSourceRef sourceRef = createSourceRef(leaveId);
            Leave expectedLeave = createLeave(leaveId);

            when(leaveRepository.findById(leaveId)).thenReturn(Optional.of(expectedLeave));

            // When
            Leave result = leaveService.findOrCreateLeaveFromSourceRef(sourceRef);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(leaveId);
            verify(leaveRepository).findById(leaveId);
        }

        @Test
        @DisplayName("Should throw exception when sourceRef has null leaveId")
        void shouldThrowWhenSourceRefHasNullLeaveId() {
            // Given
            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-id-123")
                    .leaveId(null)
                    .build();

            // When & Then
            assertThatThrownBy(() -> leaveService.findOrCreateLeaveFromSourceRef(sourceRef))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Source reference exists but has no associated leave ID");

            verify(leaveRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw exception when leaveId not found in database")
        void shouldThrowWhenLeaveIdNotFound() {
            // Given
            UUID leaveId = UUID.randomUUID();
            LeaveSourceRef sourceRef = createSourceRef(leaveId);

            when(leaveRepository.findById(leaveId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> leaveService.findOrCreateLeaveFromSourceRef(sourceRef))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Source reference points to non-existent leave: " + leaveId);

            verify(leaveRepository).findById(leaveId);
        }

        @Test
        @DisplayName("Should throw IllegalStateException with appropriate message")
        void shouldThrowIllegalStateExceptionWithAppropriateMessage() {
            // Given
            UUID leaveId = UUID.randomUUID();
            LeaveSourceRef sourceRef = createSourceRef(leaveId);

            when(leaveRepository.findById(leaveId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> leaveService.findOrCreateLeaveFromSourceRef(sourceRef))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non-existent leave");
        }
    }

    @Nested
    @DisplayName("PerformOutboundSync Tests")
    class PerformOutboundSyncTests {

        @Test
        @DisplayName("Should skip sync for CSV_BULK source type")
        void shouldSkipSyncForCsvBulkSourceType() {
            // Given
            Leave leave = createLeave(UUID.randomUUID());

            // When
            leaveService.performOutboundSync(leave, SourceType.CSV_BULK);

            // Then
            verify(outboundSyncService, never()).sync(any(), any());
        }

        @Test
        @DisplayName("Should perform sync and log success")
        void shouldPerformSyncAndLogSuccess() {
            // Given
            Leave leave = createLeave(UUID.randomUUID());
            SourceType sourceType = SourceType.SLACK;

            // When
            leaveService.performOutboundSync(leave, sourceType);

            // Then
            verify(outboundSyncService).sync(leave, sourceType);
        }

        @Test
        @DisplayName("Should handle sync exception gracefully")
        void shouldHandleSyncExceptionGracefully() {
            // Given
            Leave leave = createLeave(UUID.randomUUID());
            SourceType sourceType = SourceType.SLACK;

            doThrow(new RuntimeException("Sync failed"))
                    .when(outboundSyncService).sync(leave, sourceType);

            // When & Then - should not throw exception
            leaveService.performOutboundSync(leave, sourceType);

            // Verify sync was attempted
            verify(outboundSyncService).sync(leave, sourceType);
        }
    }

    // Helper methods

    private BulkUploadJob createTestJob(UUID jobId) {
        return BulkUploadJob.builder()
                .id(jobId)
                .status(BulkUploadJob.BulkUploadStatus.PROCESSING)
                .successfulRecords(0)
                .failedRecords(0)
                .build();
    }

    private LeaveIngestionCommand createValidCommand(String userId) {
        return LeaveIngestionCommand.builder()
                .userId(userId)
                .sourceType(SourceType.CSV_BULK)
                .sourceId("csv-row-" + UUID.randomUUID())
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .durationType(LeaveDurationType.FULL_DAY)
                .status(LeaveStatus.REQUESTED)
                .build();
    }

    private LeaveSourceRef createSourceRef(UUID leaveId) {
        return LeaveSourceRef.builder()
                .sourceType(SourceType.SLACK)
                .sourceId("slack-source-id")
                .leaveId(leaveId)
                .build();
    }

    private Leave createLeave(UUID leaveId) {
        return Leave.builder()
                .id(leaveId)
                .userId("test-user")
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .durationType(LeaveDurationType.FULL_DAY)
                .status(LeaveStatus.REQUESTED)
                .build();
    }
}
