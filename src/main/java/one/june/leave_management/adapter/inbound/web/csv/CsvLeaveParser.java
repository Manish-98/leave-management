package one.june.leave_management.adapter.inbound.web.csv;

import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CsvLeaveParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int EXPECTED_COLUMN_COUNT = 5;

    /**
     * Parse CSV file and convert to LeaveIngestionCommand list
     *
     * @param file CSV file to parse
     * @param jobId Job ID for generating source IDs
     * @return List of LeaveIngestionCommand
     * @throws IOException if file cannot be read
     * @throws CsvValidationException if CSV format is invalid
     */
    public List<LeaveIngestionCommand> parse(MultipartFile file, String jobId) throws IOException {
        List<LeaveIngestionCommand> commands = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] headers = csvReader.readNext();
            if (headers == null) {
                throw new CsvValidationException("CSV file is empty", 0);
            }

            validateHeaders(headers);

            String[] row;
            int rowNumber = 1; // Header is row 0

            while ((row = csvReader.readNext()) != null) {
                rowNumber++;

                try {
                    validateRow(row, rowNumber);
                    LeaveIngestionCommand command = parseRow(row, jobId, rowNumber);
                    commands.add(command);
                } catch (CsvValidationException e) {
                    // Log but continue processing - let caller decide whether to fail fast or continue
                    log.warn("Failed to parse row {}: {}", rowNumber, e.getMessage());
                    throw e; // Re-throw to let service layer handle
                }
            }

            log.info("Successfully parsed {} rows from CSV file", commands.size());
            return commands;

        } catch (com.opencsv.exceptions.CsvValidationException e) {
            throw new IllegalArgumentException("Failed to parse CSV file", e);
        } catch (Exception e) {
            throw new IOException("Failed to parse CSV file", e);
        }
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
     * Validate row structure
     */
    private void validateRow(String[] row, int rowNumber) {
        if (row == null || row.length < 4) {
            throw new CsvValidationException(
                    "Invalid row format. Expected at least 4 columns, got " +
                    (row == null ? 0 : row.length), rowNumber);
        }
    }

    /**
     * Parse a single CSV row into LeaveIngestionCommand
     */
    private LeaveIngestionCommand parseRow(String[] row, String jobId, int rowNumber) {
        // Columns: userId, startDate, endDate, type, [durationType]
        String userId = parseRequiredField(row[0], "userId", rowNumber);
        LocalDate startDate = parseDate(row[1], "startDate", rowNumber);
        LocalDate endDate = parseDate(row[2], "endDate", rowNumber);
        LeaveType type = parseLeaveType(row[3], rowNumber);
        LeaveDurationType durationType = parseDurationType(row.length > 4 ? row[4] : null, rowNumber);

        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new CsvValidationException("Start date cannot be after end date", rowNumber);
        }

        // Validate duration type for half-day leaves
        if (durationType != LeaveDurationType.FULL_DAY && !startDate.equals(endDate)) {
            throw new CsvValidationException(
                    "Half-day leaves (FIRST_HALF or SECOND_HALF) must have the same start and end date", rowNumber);
        }

        // Build command
        DateRange dateRange = DateRange.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return LeaveIngestionCommand.builder()
                .userId(userId)
                .dateRange(dateRange)
                .type(type)
                .status(LeaveStatus.APPROVED) // Always APPROVED for bulk uploads
                .durationType(durationType)
                .sourceType(SourceType.CSV_BULK)
                .sourceId(String.format("csv-bulk-%s-%d", jobId, rowNumber))
                .build();
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
     * Parse leave type
     */
    private LeaveType parseLeaveType(String value, int rowNumber) {
        String trimmed = value == null ? "" : value.trim().toUpperCase();
        if (trimmed.isEmpty()) {
            throw new CsvValidationException("type is required", rowNumber);
        }

        try {
            return LeaveType.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            throw new CsvValidationException(
                    "Invalid type: " + trimmed + ". Valid values are: ANNUAL_LEAVE, OPTIONAL_HOLIDAY", rowNumber);
        }
    }

    /**
     * Parse duration type (optional, defaults to FULL_DAY)
     */
    private LeaveDurationType parseDurationType(String value, int rowNumber) {
        String trimmed = value == null ? "" : value.trim().toUpperCase();

        if (trimmed.isEmpty()) {
            return LeaveDurationType.FULL_DAY; // Default
        }

        try {
            return LeaveDurationType.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            throw new CsvValidationException(
                    "Invalid durationType: " + trimmed + ". Valid values are: FULL_DAY, FIRST_HALF, SECOND_HALF. " +
                    "If not provided, defaults to FULL_DAY.", rowNumber);
        }
    }
}
