package one.june.leave_management.domain.leave.model;

import jakarta.persistence.*;
import one.june.leave_management.adapter.persistence.jpa.converter.MetadataConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bulk_upload_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = {"id"})
public class BulkUploadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @ToString.Exclude
    private BulkUploadJob job;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "entity_id")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BulkRecordStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Convert(converter = MetadataConverter.class)
    @Column(name = "metadata", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    /**
     * Mark this record as successfully processed.
     */
    public void markAsSuccess() {
        this.status = BulkRecordStatus.SUCCESS;
    }

    /**
     * Mark this record as failed with an error message.
     * @param errorMessage The error message describing what went wrong
     */
    public void markAsFailure(String errorMessage) {
        this.status = BulkRecordStatus.ERROR;
        this.errorMessage = errorMessage;
    }

    /**
     * Add a metadata key-value pair.
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Get a metadata value by key.
     * @param key The metadata key
     * @return The value, or null if not found
     */
    public String getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    public enum BulkRecordStatus {
        SUCCESS,
        ERROR
    }
}
