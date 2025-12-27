package one.june.leave_management.adapter.inbound.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for file storage directories.
 * Creates necessary directories on application startup.
 */
@Configuration
public class FileStorageConfig {

    private final Path bulkUploadResultPath;

    public FileStorageConfig(@Value("${bulk.upload.result-dir:./bulk-upload-results}") String resultDir) throws IOException {
        this.bulkUploadResultPath = Paths.get(resultDir).toAbsolutePath().normalize();

        // Create directory if it doesn't exist
        if (!Files.exists(this.bulkUploadResultPath)) {
            Files.createDirectories(this.bulkUploadResultPath);
        }
    }

    /**
     * Get the path where bulk upload result files are stored.
     *
     * @return the absolute path to the bulk upload results directory
     */
    public Path getBulkUploadResultPath() {
        return this.bulkUploadResultPath;
    }
}
