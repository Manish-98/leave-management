package one.june.leave_management.application.bulk.strategy;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy implementation for leave bulk uploads.
 * Handles result generation for processed leave records.
 * <p>
 * Note: CSV parsing and validation is handled by {@link one.june.leave_management.adapter.inbound.web.csv.CsvLeaveParserStrategy}
 * in the adapter layer. This class focuses only on generating result files from processed records.
 */
@Component
@Slf4j
public class LeaveBulkUploadStrategy implements BulkUploadStrategy {

    @Override
    public BulkUploadType getType() {
        return BulkUploadType.LEAVE;
    }

    @Override
    public String generateResultRow(BulkUploadRecord record) {
        Map<String, String> metadata = record.getMetadata();

        // Reconstruct CSV row in original order from metadata
        // Order: userId,startDate,endDate,type,durationType
        StringBuilder row = new StringBuilder();

        row.append(metadata.getOrDefault("userid", ""));
        row.append(",");
        row.append(metadata.getOrDefault("startdate", ""));
        row.append(",");
        row.append(metadata.getOrDefault("enddate", ""));
        row.append(",");
        row.append(metadata.getOrDefault("type", ""));
        row.append(",");

        // durationType is optional
        String durationType = metadata.getOrDefault("durationtype", "");
        if (durationType.isEmpty()) {
            durationType = "FULL_DAY"; // Default value
        }
        row.append(durationType);

        // Add status
        row.append(",");
        if (record.getStatus() == BulkUploadRecord.BulkRecordStatus.SUCCESS) {
            row.append("SUCCESS");
        } else {
            row.append("ERROR: ").append(record.getErrorMessage() != null ? record.getErrorMessage() : "Unknown error");
        }

        return row.toString();
    }

    @Override
    public String[] getResultHeaders() {
        return new String[]{"userId", "startDate", "endDate", "type", "durationType", "status"};
    }

    /**
     * Populate metadata from LeaveIngestionCommand.
     * Used when creating BulkUploadRecord from processed commands.
     *
     * @param userId The user ID from the command
     * @param startDate The start date
     * @param endDate The end date
     * @param type The leave type
     * @param durationType The duration type
     * @return A map of all leave fields
     */
    public Map<String, String> populateMetadataFromCommand(String userId, String startDate, String endDate,
                                                           String type, String durationType) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userid", userId);
        metadata.put("startdate", startDate);
        metadata.put("enddate", endDate);
        metadata.put("type", type);
        metadata.put("durationtype", durationType != null ? durationType : "FULL_DAY");

        log.debug("Populated metadata from command: {}", metadata);
        return metadata;
    }
}
