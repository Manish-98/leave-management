-- Add duration_type column to leave table
ALTER TABLE leave ADD COLUMN duration_type VARCHAR(20) DEFAULT 'FULL_DAY' NOT NULL;

-- Add a comment to document the new column
COMMENT ON COLUMN leave.duration_type IS 'Duration type of leave: FULL_DAY, FIRST_HALF, or SECOND_HALF';
