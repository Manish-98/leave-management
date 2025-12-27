package one.june.leave_management.application.leave.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.web.config.FileStorageConfig;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.port.LeaveRepository;
import one.june.leave_management.domain.leave.model.BulkUploadRecord.BulkRecordStatus;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating CSV result files for bulk uploads.
 * Creates a CSV file with original data plus status column.
 */
@Service
@Slf4j
public class CsvResultService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Autowired
    private BulkUploadRecordRepository bulkUploadRecordRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private LeaveMapper leaveMapper;

    /**
     * Generate a CSV result file for a bulk upload job.
     * The file contains original data (userId, startDate, endDate, type, durationType) plus a status column.
     *
     * @param job the bulk upload job
     * @return the path to the generated result file
     * @throws IOException if file cannot be written
     */
    public String generateResultFile(BulkUploadJob job) throws IOException {
        log.info("Generating result CSV for job {}", job.getId());

        // Fetch all records for this job, ordered by row number
        List<BulkUploadRecord> records = bulkUploadRecordRepository.findByJobId(job.getId());
        records.sort((r1, r2) -> r1.getRowNumber().compareTo(r2.getRowNumber()));

        // Create result file path
        String fileName = String.format("bulk-upload-result-%s.csv", job.getId());
        Path resultFilePath = fileStorageConfig.getBulkUploadResultPath().resolve(fileName);

        // Write CSV file
        try (CSVWriter writer = new CSVWriter(new FileWriter(resultFilePath.toFile()))) {
            // Write header
            String[] header = {"userId", "startDate", "endDate", "type", "durationType", "status"};
            writer.writeNext(header);

            // Write data rows
            for (BulkUploadRecord record : records) {
                String[] row = buildRow(record);
                writer.writeNext(row);
            }
        }

        log.info("Successfully generated result CSV for job {} at {}", job.getId(), resultFilePath);
        return resultFilePath.toAbsolutePath().toString();
    }

    /**
     * Build a CSV row for a bulk upload record.
     * For successful records, fetches the leave data from database.
     * For failed records, uses minimal data with error status.
     *
     * @param record the bulk upload record
     * @return array of CSV column values
     */
    private String[] buildRow(BulkUploadRecord record) {
        if (record.getStatus() == BulkRecordStatus.SUCCESS && record.getLeaveId() != null) {
            // Fetch leave details for successful records
            return buildSuccessRow(record);
        } else {
            // Build error row with minimal data
            return buildErrorRow(record);
        }
    }

    /**
     * Build a CSV row for a successful record.
     * Fetches the leave from database to get all details.
     *
     * @param record the successful bulk upload record
     * @return array of CSV column values
     */
    private String[] buildSuccessRow(BulkUploadRecord record) {
        Leave leave = leaveRepository.findById(record.getLeaveId())
                .orElse(null);

        if (leave == null) {
            log.error("Leave {} not found for successful record {}", record.getLeaveId(), record.getId());
            return new String[]{
                    record.getUserId(),
                    "", "", "", "",
                    "SUCCESS (data not found)"
            };
        }

        return new String[]{
                record.getUserId(),
                leave.getDateRange().getStartDate().format(DATE_FORMATTER),
                leave.getDateRange().getEndDate().format(DATE_FORMATTER),
                leave.getType().name(),
                leave.getDurationType().name(),
                "SUCCESS"
        };
    }

    /**
     * Build a CSV row for a failed record.
     * Includes error message in status column.
     *
     * @param record the failed bulk upload record
     * @return array of CSV column values
     */
    private String[] buildErrorRow(BulkUploadRecord record) {
        String status = "ERROR";
        if (record.getErrorMessage() != null && !record.getErrorMessage().isEmpty()) {
            status = "ERROR: " + record.getErrorMessage();
        }

        return new String[]{
                record.getUserId(),
                "", "", "", "",
                status
        };
    }
}
