package one.june.leave_management.adapter.inbound.web.csv;

import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
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
public class CsvLeaveParserStrategy implements CsvParserStrategy<LeaveIngestionCommand> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
        return BulkUploadType.LEAVE;
    }

    @Override
    public List<ParsedResult<LeaveIngestionCommand>> parse(MultipartFile file, String jobId) throws IOException {
        List<ParsedResult<LeaveIngestionCommand>> results = new ArrayList<>();

        // Handle BOM (Byte Order Mark) by skipping it if present
        InputStream inputStream = file.getInputStream();
        BOMInputStream bomInputStream = new BOMInputStream(inputStream);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(bomInputStream, StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] headers = csvReader.readNext();
            if (headers == null) {
                throw new CsvValidationException("CSV file is empty", 0);
            }

            validateHeaders(headers);

            Map<String, Integer> headerIndex = mapHeaderIndices(headers);

            String[] row;
            int rowNumber = 1; // Header is row 1, first data row will be 2

            while ((row = csvReader.readNext()) != null) {
                rowNumber++;

                try {
                    // Parse the row and capture metadata
                    ParsedResult<LeaveIngestionCommand> result = parseRow(row, headerIndex, jobId, rowNumber);
                    results.add(result);
                } catch (CsvValidationException e) {
                    log.warn("Failed to parse row {}: {}", rowNumber, e.getMessage());
                    // Create metadata map from current row
                    Map<String, String> metadata = createMetadataFromRow(row, headerIndex, headers);
                    results.add(ParsedResult.failure(metadata, rowNumber, e.getMessage()));
                    throw e; // Re-throw to let service layer handle
                }
            }

            log.info("Successfully parsed {} rows from CSV file", results.size());
            return results;

        } catch (com.opencsv.exceptions.CsvValidationException e) {
            throw new IllegalArgumentException("Failed to parse CSV file", e);
        } catch (Exception e) {
            throw new IOException("Failed to parse CSV file", e);
        }
    }

    @Override
    public String[] getResultHeaders() {
        return new String[]{"userId", "startDate", "endDate", "type", "durationType", "status"};
    }

    @Override
    public String generateResultRow(ParsedResult<LeaveIngestionCommand> result) {
        Map<String, String> metadata = result.getCsvMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        // Build CSV row from metadata
        StringBuilder row = new StringBuilder();
        row.append(escapeCsvField(metadata.getOrDefault("userid", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("startdate", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("enddate", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("type", ""))).append(",");
        row.append(escapeCsvField(metadata.getOrDefault("durationtype", "FULL_DAY"))).append(",");

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
        if (headers.length < 4) {
            throw new CsvValidationException(
                    "Invalid CSV format. Expected at least 4 columns: userId, startDate, endDate, type. " +
                    "Optional column: durationType", 0);
        }

        // Case-insensitive header validation
        String normalizedHeaders = String.join(",", headers).toLowerCase().replace(" ", "");

        if (!normalizedHeaders.contains("userid") ||
            !normalizedHeaders.contains("startdate") ||
            !normalizedHeaders.contains("enddate") ||
            !normalizedHeaders.contains("type")) {
            throw new CsvValidationException(
                    "Invalid CSV headers. Required columns: userId, startDate, endDate, type. " +
                    "Optional column: durationType", 0);
        }
    }

    /**
     * Map header names to column indices (case-insensitive)
     */
    private Map<String, Integer> mapHeaderIndices(String[] headers) {
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toLowerCase().replace(" ", "");
            indexMap.put(header, i);
        }
        return indexMap;
    }

    /**
     * Parse a single CSV row into LeaveIngestionCommand
     */
    private ParsedResult<LeaveIngestionCommand> parseRow(String[] row, Map<String, Integer> headerIndex,
                                                         String jobId, int rowNumber) throws CsvValidationException {
        // Create metadata map from CSV row
        Map<String, String> metadata = new HashMap<>();
        for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            String fieldName = entry.getKey();
            int index = entry.getValue();
            if (index < row.length) {
                metadata.put(fieldName, row[index] != null ? row[index].trim() : "");
            }
        }

        // Extract and validate fields
        String userId = getRequiredField(metadata, "userid", rowNumber);
        String startDateStr = getRequiredField(metadata, "startdate", rowNumber);
        String endDateStr = getRequiredField(metadata, "enddate", rowNumber);
        String typeStr = getRequiredField(metadata, "type", rowNumber);
        String durationTypeStr = metadata.getOrDefault("durationtype", "FULL_DAY");

        // Parse dates
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(startDateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CsvValidationException("startDate must be in yyyy-MM-dd format", rowNumber);
        }

        try {
            end = LocalDate.parse(endDateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CsvValidationException("endDate must be in yyyy-MM-dd format", rowNumber);
        }

        // Validate date range
        if (end.isBefore(start)) {
            throw new CsvValidationException("endDate must be after or equal to startDate", rowNumber);
        }

        // Parse leave type
        LeaveType leaveType;
        try {
            leaveType = LeaveType.valueOf(typeStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new CsvValidationException(
                    "Invalid type '" + typeStr + "'. Valid values are: ANNUAL_LEAVE, OPTIONAL_HOLIDAY", rowNumber);
        }

        // Parse duration type (optional)
        LeaveDurationType parsedDurationType;
        if (durationTypeStr == null || durationTypeStr.trim().isEmpty()) {
            parsedDurationType = LeaveDurationType.FULL_DAY;
        } else {
            try {
                parsedDurationType = LeaveDurationType.valueOf(durationTypeStr.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                throw new CsvValidationException(
                        "Invalid durationType '" + durationTypeStr + "'. Valid values are: FULL_DAY, FIRST_HALF, SECOND_HALF",
                        rowNumber);
            }
        }

        // Validate half-day constraint
        if ((parsedDurationType == LeaveDurationType.FIRST_HALF || parsedDurationType == LeaveDurationType.SECOND_HALF)
            && !start.equals(end)) {
            throw new CsvValidationException("Half-day leaves must have the same start and end date", rowNumber);
        }

        // Create command
        LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                .userId(userId)
                .dateRange(DateRange.builder()
                        .startDate(start)
                        .endDate(end)
                        .build())
                .type(leaveType)
                .durationType(parsedDurationType)
                .status(LeaveStatus.APPROVED)
                .sourceType(SourceType.BULK_UPLOAD)
                .sourceId(String.format("bulk-upload-%s-%d", jobId, rowNumber))
                .build();

        return ParsedResult.success(command, metadata, rowNumber);
    }

    /**
     * Get a required field from metadata
     */
    private String getRequiredField(Map<String, String> metadata, String fieldName, int rowNumber)
            throws CsvValidationException {
        String value = metadata.get(fieldName);
        if (value == null || value.trim().isEmpty()) {
            throw new CsvValidationException(fieldName + " is required", rowNumber);
        }
        return value.trim();
    }

    /**
     * Create metadata map from CSV row
     */
    private Map<String, String> createMetadataFromRow(String[] row, Map<String, Integer> headerIndex, String[] headers) {
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
