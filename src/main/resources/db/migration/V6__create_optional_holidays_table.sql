-- Create optional_holidays table for storing predefined optional holiday dates
-- Note: UUID generation is handled by JPA @PrePersist, not by database DEFAULT
CREATE TABLE IF NOT EXISTS optional_holidays (
    id UUID PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on id column for UUID performance
CREATE INDEX idx_optional_holidays_id ON optional_holidays(id);

-- Create index on date column for faster lookups
CREATE INDEX idx_optional_holidays_date ON optional_holidays(date);

-- Create index on created_at for sorting
CREATE INDEX idx_optional_holidays_created_at ON optional_holidays(created_at);

-- Insert sample optional holidays
INSERT INTO optional_holidays (id, date, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', '2024-01-01', 'New Year''s Day', 'First day of the year'),
    ('00000000-0000-0000-0000-000000000002', '2024-07-04', 'Independence Day', 'United States Independence Day'),
    ('00000000-0000-0000-0000-000000000003', '2024-05-27', 'Memorial Day', 'Memorial Day holiday'),
    ('00000000-0000-0000-0000-000000000004', '2024-11-28', 'Thanksgiving Day', 'Thanksgiving Day'),
    ('00000000-0000-0000-0000-000000000005', '2024-12-25', 'Christmas Day', 'Christmas Day celebration'),
    ('00000000-0000-0000-0000-000000000006', '2024-09-02', 'Labor Day', 'Labor Day holiday'),
    ('00000000-0000-0000-0000-000000000007', '2024-11-11', 'Veterans Day', 'Veterans Day observance');

-- Add trigger to update updated_at timestamp (PostgreSQL specific)
-- Note: Trigger creation is skipped for H2 compatibility
-- JPA @PrePersist/@PreUpdate handles timestamp updates automatically
-- Uncomment for PostgreSQL if automatic DB-level timestamp updates are desired
/*
CREATE OR REPLACE FUNCTION update_optional_holidays_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_optional_holidays_updated_at
BEFORE UPDATE ON optional_holidays
FOR EACH ROW
EXECUTE FUNCTION update_optional_holidays_updated_at();
*/

COMMENT ON TABLE optional_holidays IS 'Stores predefined optional holiday dates that can be selected by users in Slack';
COMMENT ON COLUMN optional_holidays.date IS 'Date of the holiday (unique)';
COMMENT ON COLUMN optional_holidays.name IS 'Display name of the holiday';
COMMENT ON COLUMN optional_holidays.description IS 'Optional description of the holiday';
