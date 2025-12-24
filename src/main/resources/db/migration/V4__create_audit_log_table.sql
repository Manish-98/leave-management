-- Create audit_log table to track all API requests and responses
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id VARCHAR(255),
    endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    source_type VARCHAR(50),
    request_body TEXT,
    response_status INTEGER,
    response_body TEXT,
    user_id VARCHAR(255),
    execution_time_ms BIGINT,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common query patterns
CREATE INDEX idx_audit_log_request_id ON audit_log(request_id);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_source_type ON audit_log(source_type);

-- Add comment for documentation
COMMENT ON TABLE audit_log IS 'Comprehensive audit trail of all API requests and responses';
COMMENT ON COLUMN audit_log.request_id IS 'Correlation ID from X-Request-Id header for request tracing';
COMMENT ON COLUMN audit_log.endpoint IS 'The API endpoint path (e.g., /api/leaves/ingest)';
COMMENT ON COLUMN audit_log.http_method IS 'HTTP method (GET, POST, PUT, DELETE, etc.)';
COMMENT ON COLUMN audit_log.source_type IS 'Source system (WEB or SLACK) derived from endpoint';
COMMENT ON COLUMN audit_log.request_body IS 'Full request payload captured as JSON';
COMMENT ON COLUMN audit_log.response_status IS 'HTTP response status code';
COMMENT ON COLUMN audit_log.response_body IS 'Full response payload captured as JSON';
COMMENT ON COLUMN audit_log.user_id IS 'User identifier extracted from request';
COMMENT ON COLUMN audit_log.execution_time_ms IS 'Request processing duration in milliseconds';
COMMENT ON COLUMN audit_log.error_message IS 'Error details if request failed';
COMMENT ON COLUMN audit_log.timestamp IS 'When the request was processed';
