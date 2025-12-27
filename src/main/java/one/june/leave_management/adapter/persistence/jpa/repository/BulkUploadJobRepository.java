package one.june.leave_management.adapter.persistence.jpa.repository;

import one.june.leave_management.domain.leave.model.BulkUploadJob;
import one.june.leave_management.domain.leave.model.BulkUploadJob.BulkUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulkUploadJobRepository extends JpaRepository<BulkUploadJob, UUID> {

    Optional<BulkUploadJob> findById(UUID id);

    boolean existsByStatus(BulkUploadStatus status);
}
