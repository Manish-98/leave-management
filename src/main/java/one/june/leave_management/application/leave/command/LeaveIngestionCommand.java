package one.june.leave_management.application.leave.command;

import one.june.leave_management.common.model.DateRange;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;

import java.time.LocalDate;
import java.util.Objects;

@Setter
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LeaveIngestionCommand {
    private SourceType sourceType;
    private String sourceId;
    private String userId;
    private DateRange dateRange;
    private LeaveType type;
    private LeaveStatus status;

    // Convenience getters for backward compatibility
    public LocalDate getStartDate() {
        return dateRange != null ? dateRange.getStartDate() : null;
    }

    public LocalDate getEndDate() {
        return dateRange != null ? dateRange.getEndDate() : null;
    }

    // Custom builder with validation
    public static class LeaveIngestionCommandBuilder {
        public LeaveIngestionCommandBuilder sourceType(SourceType sourceType) {
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
            return this;
        }

        public LeaveIngestionCommandBuilder sourceId(String sourceId) {
            this.sourceId = Objects.requireNonNull(sourceId, "sourceId cannot be null");
            return this;
        }

        public LeaveIngestionCommandBuilder userId(String userId) {
            this.userId = Objects.requireNonNull(userId, "userId cannot be null");
            return this;
        }

        public LeaveIngestionCommandBuilder startDate(LocalDate startDate) {
            if (this.dateRange == null) {
                this.dateRange = DateRange.builder().build();
            }
            this.dateRange.setStartDate(Objects.requireNonNull(startDate, "startDate cannot be null"));
            return this;
        }

        public LeaveIngestionCommandBuilder endDate(LocalDate endDate) {
            if (this.dateRange == null) {
                this.dateRange = DateRange.builder().build();
            }
            this.dateRange.setEndDate(Objects.requireNonNull(endDate, "endDate cannot be null"));
            return this;
        }

        public LeaveIngestionCommandBuilder dateRange(DateRange dateRange) {
            this.dateRange = dateRange;
            return this;
        }

        public LeaveIngestionCommandBuilder type(LeaveType type) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
            return this;
        }

        public LeaveIngestionCommandBuilder status(LeaveStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            return this;
        }

        public LeaveIngestionCommand build() {
            Objects.requireNonNull(sourceType, "sourceType cannot be null");
            Objects.requireNonNull(sourceId, "sourceId cannot be null");
            Objects.requireNonNull(userId, "userId cannot be null");
            Objects.requireNonNull(dateRange, "dateRange cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(status, "status cannot be null");

            LocalDate startDate = dateRange.getStartDate();
            LocalDate endDate = dateRange.getEndDate();
            Objects.requireNonNull(startDate, "startDate cannot be null");
            Objects.requireNonNull(endDate, "endDate cannot be null");

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("startDate cannot be after endDate");
            }

            return new LeaveIngestionCommand(sourceType, sourceId, userId, dateRange, type, status);
        }
    }
}