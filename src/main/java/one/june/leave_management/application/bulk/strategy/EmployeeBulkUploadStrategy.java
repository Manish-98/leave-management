package one.june.leave_management.application.bulk.strategy;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy implementation for employee bulk uploads.
 * Handles result generation for processed employee records.
 * <p>
 * Note: CSV parsing and validation is handled by {@link one.june.leave_management.adapter.inbound.web.csv.CsvEmployeeParserStrategy}
 * in the adapter layer. This class focuses only on generating result files from processed records.
 */
@Component
@Slf4j
public class EmployeeBulkUploadStrategy implements BulkUploadStrategy {

    @Override
    public BulkUploadType getType() {
        return BulkUploadType.EMPLOYEE;
    }

    @Override
    public String generateResultRow(BulkUploadRecord record) {
        Map<String, String> metadata = record.getMetadata();

        // Reconstruct CSV row in original order from metadata
        // Order: name,dateOfJoining,slackId,googleId,slackDisplayName,active,carryForwardLeaves
        StringBuilder row = new StringBuilder();

        row.append(metadata.getOrDefault("name", ""));
        row.append(",");
        row.append(metadata.getOrDefault("dateofjoining", ""));
        row.append(",");
        row.append(metadata.getOrDefault("slackid", ""));
        row.append(",");
        row.append(metadata.getOrDefault("googleid", ""));
        row.append(",");
        row.append(metadata.getOrDefault("slackdisplayname", ""));
        row.append(",");

        // active is optional, default true
        String active = metadata.getOrDefault("active", "");
        if (active.isEmpty()) {
            active = "true";
        }
        row.append(active);

        // carryForwardLeaves is optional, default 0
        row.append(",");
        String carryForwardLeaves = metadata.getOrDefault("carryforwardleaves", "");
        if (carryForwardLeaves.isEmpty()) {
            carryForwardLeaves = "0";
        }
        row.append(carryForwardLeaves);

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
        return new String[]{"name", "dateOfJoining", "slackId", "googleId", "slackDisplayName", "active", "carryForwardLeaves", "status"};
    }

    /**
     * Populate metadata from EmployeeCreateCommand.
     * Used when creating BulkUploadRecord from processed commands.
     *
     * @param name The employee name
     * @param dateOfJoining The date of joining
     * @param slackId The Slack ID
     * @param googleId The Google ID
     * @param slackDisplayName The Slack display name
     * @param active Whether the employee is active
     * @param carryForwardLeaves The carry forward leaves map
     * @return A map of all employee fields
     */
    public Map<String, String> populateMetadataFromCommand(String name, String dateOfJoining,
                                                           String slackId, String googleId,
                                                           String slackDisplayName, Boolean active,
                                                           Map<Integer, Integer> carryForwardLeaves) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("name", name);
        metadata.put("dateofjoining", dateOfJoining);
        metadata.put("slackid", slackId != null ? slackId : "");
        metadata.put("googleid", googleId != null ? googleId : "");
        metadata.put("slackdisplayname", slackDisplayName != null ? slackDisplayName : "");
        metadata.put("active", active != null ? active.toString() : "true");

        // Extract carry forward leaves for current year
        int currentYear = LocalDate.now().getYear();
        int cflDays = carryForwardLeaves != null && carryForwardLeaves.containsKey(currentYear)
                ? carryForwardLeaves.get(currentYear) : 0;
        metadata.put("carryforwardleaves", String.valueOf(cflDays));

        log.debug("Populated metadata from command: {}", metadata);
        return metadata;
    }
}
