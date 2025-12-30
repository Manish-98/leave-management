package one.june.leave_management.adapter.inbound.web.csv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

/**
 * Wrapper class containing the result of parsing a single CSV row.
 * <p>
 * This class encapsulates both the domain command created from the CSV data
 * and the original CSV metadata needed for generating result files.
 *
 * @param <T> The type of command (e.g., LeaveIngestionCommand, EmployeeCreateCommand)
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ParsedResult<T> {

    /**
     * The domain command created from the CSV row.
     * This contains the business data that will be processed.
     */
    private T command;

    /**
     * The original CSV row data as a map of field names to values.
     * This is stored separately from the command to preserve the original
     * CSV formatting and structure for accurate result generation.
     * <p>
     * Keys are normalized field names (lowercase, no spaces).
     * Values are the raw string values from the CSV file.
     */
    private Map<String, String> csvMetadata;

    /**
     * The row number in the original CSV file (1-indexed, excluding header).
     * Used for error reporting and tracking.
     */
    private int rowNumber;

    /**
     * The validation error message if parsing failed, null otherwise.
     * This allows partial parsing results to be returned with detailed error information.
     */
    private String errorMessage;

    /**
     * Indicates whether this row was successfully parsed.
     * If true, command will be non-null. If false, errorMessage will be non-null.
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Create a successful parsed result with command and metadata.
     *
     * @param command The parsed command
     * @param csvMetadata The original CSV metadata
     * @param rowNumber The row number
     * @return A ParsedResult marked as successful
     */
    public static <T> ParsedResult<T> success(T command, Map<String, String> csvMetadata, int rowNumber) {
        return ParsedResult.<T>builder()
                .command(command)
                .csvMetadata(csvMetadata)
                .rowNumber(rowNumber)
                .success(true)
                .errorMessage(null)
                .build();
    }

    /**
     * Create a failed parsed result with error message.
     *
     * @param csvMetadata The original CSV metadata (partial)
     * @param rowNumber The row number
     * @param errorMessage The validation error message
     * @return A ParsedResult marked as failed
     */
    public static <T> ParsedResult<T> failure(Map<String, String> csvMetadata, int rowNumber, String errorMessage) {
        return ParsedResult.<T>builder()
                .command(null)
                .csvMetadata(csvMetadata)
                .rowNumber(rowNumber)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
