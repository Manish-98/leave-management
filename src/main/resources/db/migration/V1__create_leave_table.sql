-- Add UUID extension if not exists
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create leave table
CREATE TABLE IF NOT EXISTS leave (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index on user_id for better query performance
CREATE INDEX IF NOT EXISTS idx_leave_user_id ON leave(user_id);

-- Create index on dates for range queries
CREATE INDEX IF NOT EXISTS idx_leave_dates ON leave(start_date, end_date);

-- Create index on status for filtering
CREATE INDEX IF NOT EXISTS idx_leave_status ON leave(status);