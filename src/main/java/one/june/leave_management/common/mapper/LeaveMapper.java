package one.june.leave_management.common.mapper;

import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.adapter.persistence.jpa.entity.LeaveJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.entity.LeaveSourceRefJpaEntity;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.dto.LeaveSourceRefDto;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.SourceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified mapper for Leave entity that handles mapping between all layers:
 * Domain ↔ Entity ↔ DTO ↔ Request
 */
@Component
public class LeaveMapper {

    // Domain ↔ Entity mappings
    public LeaveJpaEntity toJpaEntity(Leave leave) {
        if (leave == null) {
            return null;
        }

        return LeaveJpaEntity.builder()
                .id(leave.getId())
                .userId(leave.getUserId())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .type(leave.getType())
                .status(leave.getStatus())
                .durationType(leave.getDurationType() != null ? leave.getDurationType() : LeaveDurationType.FULL_DAY)
                .build();
    }

    public Leave toDomainEntity(LeaveJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        DateRange dateRange = DateRange.builder()
                .startDate(jpaEntity.getStartDate())
                .endDate(jpaEntity.getEndDate())
                .build();

        Leave leave = Leave.builder()
                .id(jpaEntity.getId())
                .userId(jpaEntity.getUserId())
                .dateRange(dateRange)
                .type(jpaEntity.getType())
                .status(jpaEntity.getStatus())
                .durationType(jpaEntity.getDurationType())
                .build();

        // Map source references
        jpaEntity.getSourceRefs().stream()
                .map(this::toDomainEntity)
                .forEach(leave::addSourceRef);

        return leave;
    }

    // Domain ↔ DTO mappings
    public LeaveDto toDto(Leave leave) {
        if (leave == null) {
            return null;
        }

        List<LeaveSourceRefDto> sourceRefDtos = leave.getSourceRefs().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return LeaveDto.builder()
                .id(leave.getId())
                .userId(leave.getUserId())
                .dateRange(leave.getDateRange())
                .type(leave.getType())
                .status(leave.getStatus())
                .durationType(leave.getDurationType())
                .sourceRefs(sourceRefDtos)
                .build();
    }

    public Leave toDtoDomain(LeaveDto dto) {
        if (dto == null) {
            return null;
        }

        Leave leave = Leave.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .dateRange(dto.getDateRange())
                .type(dto.getType())
                .status(dto.getStatus())
                .durationType(dto.getDurationType())
                .build();

        if (dto.getSourceRefs() != null) {
            dto.getSourceRefs().stream()
                    .map(this::toDtoDomain)
                    .forEach(leave::addSourceRef);
        }

        return leave;
    }

    // Request ↔ Domain mappings
    public Leave toRequestDomain(LeaveIngestionRequest request) {
        if (request == null) {
            return null;
        }

        return Leave.builder()
                .userId(request.getUserId())
                .dateRange(request.getDateRange())
                .type(request.getType())
                .status(request.getStatus())
                .durationType(request.getDurationType())
                .build();
    }

    public LeaveIngestionRequest toRequest(Leave domain) {
        if (domain == null) {
            return null;
        }

        return LeaveIngestionRequest.builder()
                .sourceType(null) // This needs to be set based on context
                .sourceId(null)   // This needs to be set based on context
                .userId(domain.getUserId())
                .dateRange(domain.getDateRange())
                .type(domain.getType())
                .status(domain.getStatus())
                .durationType(domain.getDurationType())
                .build();
    }

    // Additional utility methods for source reference mappings
    public LeaveSourceRefJpaEntity toJpaEntity(LeaveSourceRef sourceRef) {
        if (sourceRef == null) {
            return null;
        }

        return LeaveSourceRefJpaEntity.builder()
                .id(sourceRef.getId())
                .sourceType(sourceRef.getSourceType())
                .sourceId(sourceRef.getSourceId())
                // Note: leave relationship is handled at the entity level
                .build();
    }

    public LeaveSourceRef toDomainEntity(LeaveSourceRefJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return LeaveSourceRef.builder()
                .id(jpaEntity.getId())
                .sourceType(jpaEntity.getSourceType())
                .sourceId(jpaEntity.getSourceId())
                .leaveId(jpaEntity.getLeave() != null ? jpaEntity.getLeave().getId() : null)
                .build();
    }

    public LeaveSourceRefDto toDto(LeaveSourceRef sourceRef) {
        if (sourceRef == null) {
            return null;
        }

        return LeaveSourceRefDto.builder()
                .id(sourceRef.getId())
                .sourceType(sourceRef.getSourceType())
                .sourceId(sourceRef.getSourceId())
                .build();
    }

    public LeaveSourceRef toDtoDomain(LeaveSourceRefDto dto) {
        if (dto == null) {
            return null;
        }

        return LeaveSourceRef.builder()
                .id(dto.getId())
                .sourceType(dto.getSourceType())
                .sourceId(dto.getSourceId())
                .build();
    }

    // Command mapping for ingestion
    public LeaveIngestionCommand toCommand(LeaveIngestionRequest request, SourceType sourceType, String sourceId) {
        if (request == null) {
            return null;
        }

        return LeaveIngestionCommand.builder()
                .sourceType(sourceType != null ? sourceType : request.getSourceType())
                .sourceId(sourceId != null ? sourceId : request.getSourceId())
                .userId(request.getUserId())
                .dateRange(request.getDateRange())
                .type(request.getType())
                .status(request.getStatus())
                .durationType(request.getDurationType())
                .build();
    }
}