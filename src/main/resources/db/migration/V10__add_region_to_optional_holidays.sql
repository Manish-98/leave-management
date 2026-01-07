-- Add region column to optional_holidays table
-- Region is mandatory and defaults to 'PUNE' for existing records

-- Add the column as nullable first
ALTER TABLE optional_holidays ADD COLUMN IF NOT EXISTS region VARCHAR(50);

-- Update existing records to have 'PUNE' as default region
UPDATE optional_holidays SET region = 'PUNE' WHERE region IS NULL;

-- Make the column NOT NULL
ALTER TABLE optional_holidays ALTER COLUMN region SET NOT NULL;

-- Add default for future inserts
ALTER TABLE optional_holidays ALTER COLUMN region SET DEFAULT 'PUNE';

-- Add index on region for filtering
CREATE INDEX IF NOT EXISTS idx_optional_holidays_region ON optional_holidays(region);

-- Add comment for documentation
COMMENT ON COLUMN optional_holidays.region IS 'Geographical region for which this holiday is applicable (PUNE, BANGALORE, HYDERABAD)';
