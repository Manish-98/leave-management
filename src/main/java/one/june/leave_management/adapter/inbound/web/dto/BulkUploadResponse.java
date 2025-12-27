package one.june.leave_management.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object for bulk leave upload")
public class BulkUploadResponse {

    @Schema(description = "Unique job ID for tracking the bulk upload", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID jobId;

    @Schema(description = "Current status of the job", example = "PROCESSING", allowableValues = {"PROCESSING", "COMPLETED", "FAILED"})
    private String status;

    @Schema(description = "Total number of records in the CSV file", example = "100")
    private Integer totalRecords;

    @Schema(description = "Number of successfully processed records", example = "95")
    private Integer successfulRecords;

    @Schema(description = "Number of failed records", example = "5")
    private Integer failedRecords;

    @Schema(description = "Whether the result CSV file is ready for download", example = "true")
    private Boolean resultAvailable;
}
