-- Create bulk_upload_jobs table to track CSV bulk upload jobs
CREATE TABLE bulk_upload_jobs (
    id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL, -- LEAVE, EMPLOYEE, etc.
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
CREATE INDEX idx_bulk_upload_jobs_type ON bulk_upload_jobs(type);
CREATE INDEX idx_bulk_upload_jobs_created_at ON bulk_upload_jobs(created_at DESC);

-- Create bulk_upload_records table to track individual record processing results
-- Metadata stores all CSV fields as JSON text for result generation without fetching from domain
CREATE TABLE bulk_upload_records (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    source_type VARCHAR(50), -- WEB, SLACK, CALENDAR, KIMAI, CSV_BULK
    source_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    metadata TEXT NOT NULL DEFAULT '{}', -- All CSV fields stored as JSON string
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bulk_records_job
        FOREIGN KEY (job_id)
        REFERENCES bulk_upload_jobs(id)
        ON DELETE CASCADE
);

-- Create indexes for bulk_upload_records
CREATE INDEX idx_bulk_records_job_id ON bulk_upload_records(job_id);
CREATE INDEX idx_bulk_records_status ON bulk_upload_records(status);

-- Add comment for documentation
COMMENT ON TABLE bulk_upload_jobs IS 'Tracks CSV bulk upload jobs and their overall status';
COMMENT ON TABLE bulk_upload_records IS 'Tracks individual records from bulk upload with all CSV data in metadata';
COMMENT ON COLUMN bulk_upload_jobs.type IS 'Type of bulk upload: LEAVE, EMPLOYEE, etc.';
COMMENT ON COLUMN bulk_upload_records.metadata IS 'All CSV fields stored as JSON string for result generation';
