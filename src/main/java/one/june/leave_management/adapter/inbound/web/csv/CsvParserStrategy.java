package one.june.leave_management.adapter.inbound.web.csv;

import one.june.leave_management.domain.leave.model.BulkUploadType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Strategy interface for parsing CSV files in bulk upload operations.
 * <p>
 * Implementations of this interface are responsible for:
 * <ul>
 *   <li>Reading and validating CSV file structure</li>
 *   <li>Parsing CSV rows into domain commands</li>
 *   <li>Capturing CSV metadata for result generation</li>
 *   <li>Generating result CSV rows from parsed data</li>
 * </ul>
 * <p>
 * This interface follows the Strategy pattern, allowing different CSV formats
 * to be handled through different implementations without changing the bulk upload service.
 *
 * @param <T> The type of command produced by this parser (e.g., LeaveIngestionCommand, EmployeeCreateCommand)
 */
public interface CsvParserStrategy<T> {

    /**
     * Get the bulk upload type this parser handles.
     *
     * @return the bulk upload type enum value
     */
    BulkUploadType getType();

    /**
     * Parse a CSV file and convert it to a list of parsed results.
     * <p>
     * Each result contains both the domain command and the original CSV metadata
     * needed for generating result files later.
     *
     * @param file The CSV file to parse
     * @param jobId The job ID for tracking and source ID generation
     * @return List of ParsedResult containing commands and metadata
     * @throws java.io.IOException if the file cannot be read
     * @throws CsvValidationException if CSV format or data is invalid
     */
    List<ParsedResult<T>> parse(MultipartFile file, String jobId) throws java.io.IOException;

    /**
     * Get the headers for the result CSV file.
     * <p>
     * Result CSVs typically include all original columns plus a status/error column.
     *
     * @return Array of column headers for the result file
     */
    String[] getResultHeaders();

    /**
     * Generate a single row for the result CSV file from a parsed result.
     * <p>
     * This combines the original CSV metadata with processing status/error information.
     *
     * @param result The parsed result containing command, metadata, and processing status
     * @return A comma-separated string representing a single row in the result CSV
     */
    String generateResultRow(ParsedResult<T> result);
}
