package one.june.leave_management.test.builder;

import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.adapter.persistence.jpa.entity.OptionalHolidayJpaEntity;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builder utility for creating test data objects in tests.
 * Provides convenient methods to build test entities with sensible defaults.
 */
public class LeaveTestDataBuilder {

    /**
     * Creates a default LeaveIngestionRequest with sensible defaults.
     */
    public static LeaveIngestionRequest.LeaveIngestionRequestBuilder defaultRequest() {
        return LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("test-source-" + UUID.randomUUID())
                .userId("test-user")
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2024, 1, 5))
                        .build())
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY);
    }

    /**
     * Creates a request with custom dates.
     */
    public static LeaveIngestionRequest.LeaveIngestionRequestBuilder withDates(LocalDate startDate, LocalDate endDate) {
        return defaultRequest()
                .dateRange(DateRange.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build());
    }

    /**
     * Creates a request for a specific user.
     */
    public static LeaveIngestionRequest.LeaveIngestionRequestBuilder forUser(String userId) {
        return defaultRequest().userId(userId);
    }

    /**
     * Creates a request with a specific leave type.
     */
    public static LeaveIngestionRequest.LeaveIngestionRequestBuilder withType(LeaveType type) {
        return defaultRequest().type(type);
    }

    /**
     * Creates a request with a specific status.
     */
    public static LeaveIngestionRequest.LeaveIngestionRequestBuilder withStatus(LeaveStatus status) {
        return defaultRequest().status(status);
    }

    /**
     * Creates a request with a specific duration type.
     */
    public static LeaveIngestionRequest.LeaveIngestionRequestBuilder withDurationType(LeaveDurationType durationType) {
        return defaultRequest().durationType(durationType);
    }

    /**
     * Creates a BulkUploadJob with sensible defaults.
     */
    public static BulkUploadJob.BulkUploadJobBuilder testJob() {
        return BulkUploadJob.builder()
                .id(UUID.randomUUID())
                .fileName("test-file.csv")
                .status(BulkUploadJob.BulkUploadStatus.PROCESSING)
                .totalRecords(10)
                .successfulRecords(0)
                .failedRecords(0);
    }

    /**
     * Creates a completed job.
     */
    public static BulkUploadJob.BulkUploadJobBuilder completedJob() {
        return testJob()
                .status(BulkUploadJob.BulkUploadStatus.COMPLETED)
                .successfulRecords(10)
                .failedRecords(0);
    }

    /**
     * Creates a failed job.
     */
    public static BulkUploadJob.BulkUploadJobBuilder failedJob() {
        return testJob()
                .status(BulkUploadJob.BulkUploadStatus.FAILED)
                .successfulRecords(5)
                .failedRecords(5);
    }

    /**
     * Creates a BulkUploadRecord for a successful row.
     * Note: All CSV data is now stored in metadata.
     */
    public static BulkUploadRecord.BulkUploadRecordBuilder successRecord(Integer rowNumber) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userid", "test-user");
        metadata.put("startdate", "2024-01-01");
        metadata.put("enddate", "2024-01-05");
        metadata.put("type", "ANNUAL_LEAVE");
        metadata.put("durationtype", "FULL_DAY");

        return BulkUploadRecord.builder()
                .rowNumber(rowNumber)
                .status(BulkUploadRecord.BulkRecordStatus.SUCCESS)
                .metadata(metadata);
    }

    /**
     * Creates a BulkUploadRecord for a failed row.
     */
    public static BulkUploadRecord.BulkUploadRecordBuilder errorRecord(Integer rowNumber, String errorMessage) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userid", "test-user");
        metadata.put("startdate", "2024-01-01");
        metadata.put("enddate", "2024-01-05");
        metadata.put("type", "ANNUAL_LEAVE");
        metadata.put("durationtype", "FULL_DAY");

        return BulkUploadRecord.builder()
                .rowNumber(rowNumber)
                .status(BulkUploadRecord.BulkRecordStatus.ERROR)
                .errorMessage(errorMessage)
                .metadata(metadata);
    }

    /**
     * Creates an OptionalHolidayJpaEntity with sensible defaults.
     */
    public static OptionalHolidayJpaEntity.OptionalHolidayJpaEntityBuilder testHoliday() {
        return OptionalHolidayJpaEntity.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2024, 1, 1))
                .name("Test Holiday")
                .description("Test holiday description");
    }

    /**
     * Creates an optional holiday with custom date and name.
     */
    public static OptionalHolidayJpaEntity.OptionalHolidayJpaEntityBuilder holidayOn(LocalDate date, String name) {
        return OptionalHolidayJpaEntity.builder()
                .id(UUID.randomUUID())
                .date(date)
                .name(name)
                .description(name + " description");
    }

    /**
     * Creates New Year's Day holiday.
     */
    public static OptionalHolidayJpaEntity newYearsDay() {
        return holidayOn(LocalDate.of(2024, 1, 1), "New Year's Day").build();
    }

    /**
     * Creates Christmas holiday.
     */
    public static OptionalHolidayJpaEntity christmas() {
        return holidayOn(LocalDate.of(2024, 12, 25), "Christmas Day").build();
    }

    /**
     * Creates a bulk upload job with specific record counts.
     */
    public static BulkUploadJob jobWithCounts(int total, int success, int error) {
        BulkUploadJob.BulkUploadStatus status = error > 0 ?
                BulkUploadJob.BulkUploadStatus.FAILED :
                BulkUploadJob.BulkUploadStatus.COMPLETED;

        return testJob()
                .status(status)
                .totalRecords(total)
                .successfulRecords(success)
                .failedRecords(error)
                .build();
    }

    /**
     * Creates a request from CSV data.
     */
    public static LeaveIngestionRequest.LeaveIngestionRequestBuilder fromCsvData(
            String userId,
            String startDate,
            String endDate,
            String type
    ) {
        return LeaveIngestionRequest.builder()
                .sourceType(SourceType.BULK_UPLOAD)
                .sourceId("csv-bulk-upload")
                .userId(userId)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.parse(startDate))
                        .endDate(LocalDate.parse(endDate))
                        .build())
                .type(LeaveType.valueOf(type))
                .status(LeaveStatus.REQUESTED)
                .durationType(LeaveDurationType.FULL_DAY);
    }

    /**
     * Creates a request for optional holiday leave.
     */
    public static LeaveIngestionRequest.LeaveIngestionRequestBuilder optionalHolidayLeave(
            String userId,
            LocalDate holidayDate
    ) {
        return LeaveIngestionRequest.builder()
                .sourceType(SourceType.SLACK)
                .sourceId("slack-" + UUID.randomUUID())
                .userId(userId)
                .dateRange(DateRange.builder()
                        .startDate(holidayDate)
                        .endDate(holidayDate)
                        .build())
                .type(LeaveType.OPTIONAL_HOLIDAY)
                .status(LeaveStatus.APPROVED)
                .durationType(LeaveDurationType.FULL_DAY);
    }
}
