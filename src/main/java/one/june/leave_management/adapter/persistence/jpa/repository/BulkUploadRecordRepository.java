package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.BulkUploadRecord.BulkRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkUploadRecordRepository extends JpaRepository<BulkUploadRecord, Long> {

    List<BulkUploadRecord> findByJobId(UUID jobId);

    List<BulkUploadRecord> findByJobAndStatus(BulkUploadJob job, BulkRecordStatus status);

    void deleteByJobId(UUID jobId);
}
