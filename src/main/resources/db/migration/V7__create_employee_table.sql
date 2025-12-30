-- Create employee table for storing employee information
-- Note: UUID generation is handled by JPA @PrePersist, not by database DEFAULT
CREATE TABLE IF NOT EXISTS employee (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slack_id VARCHAR(255),
    google_id VARCHAR(255),
    slack_display_name VARCHAR(255),
    date_of_joining DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    carry_forward_leaves TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create unique constraints on external IDs
-- Note: IF NOT EXISTS not supported for constraints in older PostgreSQL, so we use DO blocks
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uc_employee_slack_id') THEN
        ALTER TABLE employee ADD CONSTRAINT uc_employee_slack_id UNIQUE (slack_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uc_employee_google_id') THEN
        ALTER TABLE employee ADD CONSTRAINT uc_employee_google_id UNIQUE (google_id);
    END IF;
END $$;

-- Create index on id column for UUID performance
CREATE INDEX IF NOT EXISTS idx_employee_id ON employee(id);

-- Create index on active for filtering active/inactive employees
CREATE INDEX IF NOT EXISTS idx_employee_active ON employee(active);

-- Create index on name for search functionality
CREATE INDEX IF NOT EXISTS idx_employee_name ON employee(name);

-- Create index on created_at for sorting
CREATE INDEX IF NOT EXISTS idx_employee_created_at ON employee(created_at);
/*
CREATE OR REPLACE FUNCTION update_employee_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_employee_updated_at
BEFORE UPDATE ON employee
FOR EACH ROW
EXECUTE FUNCTION update_employee_updated_at();
*/

-- Add comments for documentation
COMMENT ON TABLE employee IS 'Stores employee information including external IDs and leave balance data';
COMMENT ON COLUMN employee.id IS 'Unique identifier for the employee';
COMMENT ON COLUMN employee.name IS 'Full name of the employee (required)';
COMMENT ON COLUMN employee.slack_id IS 'Slack user ID (unique, optional)';
COMMENT ON COLUMN employee.google_id IS 'Google user ID (unique, optional)';
COMMENT ON COLUMN employee.slack_display_name IS 'Display name in Slack (optional)';
COMMENT ON COLUMN employee.date_of_joining IS 'Date when the employee joined the company';
COMMENT ON COLUMN employee.active IS 'Employee status: true for active, false for inactive (soft delete)';
COMMENT ON COLUMN employee.carry_forward_leaves IS 'JSON string storing carry forward leaves by year (e.g., {"2024": 5, "2025": 3})';
COMMENT ON COLUMN employee.created_at IS 'Timestamp when the record was created';
COMMENT ON COLUMN employee.updated_at IS 'Timestamp when the record was last updated';
