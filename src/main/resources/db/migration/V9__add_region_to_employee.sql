-- Add region column to employee table
-- Region is mandatory and defaults to 'PUNE' for existing records

-- Add the column as nullable first
ALTER TABLE employee ADD COLUMN IF NOT EXISTS region VARCHAR(50);

-- Update existing records to have 'PUNE' as default region
UPDATE employee SET region = 'PUNE' WHERE region IS NULL;

-- Make the column NOT NULL
ALTER TABLE employee ALTER COLUMN region SET NOT NULL;

-- Add default for future inserts
ALTER TABLE employee ALTER COLUMN region SET DEFAULT 'PUNE';

-- Add index on region for filtering
CREATE INDEX IF NOT EXISTS idx_employee_region ON employee(region);

-- Add comment for documentation
COMMENT ON COLUMN employee.region IS 'Geographical region of the employee (PUNE, BANGALORE, HYDERABAD)';
