-- Remove source_type and source_id columns from bulk_upload_records
-- These were used to track CSV_BULK source, which is now handled by LeaveSourceRef with BULK_UPLOAD type

-- Drop the columns (no data migration needed as these were only for CSV_BULK tracking)
ALTER TABLE bulk_upload_records DROP COLUMN IF EXISTS source_type;
ALTER TABLE bulk_upload_records DROP COLUMN IF EXISTS source_id;

-- Add entity_id column to reference the created entity (Leave UUID, Employee ID, etc.)
ALTER TABLE bulk_upload_records ADD COLUMN entity_id UUID;

-- Create index on entity_id for efficient lookups
CREATE INDEX idx_bulk_records_entity_id ON bulk_upload_records(entity_id);

-- Add comment for documentation
COMMENT ON COLUMN bulk_upload_records.entity_id IS 'References the created entity (Leave UUID, Employee ID, etc.) from bulk upload';
