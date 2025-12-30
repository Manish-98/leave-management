package one.june.leave_management.application.bulk.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.adapter.inbound.web.config.FileStorageConfig;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import one.june.leave_management.application.bulk.strategy.BulkUploadStrategy;
import one.june.leave_management.application.bulk.strategy.BulkUploadStrategyRegistry;
import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating CSV result files for bulk uploads.
 * Uses strategy pattern to handle different entity types (leave, employee, etc.).
 * Generates result CSV from metadata stored in bulk upload records.
 */
@Service
@Slf4j
public class CsvResultService {

    private final FileStorageConfig fileStorageConfig;
    private final BulkUploadRecordRepository bulkUploadRecordRepository;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final BulkUploadStrategyRegistry strategyRegistry;

    public CsvResultService(FileStorageConfig fileStorageConfig,
                            BulkUploadRecordRepository bulkUploadRecordRepository,
                            BulkUploadJobRepository bulkUploadJobRepository,
                            BulkUploadStrategyRegistry strategyRegistry) {
        this.fileStorageConfig = fileStorageConfig;
        this.bulkUploadRecordRepository = bulkUploadRecordRepository;
        this.bulkUploadJobRepository = bulkUploadJobRepository;
        this.strategyRegistry = strategyRegistry;
    }

    /**
     * Generate a CSV result file for a bulk upload job by ID.
     * Uses the appropriate strategy based on the job type.
     *
     * @param jobId the bulk upload job ID
     * @return the path to the generated result file
     * @throws IOException if file cannot be written
     */
    public String generateResultCsv(UUID jobId) throws IOException {
        BulkUploadJob job = bulkUploadJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk upload job not found: " + jobId));

        return generateResultFile(job);
    }

    /**
     * Generate a CSV result file for a bulk upload job.
     * Uses the appropriate strategy based on the job type to generate rows from metadata.
     *
     * @param job the bulk upload job
     * @return the path to the generated result file
     * @throws IOException if file cannot be written
     */
    public String generateResultFile(BulkUploadJob job) throws IOException {
        log.info("Generating result CSV for job {} of type {}", job.getId(), job.getType());

        // Get the appropriate strategy for this job type
        BulkUploadStrategy strategy = strategyRegistry.getStrategy(job.getType());

        // Fetch all records for this job, ordered by row number
        List<BulkUploadRecord> records = bulkUploadRecordRepository.findByJobId(job.getId());
        records.sort(Comparator.comparing(BulkUploadRecord::getRowNumber));

        // Create result file path
        String fileName = String.format("bulk-upload-result-%s.csv", job.getId());
        Path resultFilePath = fileStorageConfig.getBulkUploadResultPath().resolve(fileName);

        // Write CSV file
        try (CSVWriter writer = new CSVWriter(new FileWriter(resultFilePath.toFile()))) {
            // Write header using strategy
            String[] header = strategy.getResultHeaders();
            writer.writeNext(header);

            // Write data rows using strategy
            for (BulkUploadRecord record : records) {
                String row = strategy.generateResultRow(record);
                writer.writeNext(row.split(","));
            }
        }

        log.info("Successfully generated result CSV for job {} at {}", job.getId(), resultFilePath);
        return resultFilePath.toAbsolutePath().toString();
    }
}
