package one.june.leave_management.application.leave.service;

import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.common.exception.OverlappingLeaveException;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.port.LeaveSourceRefRepository;
import one.june.leave_management.domain.leave.service.LeaveDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveIngestionServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private LeaveSourceRefRepository leaveSourceRefRepository;

    @Mock
    private OutboundSyncService outboundSyncService;

    @Mock
    private LeaveDomainService leaveDomainService;

    @Mock
    private LeaveMapper leaveMapper;

    private LeaveIngestionService leaveIngestionService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_SOURCE_ID = "source-123";

    @BeforeEach
    void setUp() {
        leaveIngestionService = new LeaveIngestionService(
                leaveRepository,
                leaveSourceRefRepository,
                outboundSyncService,
                leaveDomainService,
                leaveMapper
        );
    }

    @Test
    void ingestShouldCreateNewLeaveWhenSourceRefDoesNotExist() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                .sourceType(SourceType.WEB)
                .sourceId(TEST_SOURCE_ID)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                .sourceType(SourceType.WEB)
                .sourceId(TEST_SOURCE_ID)
                .build();

        Leave newLeave = Leave.builder()
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        Leave savedLeave = Leave.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        LeaveDto expectedDto = LeaveDto.builder()
                .id(savedLeave.getId())
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID))
                .thenReturn(Optional.empty());
        when(leaveRepository.save(any(Leave.class))).thenReturn(savedLeave);
        when(leaveMapper.toDto(savedLeave)).thenReturn(expectedDto);

        LeaveDto result = leaveIngestionService.ingest(command);

        assertNotNull(result);
        assertEquals(savedLeave.getId(), result.getId());
        assertEquals(TEST_USER_ID, result.getUserId());

        verify(leaveSourceRefRepository).findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID);
        verify(leaveDomainService).validateLeaveForPersistence(any(Leave.class));
        verify(leaveDomainService).validateNoOverlappingLeaves(any(Leave.class));
        verify(leaveRepository).save(any(Leave.class));
        verify(outboundSyncService).sync(savedLeave, SourceType.WEB);
        verify(leaveMapper).toDto(savedLeave);
    }

    @Test
    void ingestShouldUpdateExistingLeaveWhenSourceRefExists() {
        DateRange newDateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .build();

        LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                .sourceType(SourceType.WEB)
                .sourceId(TEST_SOURCE_ID)
                .userId(TEST_USER_ID)
                .dateRange(newDateRange)
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.APPROVED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        UUID existingLeaveId = UUID.randomUUID();

        LeaveSourceRef existingSourceRef = LeaveSourceRef.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.WEB)
                .sourceId(TEST_SOURCE_ID)
                .leaveId(existingLeaveId)
                .build();

        Leave existingLeave = Leave.builder()
                .id(existingLeaveId)
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(3))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        Leave updatedLeave = Leave.builder()
                .id(existingLeaveId)
                .userId(TEST_USER_ID)
                .dateRange(newDateRange)
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.APPROVED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        Leave savedLeave = Leave.builder()
                .id(existingLeaveId)
                .userId(TEST_USER_ID)
                .dateRange(newDateRange)
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.APPROVED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        LeaveDto expectedDto = LeaveDto.builder()
                .id(existingLeaveId)
                .userId(TEST_USER_ID)
                .dateRange(newDateRange)
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.APPROVED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID))
                .thenReturn(Optional.of(existingSourceRef));
        when(leaveRepository.findById(existingLeaveId)).thenReturn(Optional.of(existingLeave));
        when(leaveRepository.save(any(Leave.class))).thenReturn(savedLeave);
        when(leaveMapper.toDto(savedLeave)).thenReturn(expectedDto);

        LeaveDto result = leaveIngestionService.ingest(command);

        assertNotNull(result);
        assertEquals(existingLeaveId, result.getId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(LeaveType.OPTIONAL_HOLIDAY, result.getType());
        assertEquals(LeaveStatus.APPROVED, result.getStatus());

        verify(leaveSourceRefRepository).findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID);
        verify(leaveRepository).findById(existingLeaveId);
        verify(leaveDomainService).validateLeaveForPersistence(any(Leave.class));
        verify(leaveDomainService).validateNoOverlappingLeaves(any(Leave.class));
        verify(leaveRepository).save(any(Leave.class));
        verify(outboundSyncService).sync(savedLeave, SourceType.WEB);
        verify(leaveMapper).toDto(savedLeave);
    }

    @Test
    void ingestShouldThrowExceptionWhenSourceRefExistsButLeaveNotFound() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                .sourceType(SourceType.WEB)
                .sourceId(TEST_SOURCE_ID)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        UUID existingLeaveId = UUID.randomUUID();

        LeaveSourceRef existingSourceRef = LeaveSourceRef.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.WEB)
                .sourceId(TEST_SOURCE_ID)
                .leaveId(existingLeaveId)
                .build();

        when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID))
                .thenReturn(Optional.of(existingSourceRef));
        when(leaveRepository.findById(existingLeaveId)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> leaveIngestionService.ingest(command)
        );

        assertTrue(exception.getMessage().contains("Source reference points to non-existent leave"));

        verify(leaveSourceRefRepository).findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID);
        verify(leaveRepository).findById(existingLeaveId);
        verify(leaveRepository, never()).save(any(Leave.class));
        verify(outboundSyncService, never()).sync(any(), any());
    }

    @Test
    void ingestShouldThrowExceptionWhenOverlappingLeaveExists() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                .sourceType(SourceType.WEB)
                .sourceId(TEST_SOURCE_ID)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID))
                .thenReturn(Optional.empty());
        doThrow(new OverlappingLeaveException(
                TEST_USER_ID,
                dateRange.getStartDate(),
                dateRange.getEndDate(),
                UUID.randomUUID()
        )).when(leaveDomainService).validateNoOverlappingLeaves(any(Leave.class));

        assertThrows(
                OverlappingLeaveException.class,
                () -> leaveIngestionService.ingest(command)
        );

        verify(leaveSourceRefRepository).findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID);
        verify(leaveDomainService).validateNoOverlappingLeaves(any(Leave.class));
        verify(leaveRepository, never()).save(any(Leave.class));
        verify(outboundSyncService, never()).sync(any(), any());
    }

    @Test
    void ingestShouldContinueWhenOutboundSyncFails() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                .sourceType(SourceType.WEB)
                .sourceId(TEST_SOURCE_ID)
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        Leave savedLeave = Leave.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        LeaveDto expectedDto = LeaveDto.builder()
                .id(savedLeave.getId())
                .userId(TEST_USER_ID)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY)
                .build();

        when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID))
                .thenReturn(Optional.empty());
        when(leaveRepository.save(any(Leave.class))).thenReturn(savedLeave);
        when(leaveMapper.toDto(savedLeave)).thenReturn(expectedDto);
        doThrow(new RuntimeException("Sync service unavailable"))
                .when(outboundSyncService).sync(savedLeave, SourceType.WEB);

        LeaveDto result = leaveIngestionService.ingest(command);

        assertNotNull(result);
        assertEquals(savedLeave.getId(), result.getId());

        verify(leaveSourceRefRepository).findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID);
        verify(leaveDomainService).validateLeaveForPersistence(any(Leave.class));
        verify(leaveDomainService).validateNoOverlappingLeaves(any(Leave.class));
        verify(leaveRepository).save(any(Leave.class));
        verify(outboundSyncService).sync(savedLeave, SourceType.WEB);
        verify(leaveMapper).toDto(savedLeave);
    }

    @Test
    void ingestShouldHandleAllDurationTypes() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        for (LeaveDurationType durationType : LeaveDurationType.values()) {
            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId(TEST_SOURCE_ID + "-" + durationType)
                    .userId(TEST_USER_ID)
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(durationType)
                    .build();

            Leave savedLeave = Leave.builder()
                    .id(UUID.randomUUID())
                    .userId(TEST_USER_ID)
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(durationType)
                    .build();

            LeaveDto expectedDto = LeaveDto.builder()
                    .id(savedLeave.getId())
                    .userId(TEST_USER_ID)
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(durationType)
                    .build();

            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(SourceType.WEB, TEST_SOURCE_ID + "-" + durationType))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenReturn(savedLeave);
            when(leaveMapper.toDto(savedLeave)).thenReturn(expectedDto);

            LeaveDto result = leaveIngestionService.ingest(command);

            assertNotNull(result);
            assertEquals(durationType, result.getDurationType());
        }
    }

    @Test
    void ingestShouldHandleAllSourceTypes() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        for (SourceType sourceType : SourceType.values()) {
            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .sourceType(sourceType)
                    .sourceId(sourceType.name().toLowerCase() + "-123")
                    .userId(TEST_USER_ID)
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            Leave savedLeave = Leave.builder()
                    .id(UUID.randomUUID())
                    .userId(TEST_USER_ID)
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            LeaveDto expectedDto = LeaveDto.builder()
                    .id(savedLeave.getId())
                    .userId(TEST_USER_ID)
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            when(leaveSourceRefRepository.findBySourceTypeAndSourceIdWithLeave(
                    sourceType, sourceType.name().toLowerCase() + "-123"))
                    .thenReturn(Optional.empty());
            when(leaveRepository.save(any(Leave.class))).thenReturn(savedLeave);
            when(leaveMapper.toDto(savedLeave)).thenReturn(expectedDto);

            LeaveDto result = leaveIngestionService.ingest(command);

            assertNotNull(result);
            verify(outboundSyncService).sync(savedLeave, sourceType);
        }
    }
}
