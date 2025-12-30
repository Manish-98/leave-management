package one.june.leave_management.domain.leave.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_upload_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = {"id"})
public class BulkUploadJob {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BulkUploadType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BulkUploadStatus status;

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords;

    @Column(name = "successful_records", nullable = false)
    private Integer successfulRecords;

    @Column(name = "failed_records", nullable = false)
    private Integer failedRecords;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "result_file_path")
    private String resultFilePath;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = BulkUploadStatus.PROCESSING;
        }
        if (totalRecords == null) {
            totalRecords = 0;
        }
        if (successfulRecords == null) {
            successfulRecords = 0;
        }
        if (failedRecords == null) {
            failedRecords = 0;
        }
    }

    public void markAsCompleted() {
        this.status = BulkUploadStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        if (createdAt != null) {
            this.processingTimeMs = java.time.Duration.between(createdAt, completedAt).toMillis();
        }
    }

    public void markAsFailed() {
        this.status = BulkUploadStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        if (createdAt != null) {
            this.processingTimeMs = java.time.Duration.between(createdAt, completedAt).toMillis();
        }
    }

    public void incrementSuccess() {
        this.successfulRecords++;
    }

    public void incrementFailure() {
        this.failedRecords++;
    }

    public enum BulkUploadStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
