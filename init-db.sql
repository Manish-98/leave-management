-- PostgreSQL initialization script for Leave Management Application
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create the main user schema
CREATE SCHEMA IF NOT EXISTS leave_management;

-- Set default schema
SET search_path TO leave_management, public;

-- Grant permissions
GRANT ALL ON SCHEMA leave_management TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA leave_management TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA leave_management TO postgres;

-- Log initialization
\echo 'Leave Management database initialized successfully';