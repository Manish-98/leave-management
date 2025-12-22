-- Create leave_source_ref table
CREATE TABLE IF NOT EXISTS leave_source_ref (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    leave_id UUID NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint to leave table
    CONSTRAINT fk_leave_source_ref_leave_id
        FOREIGN KEY (leave_id)
        REFERENCES leave(id)
        ON DELETE CASCADE
);

-- Create unique constraint on (source_type, source_id)
CREATE UNIQUE INDEX IF NOT EXISTS uk_leave_source_ref_type_id
    ON leave_source_ref(source_type, source_id);

-- Create index on leave_id for faster joins
CREATE INDEX IF NOT EXISTS idx_leave_source_ref_leave_id
    ON leave_source_ref(leave_id);

-- Create index on source_type for filtering
CREATE INDEX IF NOT EXISTS idx_leave_source_ref_source_type
    ON leave_source_ref(source_type);