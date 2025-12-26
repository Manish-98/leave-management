package one.june.leave_management.common.mapper;

import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.adapter.persistence.jpa.entity.LeaveJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.entity.LeaveSourceRefJpaEntity;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.dto.LeaveSourceRefDto;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LeaveMapper}
 * <p>
 * Tests mapping between different layers: Domain ↔ Entity ↔ DTO ↔ Request
 */
@DisplayName("LeaveMapper Unit Tests")
class LeaveMapperTest {

    private LeaveMapper mapper;
    private UUID testLeaveId;
    private String testUserId;
    private LocalDate testStartDate;
    private LocalDate testEndDate;
    private DateRange testDateRange;

    @BeforeEach
    void setUp() {
        mapper = new LeaveMapper();
        testLeaveId = UUID.randomUUID();
        testUserId = "user123";
        testStartDate = LocalDate.of(2025, 1, 1);
        testEndDate = LocalDate.of(2025, 1, 5);
        testDateRange = DateRange.builder()
                .startDate(testStartDate)
                .endDate(testEndDate)
                .build();
    }

    @Nested
    @DisplayName("Domain ↔ Entity Mapping Tests")
    class DomainEntityMappingTests {

        @Test
        @DisplayName("Should successfully map Domain to JPA Entity")
        void shouldSuccessfullyMapDomainToJpaEntity() {
            // Given
            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When
            LeaveJpaEntity entity = mapper.toJpaEntity(domainLeave);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(testLeaveId);
            assertThat(entity.getUserId()).isEqualTo(testUserId);
            assertThat(entity.getStartDate()).isEqualTo(testStartDate);
            assertThat(entity.getEndDate()).isEqualTo(testEndDate);
            assertThat(entity.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(entity.getStatus()).isEqualTo(LeaveStatus.REQUESTED);
            assertThat(entity.getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
        }

        @Test
        @DisplayName("Should map Domain to JPA Entity with null duration type as FULL_DAY")
        void shouldMapDomainToEntityWithNullDurationType() {
            // Given
            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(null)
                    .build();

            // When
            LeaveJpaEntity entity = mapper.toJpaEntity(domainLeave);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
        }

        @Test
        @DisplayName("Should successfully map JPA Entity to Domain")
        void shouldSuccessfullyMapJpaEntityToDomain() {
            // Given
            LeaveJpaEntity entity = LeaveJpaEntity.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .startDate(testStartDate)
                    .endDate(testEndDate)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(List.of())
                    .build();

            // When
            Leave domainLeave = mapper.toDomainEntity(entity);

            // Then
            assertThat(domainLeave).isNotNull();
            assertThat(domainLeave.getId()).isEqualTo(testLeaveId);
            assertThat(domainLeave.getUserId()).isEqualTo(testUserId);
            assertThat(domainLeave.getDateRange()).isEqualTo(testDateRange);
            assertThat(domainLeave.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(domainLeave.getStatus()).isEqualTo(LeaveStatus.REQUESTED);
            assertThat(domainLeave.getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
        }

        @Test
        @DisplayName("Should map JPA Entity to Domain with source references")
        void shouldMapJpaEntityToDomainWithSourceRefs() {
            // Given
            LeaveSourceRefJpaEntity sourceRefEntity = LeaveSourceRefJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-source-123")
                    .leave(null)
                    .build();

            LeaveJpaEntity entity = LeaveJpaEntity.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .startDate(testStartDate)
                    .endDate(testEndDate)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(List.of(sourceRefEntity))
                    .build();

            // When
            Leave domainLeave = mapper.toDomainEntity(entity);

            // Then
            assertThat(domainLeave).isNotNull();
            assertThat(domainLeave.getSourceRefs()).hasSize(1);
            LeaveSourceRef sourceRef = domainLeave.getSourceRefs().iterator().next();
            assertThat(sourceRef.getSourceType()).isEqualTo(SourceType.SLACK);
            assertThat(sourceRef.getSourceId()).isEqualTo("slack-source-123");
        }

        @Test
        @DisplayName("Should return null when mapping null Domain to Entity")
        void shouldReturnNullWhenMappingNullDomainToEntity() {
            // When
            LeaveJpaEntity entity = mapper.toJpaEntity((Leave) null);

            // Then
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("Should return null when mapping null Entity to Domain")
        void shouldReturnNullWhenMappingNullEntityToDomain() {
            // When
            Leave domainLeave = mapper.toDomainEntity((LeaveJpaEntity) null);

            // Then
            assertThat(domainLeave).isNull();
        }
    }

    @Nested
    @DisplayName("Domain ↔ DTO Mapping Tests")
    class DomainDtoMappingTests {

        @Test
        @DisplayName("Should successfully map Domain to DTO")
        void shouldSuccessfullyMapDomainToDto() {
            // Given
            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-source-123")
                    .build();

            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(List.of(sourceRef))
                    .build();

            // When
            LeaveDto dto = mapper.toDto(domainLeave);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(testLeaveId);
            assertThat(dto.getUserId()).isEqualTo(testUserId);
            assertThat(dto.getDateRange()).isEqualTo(testDateRange);
            assertThat(dto.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(dto.getStatus()).isEqualTo(LeaveStatus.REQUESTED);
            assertThat(dto.getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
            assertThat(dto.getSourceRefs()).hasSize(1);
        }

        @Test
        @DisplayName("Should successfully map DTO to Domain")
        void shouldSuccessfullyMapDtoToDomain() {
            // Given
            LeaveSourceRefDto sourceRefDto = LeaveSourceRefDto.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-source-123")
                    .build();

            LeaveDto dto = LeaveDto.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(List.of(sourceRefDto))
                    .build();

            // When
            Leave domainLeave = mapper.toDtoDomain(dto);

            // Then
            assertThat(domainLeave).isNotNull();
            assertThat(domainLeave.getId()).isEqualTo(testLeaveId);
            assertThat(domainLeave.getUserId()).isEqualTo(testUserId);
            assertThat(domainLeave.getDateRange()).isEqualTo(testDateRange);
            assertThat(domainLeave.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(domainLeave.getStatus()).isEqualTo(LeaveStatus.REQUESTED);
            assertThat(domainLeave.getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
            assertThat(domainLeave.getSourceRefs()).hasSize(1);
        }

        @Test
        @DisplayName("Should return null when mapping null Domain to DTO")
        void shouldReturnNullWhenMappingNullDomainToDto() {
            // When
            LeaveDto dto = mapper.toDto((Leave) null);

            // Then
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("Should return null when mapping null DTO to Domain")
        void shouldReturnNullWhenMappingNullDtoToDomain() {
            // When
            Leave domainLeave = mapper.toDtoDomain((LeaveDto) null);

            // Then
            assertThat(domainLeave).isNull();
        }

        @Test
        @DisplayName("Should map DTO to Domain with null source refs")
        void shouldMapDtoToDomainWithNullSourceRefs() {
            // Given
            LeaveDto dto = LeaveDto.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceRefs(null)
                    .build();

            // When
            Leave domainLeave = mapper.toDtoDomain(dto);

            // Then
            assertThat(domainLeave).isNotNull();
            assertThat(domainLeave.getSourceRefs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Request ↔ Domain Mapping Tests")
    class RequestDomainMappingTests {

        @Test
        @DisplayName("Should successfully map Request to Domain")
        void shouldSuccessfullyMapRequestToDomain() {
            // Given
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FIRST_HALF)
                    .build();

            // When
            Leave domainLeave = mapper.toRequestDomain(request);

            // Then
            assertThat(domainLeave).isNotNull();
            assertThat(domainLeave.getUserId()).isEqualTo(testUserId);
            assertThat(domainLeave.getDateRange()).isEqualTo(testDateRange);
            assertThat(domainLeave.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(domainLeave.getStatus()).isEqualTo(LeaveStatus.REQUESTED);
            assertThat(domainLeave.getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
        }

        @Test
        @DisplayName("Should successfully map Domain to Request")
        void shouldSuccessfullyMapDomainToRequest() {
            // Given
            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.SECOND_HALF)
                    .build();

            // When
            LeaveIngestionRequest request = mapper.toRequest(domainLeave);

            // Then
            assertThat(request).isNotNull();
            assertThat(request.getUserId()).isEqualTo(testUserId);
            assertThat(request.getDateRange()).isEqualTo(testDateRange);
            assertThat(request.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(request.getStatus()).isEqualTo(LeaveStatus.REQUESTED);
            assertThat(request.getDurationType()).isEqualTo(LeaveDurationType.SECOND_HALF);
            // These are expected to be null as per the mapper implementation
            assertThat(request.getSourceType()).isNull();
            assertThat(request.getSourceId()).isNull();
        }

        @Test
        @DisplayName("Should return null when mapping null Request to Domain")
        void shouldReturnNullWhenMappingNullRequestToDomain() {
            // When
            Leave domainLeave = mapper.toRequestDomain(null);

            // Then
            assertThat(domainLeave).isNull();
        }

        @Test
        @DisplayName("Should return null when mapping null Domain to Request")
        void shouldReturnNullWhenMappingNullDomainToRequest() {
            // When
            LeaveIngestionRequest request = mapper.toRequest(null);

            // Then
            assertThat(request).isNull();
        }
    }

    @Nested
    @DisplayName("Source Reference Mapping Tests")
    class SourceReferenceMappingTests {

        @Test
        @DisplayName("Should successfully map Domain SourceRef to JPA Entity")
        void shouldSuccessfullyMapDomainSourceRefToJpaEntity() {
            // Given
            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-source-123")
                    .build();

            // When
            LeaveSourceRefJpaEntity entity = mapper.toJpaEntity(sourceRef);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(sourceRef.getId());
            assertThat(entity.getSourceType()).isEqualTo(SourceType.SLACK);
            assertThat(entity.getSourceId()).isEqualTo("slack-source-123");
        }

        @Test
        @DisplayName("Should successfully map JPA Entity to Domain SourceRef")
        void shouldSuccessfullyMapJpaEntityToDomainSourceRef() {
            // Given
            UUID sourceRefId = UUID.randomUUID();
            LeaveSourceRefJpaEntity entity = LeaveSourceRefJpaEntity.builder()
                    .id(sourceRefId)
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-source-123")
                    .leave(null)
                    .build();

            // When
            LeaveSourceRef sourceRef = mapper.toDomainEntity(entity);

            // Then
            assertThat(sourceRef).isNotNull();
            assertThat(sourceRef.getId()).isEqualTo(sourceRefId);
            assertThat(sourceRef.getSourceType()).isEqualTo(SourceType.SLACK);
            assertThat(sourceRef.getSourceId()).isEqualTo("slack-source-123");
            assertThat(sourceRef.getLeaveId()).isNull();
        }

        @Test
        @DisplayName("Should successfully map Domain SourceRef to DTO")
        void shouldSuccessfullyMapDomainSourceRefToDto() {
            // Given
            LeaveSourceRef sourceRef = LeaveSourceRef.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.CALENDAR)
                    .sourceId("api-source-456")
                    .build();

            // When
            LeaveSourceRefDto dto = mapper.toDto(sourceRef);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(sourceRef.getId());
            assertThat(dto.getSourceType()).isEqualTo(SourceType.CALENDAR);
            assertThat(dto.getSourceId()).isEqualTo("api-source-456");
        }

        @Test
        @DisplayName("Should successfully map DTO SourceRef to Domain")
        void shouldSuccessfullyMapDtoSourceRefToDomain() {
            // Given
            UUID sourceRefId = UUID.randomUUID();
            LeaveSourceRefDto dto = LeaveSourceRefDto.builder()
                    .id(sourceRefId)
                    .sourceType(SourceType.WEB)
                    .sourceId("web-source-789")
                    .build();

            // When
            LeaveSourceRef sourceRef = mapper.toDtoDomain(dto);

            // Then
            assertThat(sourceRef).isNotNull();
            assertThat(sourceRef.getId()).isEqualTo(sourceRefId);
            assertThat(sourceRef.getSourceType()).isEqualTo(SourceType.WEB);
            assertThat(sourceRef.getSourceId()).isEqualTo("web-source-789");
        }

        @Test
        @DisplayName("Should return null for all source ref null mappings")
        void shouldReturnNullForAllSourceRefNullMappings() {
            // When & Then
            assertThat(mapper.toJpaEntity((LeaveSourceRef) null)).isNull();
            assertThat(mapper.toDomainEntity((LeaveSourceRefJpaEntity) null)).isNull();
            assertThat(mapper.toDto((LeaveSourceRef) null)).isNull();
            assertThat(mapper.toDtoDomain((LeaveSourceRefDto) null)).isNull();
        }
    }

    @Nested
    @DisplayName("Command Mapping Tests")
    class CommandMappingTests {

        @Test
        @DisplayName("Should successfully map request to command with all parameters")
        void shouldSuccessfullyMapRequestToCommandWithAllParameters() {
            // Given
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-source-123")
                    .build();

            SourceType sourceType = SourceType.CALENDAR;
            String sourceId = "api-source-456";

            // When
            LeaveIngestionCommand command = mapper.toCommand(request, sourceType, sourceId);

            // Then
            assertThat(command).isNotNull();
            assertThat(command.getUserId()).isEqualTo(testUserId);
            assertThat(command.getDateRange()).isEqualTo(testDateRange);
            assertThat(command.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
            assertThat(command.getStatus()).isEqualTo(LeaveStatus.REQUESTED);
            assertThat(command.getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
            // Overrides with provided parameters
            assertThat(command.getSourceType()).isEqualTo(sourceType);
            assertThat(command.getSourceId()).isEqualTo(sourceId);
        }

        @Test
        @DisplayName("Should map request to command with null override parameters")
        void shouldMapRequestToCommandWithNullOverrideParameters() {
            // Given
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-source-123")
                    .build();

            // When
            LeaveIngestionCommand command = mapper.toCommand(request, null, null);

            // Then
            assertThat(command).isNotNull();
            // Should fall back to request values when overrides are null
            assertThat(command.getSourceType()).isEqualTo(SourceType.SLACK);
            assertThat(command.getSourceId()).isEqualTo("slack-source-123");
        }

        @Test
        @DisplayName("Should return null when mapping null request to command")
        void shouldReturnNullWhenMappingNullRequestToCommand() {
            // When
            LeaveIngestionCommand command = mapper.toCommand(
                    null,
                    SourceType.SLACK,
                    "slack-source-123"
            );

            // Then
            assertThat(command).isNull();
        }

        @Test
        @DisplayName("Should map half day duration type correctly")
        void shouldMapHalfDayDurationTypeCorrectly() {
            // Given
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .durationType(LeaveDurationType.FIRST_HALF)
                    .build();

            // When
            LeaveIngestionCommand command = mapper.toCommand(
                    request,
                    SourceType.SLACK,
                    "slack-source-123"
            );

            // Then
            assertThat(command.getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
        }
    }

    @Nested
    @DisplayName("Round-Trip Mapping Tests")
    class RoundTripMappingTests {

        @Test
        @DisplayName("Should maintain data integrity through Domain → Entity → Domain round trip")
        void shouldMaintainDataIntegrityThroughDomainEntityRoundTrip() {
            // Given
            Leave originalDomain = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When - Domain → Entity → Domain
            LeaveJpaEntity entity = mapper.toJpaEntity(originalDomain);
            Leave reconstructedDomain = mapper.toDomainEntity(entity);

            // Then
            assertThat(reconstructedDomain).isEqualTo(originalDomain);
        }

        @Test
        @DisplayName("Should maintain data integrity through Domain → DTO → Domain round trip")
        void shouldMaintainDataIntegrityThroughDomainDtoRoundTrip() {
            // Given
            Leave originalDomain = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When - Domain → DTO → Domain
            LeaveDto dto = mapper.toDto(originalDomain);
            Leave reconstructedDomain = mapper.toDtoDomain(dto);

            // Then
            assertThat(reconstructedDomain).isEqualTo(originalDomain);
        }

        @Test
        @DisplayName("Should maintain data integrity through SourceRef → Entity → SourceRef round trip")
        void shouldMaintainDataIntegrityThroughSourceRefEntityRoundTrip() {
            // Given
            LeaveSourceRef originalSourceRef = LeaveSourceRef.builder()
                    .id(UUID.randomUUID())
                    .sourceType(SourceType.SLACK)
                    .sourceId("slack-source-123")
                    .build();

            // When - SourceRef → Entity → SourceRef
            LeaveSourceRefJpaEntity entity = mapper.toJpaEntity(originalSourceRef);
            LeaveSourceRef reconstructedSourceRef = mapper.toDomainEntity(entity);

            // Then
            assertThat(reconstructedSourceRef.getId()).isEqualTo(originalSourceRef.getId());
            assertThat(reconstructedSourceRef.getSourceType()).isEqualTo(originalSourceRef.getSourceType());
            assertThat(reconstructedSourceRef.getSourceId()).isEqualTo(originalSourceRef.getSourceId());
        }
    }

    @Nested
    @DisplayName("Different Leave Types and Durations Tests")
    class DifferentLeaveTypesAndDurationsTests {

        @Test
        @DisplayName("Should map ANNUAL_LEAVE type correctly")
        void shouldMapAnnualLeaveTypeCorrectly() {
            // Given
            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When
            LeaveDto dto = mapper.toDto(domainLeave);

            // Then
            assertThat(dto.getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        }

        @Test
        @DisplayName("Should map OPTIONAL_HOLIDAY type correctly")
        void shouldMapOptionalHolidayTypeCorrectly() {
            // Given
            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.OPTIONAL_HOLIDAY)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When
            LeaveDto dto = mapper.toDto(domainLeave);

            // Then
            assertThat(dto.getType()).isEqualTo(LeaveType.OPTIONAL_HOLIDAY);
        }

        @Test
        @DisplayName("Should map FIRST_HALF duration correctly")
        void shouldMapFirstHalfDurationCorrectly() {
            // Given
            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FIRST_HALF)
                    .build();

            // When
            LeaveDto dto = mapper.toDto(domainLeave);

            // Then
            assertThat(dto.getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
        }

        @Test
        @DisplayName("Should map SECOND_HALF duration correctly")
        void shouldMapSecondHalfDurationCorrectly() {
            // Given
            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.SECOND_HALF)
                    .build();

            // When
            LeaveDto dto = mapper.toDto(domainLeave);

            // Then
            assertThat(dto.getDurationType()).isEqualTo(LeaveDurationType.SECOND_HALF);
        }

        @Test
        @DisplayName("Should map FULL_DAY duration correctly")
        void shouldMapFullDayDurationCorrectly() {
            // Given
            Leave domainLeave = Leave.builder()
                    .id(testLeaveId)
                    .userId(testUserId)
                    .dateRange(testDateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .build();

            // When
            LeaveDto dto = mapper.toDto(domainLeave);

            // Then
            assertThat(dto.getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
        }
    }
}
