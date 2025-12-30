package one.june.leave_management.application.bulk.strategy;

import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.BulkUploadType;

import java.util.Map;

/**
 * Strategy interface for handling different types of bulk uploads.
 * <p>
 * Each implementation (LEAVE, EMPLOYEE, etc.) provides specific logic for:
 * <ul>
 *   <li>Identifying the bulk upload type</li>
 *   <li>Generating result CSV rows from processed bulk upload records</li>
 *   <li>Providing result CSV headers</li>
 * </ul>
 * <p>
 * Note: CSV parsing and validation is handled by {@link one.june.leave_management.adapter.inbound.web.csv.CsvParserStrategy}
 * implementations in the adapter layer. This interface focuses only on the processing phase.
 */
public interface BulkUploadStrategy {

    /**
     * Get the bulk upload type this strategy handles.
     * @return The bulk upload type
     */
    BulkUploadType getType();

    /**
     * Generate a result CSV row from a bulk upload record.
     * Uses the metadata stored in the record (populated during parsing) to generate the row.
     *
     * @param record The bulk upload record containing metadata and processing status
     * @return CSV row as a string (comma-separated values)
     */
    String generateResultRow(BulkUploadRecord record);

    /**
     * Get the CSV headers for the result file.
     * Includes all original CSV headers plus a status/error column.
     *
     * @return Array of CSV header strings
     */
    String[] getResultHeaders();
}
