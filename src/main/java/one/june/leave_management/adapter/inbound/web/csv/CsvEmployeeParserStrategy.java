package one.june.leave_management.adapter.inbound.web.csv;

import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.application.employee.command.EmployeeCreateCommand;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CsvEmployeeParserStrategy implements CsvParserStrategy<EmployeeCreateCommand> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CURRENT_YEAR = LocalDate.now().getYear();

    /**
     * InputStream wrapper that skips BOM (Byte Order Mark) if present.
     */
    private static class BOMInputStream extends InputStream {
        private final InputStream delegate;
        private boolean bomSkipped = false;

        public BOMInputStream(InputStream in) {
            this.delegate = in;
        }

        @Override
        public int read() throws IOException {
            if (!bomSkipped) {
                bomSkipped = true;
                // Check for UTF-8 BOM: EF BB BF
                int first = delegate.read();
                if (first == 0xEF) {
                    int second = delegate.read();
                    if (second == 0xBB) {
                        int third = delegate.read();
                        if (third == 0xBF) {
                            // BOM found and skipped, read first actual byte
                            return delegate.read();
                        }
                        // Not a BOM, push back the bytes
                        return first;
                    }
                    // Not a BOM, push back the bytes
                    return first;
                }
                return first;
            }
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!bomSkipped) {
                int firstByte = read();
                if (firstByte == -1) {
                    return -1;
                }
                if (b.length > 0) {
                    b[off] = (byte) firstByte;
                    int bytesRead = delegate.read(b, off + 1, len - 1);
                    return bytesRead == -1 ? 1 : bytesRead + 1;
                }
                return 1;
            }
            return delegate.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    @Override
    public BulkUploadType getType() {
        return BulkUploadType.EMPLOYEE;
    }

    @Override
    public List<ParsedResult<EmployeeCreateCommand>> parse(MultipartFile file, String jobId) throws IOException {
        List<ParsedResult<EmployeeCreateCommand>> results = new ArrayList<>();

        // Handle BOM (Byte Order Mark) by skipping it if present
        InputStream inputStream = file.getInputStream();
        BOMInputStream bomInputStream = new BOMInputStream(inputStream);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(bomInputStream, StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] headers = csvReader.readNext();
            if (headers == null) {
                throw new IllegalArgumentException("Failed to parse CSV file",
                        new CsvValidationException("CSV file is empty", 0));
            }

            try {
                validateHeaders(headers);
            } catch (CsvValidationException e) {
                throw new IllegalArgumentException("Failed to parse CSV file", e);
            }

            Map<String, Integer> headerIndex = mapHeaderIndices(headers);

            String[] row;
            int rowNumber = 1; // Header is row 1, first data row will be 2

            while ((row = csvReader.readNext()) != null) {
                rowNumber++;

                try {
                    validateRow(row, rowNumber);
                    ParsedResult<EmployeeCreateCommand> result = parseRow(row, headerIndex, rowNumber);
                    results.add(result);
                } catch (CsvValidationException e) {
                    log.warn("Failed to parse row {}: {}", rowNumber, e.getMessage());
                    // Create metadata map from current row
                    Map<String, String> metadata = createMetadataFromRow(row, headerIndex);
                    results.add(ParsedResult.failure(metadata, rowNumber, e.getMessage()));
                    throw new IllegalArgumentException("Failed to parse CSV file", e);
                }
            }

            log.info("Successfully parsed {} rows from CSV file", results.size());
            return results;

        } catch (com.opencsv.exceptions.CsvValidationException e) {
            throw new IllegalArgumentException("Failed to parse CSV file", e);
        }
    }

    @Override
    public String[] getResultHeaders() {
        return new String[]{"name", "dateOfJoining", "slackId", "googleId", "slackDisplayName",
                           "active", "carryForwardLeaves", "status"};
    }

    @Override
    public String generateResultRow(ParsedResult<EmployeeCreateCommand> result) {
        Map<String, String> metadata = result.getCsvMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        // Build CSV row from metadata
        StringBuilder row = new StringBuilder();
        row.append(escapeCsvField(metadata.getOrDefault("name", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("dateofjoining", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("slackid", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("googleid", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("slackdisplayname", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("active", "true"))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("carryforwardleaves", "0"))).append(",");

        // Add status
        if (result.isSuccess()) {
            row.append("SUCCESS");
        } else {
            row.append("ERROR: ").append(escapeCsvField(result.getErrorMessage() != null ? result.getErrorMessage() : ""));
        }

        return row.toString();
    }

    /**
     * Validate CSV headers
     */
    private void validateHeaders(String[] headers) {
        if (headers.length < 3) {
            throw new CsvValidationException(
                    "Invalid CSV format. Expected at least 3 columns: name, dateOfJoining, and (slackId OR googleId). " +
                    "Optional columns: slackDisplayName, active, carryForwardLeaves", 0);
        }

        // Case-insensitive header validation
        String normalizedHeaders = String.join(",", headers).toLowerCase().replace(" ", "");

        if (!normalizedHeaders.contains("name")) {
            throw new CsvValidationException(
                    "Invalid CSV headers. Required column: name. " +
                    "Optional columns: slackId, googleId, slackDisplayName, dateOfJoining, active, carryForwardLeaves", 0);
        }

        if (!normalizedHeaders.contains("dateofjoining")) {
            throw new CsvValidationException(
                    "Invalid CSV headers. Required column: dateOfJoining. " +
                    "Optional columns: name, slackId, googleId, slackDisplayName, active, carryForwardLeaves", 0);
        }

        if (!normalizedHeaders.contains("slackid") && !normalizedHeaders.contains("googleid")) {
            throw new CsvValidationException(
                    "Invalid CSV headers. At least one external ID column (slackId OR googleId) is required. " +
                    "Optional columns: name, slackDisplayName, dateOfJoining, active, carryForwardLeaves", 0);
        }
    }

    /**
     * Map header names to column indices (case-insensitive)
     */
    private Map<String, Integer> mapHeaderIndices(String[] row) {
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < row.length; i++) {
            String header = row[i].toLowerCase().replace(" ", "");
            indexMap.put(header, i);
        }
        return indexMap;
    }

    /**
     * Validate row structure
     */
    private void validateRow(String[] row, int rowNumber) {
        if (row == null || row.length < 3) {
            throw new CsvValidationException(
                    "Invalid row format. Expected at least 3 columns, got " +
                    (row == null ? 0 : row.length), rowNumber);
        }
    }

    /**
     * Get value from row at the specified header index, with bounds checking
     */
    private String getRowValue(String[] row, Map<String, Integer> headerIndex, String fieldName) {
        Integer index = headerIndex.get(fieldName);
        if (index == null || index >= row.length) {
            return ""; // Return empty string if field is missing or out of bounds
        }
        return row[index];
    }

    /**
     * Parse a single CSV row into EmployeeCreateCommand
     */
    private ParsedResult<EmployeeCreateCommand> parseRow(String[] row, Map<String, Integer> headerIndex,
                                                         int rowNumber) {
        // Create metadata map from CSV row
        Map<String, String> metadata = new HashMap<>();
        for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            String fieldName = entry.getKey();
            int index = entry.getValue();
            if (index < row.length) {
                metadata.put(fieldName, row[index] != null ? row[index].trim() : "");
            }
        }

        // Extract required fields
        String name = parseRequiredField(getRowValue(row, headerIndex, "name"), "name", rowNumber);
        String slackId = headerIndex.containsKey("slackid") ?
                parseOptionalField(getRowValue(row, headerIndex, "slackid"), "slackId", rowNumber) : null;
        String googleId = headerIndex.containsKey("googleid") ?
                parseOptionalField(getRowValue(row, headerIndex, "googleid"), "googleId", rowNumber) : null;
        String slackDisplayName = headerIndex.containsKey("slackdisplayname") ?
                parseOptionalField(getRowValue(row, headerIndex, "slackdisplayname"), "slackDisplayName", rowNumber) : null;
        LocalDate dateOfJoining = parseDate(getRowValue(row, headerIndex, "dateofjoining"), "dateOfJoining", rowNumber);
        Boolean active = headerIndex.containsKey("active") ?
                parseBoolean(getRowValue(row, headerIndex, "active"), "active", rowNumber) : true;
        String carryForwardLeavesStr = headerIndex.containsKey("carryforwardleaves") ?
                parseOptionalField(getRowValue(row, headerIndex, "carryforwardleaves"), "carryForwardLeaves", rowNumber) : "0";

        // Validate at least one external ID
        if ((slackId == null || slackId.trim().isEmpty()) &&
            (googleId == null || googleId.trim().isEmpty())) {
            throw new CsvValidationException(
                    "At least one external ID (slackId or googleId) is required", rowNumber);
        }

        // Parse carry forward leaves and use current year
        int carryForwardDays = parseInteger(carryForwardLeavesStr, "carryForwardLeaves", rowNumber);
        Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
        if (carryForwardDays > 0) {
            carryForwardLeaves.put(CURRENT_YEAR, carryForwardDays);
        }

        EmployeeCreateCommand command = EmployeeCreateCommand.builder()
                .name(name)
                .slackId(slackId)
                .googleId(googleId)
                .slackDisplayName(slackDisplayName)
                .dateOfJoining(dateOfJoining)
                .active(active)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        return ParsedResult.success(command, metadata, rowNumber);
    }

    /**
     * Parse required field
     */
    private String parseRequiredField(String value, String fieldName, int rowNumber) {
        if (value == null || value.trim().isEmpty()) {
            throw new CsvValidationException(fieldName + " is required", rowNumber);
        }
        return value.trim();
    }

    /**
     * Parse optional field
     */
    private String parseOptionalField(String value, String fieldName, int rowNumber) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /**
     * Parse date field
     */
    private LocalDate parseDate(String value, String fieldName, int rowNumber) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new CsvValidationException(fieldName + " is required", rowNumber);
        }

        try {
            return LocalDate.parse(trimmed, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CsvValidationException(
                    fieldName + " must be in yyyy-MM-dd format, got: " + trimmed, rowNumber);
        }
    }

    /**
     * Parse boolean field (supports: true/false, yes/no, 1/0)
     */
    private Boolean parseBoolean(String value, String fieldName, int rowNumber) {
        String trimmed = value == null ? "" : value.trim().toLowerCase();

        if (trimmed.isEmpty()) {
            return true; // Default to true
        }

        if (trimmed.equals("true") || trimmed.equals("yes") || trimmed.equals("1")) {
            return true;
        } else if (trimmed.equals("false") || trimmed.equals("no") || trimmed.equals("0")) {
            return false;
        } else {
            throw new CsvValidationException(
                    fieldName + " must be a boolean value (true/false, yes/no, 1/0), got: " + value, rowNumber);
        }
    }

    /**
     * Parse integer field
     */
    private Integer parseInteger(String value, String fieldName, int rowNumber) {
        String trimmed = value == null ? "" : value.trim();

        if (trimmed.isEmpty()) {
            return 0; // Default to 0
        }

        try {
            int intValue = Integer.parseInt(trimmed);
            if (intValue < 0) {
                throw new CsvValidationException(fieldName + " cannot be negative", rowNumber);
            }
            return intValue;
        } catch (NumberFormatException e) {
            throw new CsvValidationException(
                    fieldName + " must be a valid integer, got: " + trimmed, rowNumber);
        }
    }

    /**
     * Create metadata map from CSV row
     */
    private Map<String, String> createMetadataFromRow(String[] row, Map<String, Integer> headerIndex) {
        Map<String, String> metadata = new HashMap<>();
        for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            String fieldName = entry.getKey();
            int index = entry.getValue();
            if (index < row.length) {
                metadata.put(fieldName, row[index] != null ? row[index].trim() : "");
            }
        }
        return metadata;
    }

    /**
     * Escape a CSV field if it contains special characters
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // If field contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
