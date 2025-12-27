-- Create bulk_upload_jobs table to track CSV bulk upload jobs
CREATE TABLE bulk_upload_jobs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    total_records INTEGER NOT NULL DEFAULT 0,
    successful_records INTEGER NOT NULL DEFAULT 0,
    failed_records INTEGER NOT NULL DEFAULT 0,
    file_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    processing_time_ms BIGINT,
    result_file_path VARCHAR(500)
);

-- Create indexes for bulk_upload_jobs
CREATE INDEX idx_bulk_upload_jobs_status ON bulk_upload_jobs(status);
CREATE INDEX idx_bulk_upload_jobs_created_at ON bulk_upload_jobs(created_at DESC);

-- Create bulk_upload_records table to track individual record processing results
CREATE TABLE bulk_upload_records (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    leave_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bulk_records_job
        FOREIGN KEY (job_id)
        REFERENCES bulk_upload_jobs(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_bulk_records_leave
        FOREIGN KEY (leave_id)
        REFERENCES leave(id)
        ON DELETE SET NULL
);

-- Create indexes for bulk_upload_records
CREATE INDEX idx_bulk_records_job_id ON bulk_upload_records(job_id);
CREATE INDEX idx_bulk_records_user_id ON bulk_upload_records(user_id);
CREATE INDEX idx_bulk_records_status ON bulk_upload_records(status);

-- Add comment for documentation
COMMENT ON TABLE bulk_upload_jobs IS 'Tracks CSV bulk upload jobs and their overall status';
COMMENT ON TABLE bulk_upload_records IS 'Tracks individual records from bulk upload with success/failure status';
