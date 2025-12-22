package one.june.leave_management.application.leave.dto;

import one.june.leave_management.domain.leave.model.SourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LeaveSourceRefDto {
    private UUID id;
    private SourceType sourceType;
    private String sourceId;
}