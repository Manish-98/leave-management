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

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BulkRecordStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "leave_id")
    private UUID leaveId;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static BulkUploadRecord createSuccess(BulkUploadJob job, Integer rowNumber, String userId, UUID leaveId) {
        return BulkUploadRecord.builder()
                .job(job)
                .rowNumber(rowNumber)
                .userId(userId)
                .status(BulkRecordStatus.SUCCESS)
                .leaveId(leaveId)
                .build();
    }

    public static BulkUploadRecord createFailure(BulkUploadJob job, Integer rowNumber, String userId, String errorMessage) {
        return BulkUploadRecord.builder()
                .job(job)
                .rowNumber(rowNumber)
                .userId(userId)
                .status(BulkRecordStatus.ERROR)
                .errorMessage(errorMessage)
                .build();
    }

    public enum BulkRecordStatus {
        SUCCESS,
        ERROR
    }
}
