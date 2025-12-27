# Bulk CSV Upload Guide

## Overview

The bulk upload feature allows administrators to import multiple leave requests at once using a CSV file. All leaves imported via bulk upload are automatically set to **APPROVED** status and skip outbound sync services.

## CSV Format

### Required Columns
- **userId**: Employee/user ID (String)
- **startDate**: Leave start date in `yyyy-MM-dd` format
- **endDate**: Leave end date in `yyyy-MM-dd` format
- **type**: Leave type - either `ANNUAL_LEAVE` or `OPTIONAL_HOLIDAY`

### Optional Columns
- **durationType**: Duration type - `FULL_DAY` (default), `FIRST_HALF`, or `SECOND_HALF`
  - If omitted, defaults to `FULL_DAY`
  - **Important**: Half-day leaves (`FIRST_HALF` or `SECOND_HALF`) must have the same startDate and endDate

## Sample CSV File

A sample CSV file is provided at: `sample-bulk-upload.csv`

```csv
userId,startDate,endDate,type,durationType
john.doe,2024-02-01,2024-02-01,ANNUAL_LEAVE,FIRST_HALF
john.doe,2024-02-02,2024-02-03,ANNUAL_LEAVE,FULL_DAY
jane.smith,2024-02-05,2024-02-05,ANNUAL_LEAVE,SECOND_HALF
jane.smith,2024-02-10,2024-02-15,ANNUAL_LEAVE,FULL_DAY
```

## API Endpoints

### 1. Upload CSV File
**POST** `/api/leaves/bulk-upload`

Uploads a CSV file and initiates asynchronous processing.

**Request:**
- Content-Type: `multipart/form-data`
- Parameter: `file` (the CSV file)
- Max file size: 10MB

**Response (202 Accepted):**
```json
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PROCESSING",
  "totalRecords": 15,
  "successfulRecords": 0,
  "failedRecords": 0,
  "resultAvailable": false
}
```

**Example using curl:**
```bash
curl -X POST http://localhost:8080/api/leaves/bulk-upload \
  -F "file=@sample-bulk-upload.csv"
```

### 2. Check Job Status
**GET** `/api/leaves/bulk-upload/status/{jobId}`

Retrieves the current status of a bulk upload job.

**Response (200 OK):**
```json
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "COMPLETED",
  "totalRecords": 15,
  "successfulRecords": 14,
  "failedRecords": 1,
  "resultAvailable": true
}
```

**Status Values:**
- `PROCESSING`: Job is currently being processed
- `COMPLETED`: Job completed successfully (may have partial failures)
- `FAILED`: Job failed entirely

**Example using curl:**
```bash
curl http://localhost:8080/api/leaves/bulk-upload/status/123e4567-e89b-12d3-a456-426614174000
```

### 3. Download Result CSV
**GET** `/api/leaves/bulk-download/{jobId}`

Downloads the result CSV file containing all rows with their status.

**Response (200 OK):**
- Content-Type: `text/csv`
- Includes all original columns plus a `status` column

**Status Column Values:**
- `SUCCESS` - Row was processed successfully
- `ERROR: {message}` - Row failed with error message

**Result CSV Example:**
```csv
userId,startDate,endDate,type,durationType,status
john.doe,2024-02-01,2024-02-01,ANNUAL_LEAVE,FIRST_HALF,SUCCESS
john.doe,2024-02-02,2024-02-03,ANNUAL_LEAVE,FULL_DAY,SUCCESS
jane.smith,2024-02-05,2024-02-05,ANNUAL_LEAVE,SECOND_HALF,SUCCESS
invalid.user,2024-02-10,2024-02-15,INVALID_TYPE,FULL_DAY,ERROR: Invalid type: INVALID_TYPE
```

**Example using curl:**
```bash
curl -O http://localhost:8080/api/leaves/bulk-download/123e4567-e89b-12d3-a456-426614174000 \
  --output bulk-upload-result-123e4567.csv
```

## Complete Workflow Example

### 1. Upload CSV
```bash
# Upload the sample CSV file
JOB_RESPONSE=$(curl -X POST http://localhost:8080/api/leaves/bulk-upload \
  -F "file=@sample-bulk-upload.csv")

# Extract jobId from response
JOB_ID=$(echo $JOB_RESPONSE | jq -r '.jobId')
echo "Job ID: $JOB_ID"
```

### 2. Poll for Completion
```bash
# Check status periodically
while true; do
  STATUS=$(curl -s http://localhost:8080/api/leaves/bulk-upload/status/$JOB_ID)
  JOB_STATUS=$(echo $STATUS | jq -r '.status')

  echo "Job status: $JOB_STATUS"
  echo "Successful: $(echo $STATUS | jq -r '.successfulRecords')"
  echo "Failed: $(echo $STATUS | jq -r '.failedRecords')"

  if [ "$JOB_STATUS" = "COMPLETED" ] || [ "$JOB_STATUS" = "FAILED" ]; then
    break
  fi

  sleep 2
done
```

### 3. Download Results
```bash
# Download result file
curl -O http://localhost:8080/api/leaves/bulk-download/$JOB_ID \
  --output bulk-upload-result-$JOB_ID.csv

echo "Results saved to bulk-upload-result-$JOB_ID.csv"
```

## Validation Rules

### Date Validation
- Dates must be in `yyyy-MM-dd` format
- `startDate` must not be after `endDate`
- Dates must be valid calendar dates

### Duration Type Validation
- `FULL_DAY`: Can span multiple days
- `FIRST_HALF` and `SECOND_HALF`: Must have same `startDate` and `endDate`
- Default: `FULL_DAY`

### Leave Type Validation
- Must be either `ANNUAL_LEAVE` or `OPTIONAL_HOLIDAY`
- Case-insensitive (converts to uppercase automatically)

### Business Logic Validation
- No overlapping leaves for the same user on the same date
- User must exist in the system

## Error Handling

### Partial Success
The bulk upload continues processing even if individual rows fail. Each failed row is recorded with an error message in the result CSV.

### Common Errors
1. **Invalid date format**: Use `yyyy-MM-dd` format
2. **Start date after end date**: Ensure `startDate <= endDate`
3. **Half-day with multiple dates**: Half-day leaves must be single day
4. **Invalid leave type**: Use `ANNUAL_LEAVE` or `OPTIONAL_HOLIDAY`
5. **Overlapping leaves**: User already has leave on the specified date(s)

## File Storage

Result files are stored in the configured directory:
- **Local**: `./bulk-upload-results` (default)
- **Docker**: `/app/bulk-upload-results`

Location can be configured via `bulk.upload.result-dir` property in `application.properties`.

## Configuration

### Application Properties
```properties
# Bulk Upload Configuration
bulk.upload.result-dir=./bulk-upload-results
```

### Docker Environment
```bash
export BULK_UPLOAD_RESULT_DIR=/app/bulk-upload-results
```

## Testing

Use the provided `sample-bulk-upload.csv` file to test the bulk upload feature:

```bash
# Test with sample file
curl -X POST http://localhost:8080/api/leaves/bulk-upload \
  -F "file=@sample-bulk-upload.csv"
```

## API Documentation

Interactive API documentation is available at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
