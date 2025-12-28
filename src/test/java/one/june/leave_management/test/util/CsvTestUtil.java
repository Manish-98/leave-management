package one.june.leave_management.test.util;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating test CSV files for bulk upload testing.
 */
public class CsvTestUtil {

    /**
     * Creates a valid CSV file with all required columns.
     */
    public static MultipartFile createValidCsvFile(String filename, List<CsvLeaveRecord> records) {
        String csvContent = createCsvContent(records);
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a valid CSV file with all columns including durationType.
     */
    public static MultipartFile createValidCsvWithDurationType(String filename, List<CsvLeaveRecord> records) {
        StringBuilder csv = new StringBuilder("userId,startDate,endDate,type,durationType\n");
        for (CsvLeaveRecord record : records) {
            csv.append(String.format("%s,%s,%s,%s,%s\n",
                    record.userId(),
                    record.startDate(),
                    record.endDate(),
                    record.type(),
                    record.durationType() != null ? record.durationType() : "FULL_DAY"));
        }
        return createMultipartFile(filename, csv.toString());
    }

    /**
     * Creates a CSV file with missing required headers.
     */
    public static MultipartFile createCsvWithMissingHeaders(String filename) {
        String csvContent = "userId,startDate\nuser1,2024-01-01\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a CSV file with invalid date format.
     */
    public static MultipartFile createCsvWithInvalidDateFormat(String filename) {
        String csvContent = "userId,startDate,endDate,type\nuser1,01-01-2024,01-05-2024,ANNUAL_LEAVE\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a CSV file where start date is after end date.
     */
    public static MultipartFile createCsvWithStartDateAfterEndDate(String filename) {
        String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-05,2024-01-01,ANNUAL_LEAVE\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a CSV file with invalid leave type.
     */
    public static MultipartFile createCsvWithInvalidLeaveType(String filename) {
        String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-01,2024-01-05,INVALID_TYPE\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a CSV file with invalid duration type.
     */
    public static MultipartFile createCsvWithInvalidDurationType(String filename) {
        String csvContent = "userId,startDate,endDate,type,durationType\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE,INVALID_DURATION\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a CSV file with empty required fields.
     */
    public static MultipartFile createCsvWithEmptyFields(String filename) {
        String csvContent = "userId,startDate,endDate,type\n,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a CSV file with half-day leave and different dates (should fail validation).
     */
    public static MultipartFile createCsvWithHalfDayMismatch(String filename) {
        String csvContent = "userId,startDate,endDate,type,durationType\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE,FIRST_HALF\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates an empty CSV file.
     */
    public static MultipartFile createEmptyCsvFile(String filename) {
        return createMultipartFile(filename, "");
    }

    /**
     * Creates a CSV file with only headers (no data rows).
     */
    public static MultipartFile createCsvWithHeadersOnly(String filename) {
        String csvContent = "userId,startDate,endDate,type\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a non-CSV file.
     */
    public static MultipartFile createNonCsvFile(String filename) {
        return new MockMultipartFile(filename, filename, "text/plain", "This is not a CSV file".getBytes());
    }

    /**
     * Creates a CSV file that exceeds the size limit (>10MB).
     */
    public static MultipartFile createOversizedCsvFile(String filename) {
        // Create a CSV file larger than 10MB
        // Each row is ~45 bytes, so we need at least 233,000 rows to exceed 10MB
        StringBuilder csv = new StringBuilder("userId,startDate,endDate,type\n");
        for (int i = 0; i < 250000; i++) {  // Each row ~45 bytes, 250k rows > 11MB
            csv.append(String.format("user%d,2024-01-01,2024-01-05,ANNUAL_LEAVE\n", i));
        }
        byte[] content = csv.toString().getBytes();
        return new MockMultipartFile(filename, filename, "text/csv", content);
    }

    /**
     * Creates a CSV file with case-insensitive headers.
     */
    public static MultipartFile createCsvWithMixedCaseHeaders(String filename) {
        String csvContent = "UserID,StartDate,EndDate,Type\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates a CSV file with whitespace in fields.
     */
    public static MultipartFile createCsvWithWhitespace(String filename) {
        String csvContent = "userId,startDate,endDate,type\n user1 , 2024-01-01 , 2024-01-05 , ANNUAL_LEAVE \n";
        return createMultipartFile(filename, csvContent);
    }

    /**
     * Creates CSV content string from a list of records.
     */
    public static String createCsvContent(List<CsvLeaveRecord> records) {
        StringBuilder csv = new StringBuilder("userId,startDate,endDate,type\n");
        for (CsvLeaveRecord record : records) {
            csv.append(String.format("%s,%s,%s,%s\n",
                    record.userId(),
                    record.startDate(),
                    record.endDate(),
                    record.type()));
        }
        return csv.toString();
    }

    /**
     * Creates a MultipartFile from content string.
     */
    public static MultipartFile createMultipartFile(String filename, String content) {
        return new MockMultipartFile(
                filename,
                filename + ".csv",
                "text/csv",
                content.getBytes()
        );
    }

    /**
     * Record class for CSV leave data.
     */
    public record CsvLeaveRecord(
            String userId,
            String startDate,
            String endDate,
            String type,
            String durationType
    ) {
        public static CsvLeaveRecordBuilder builder() {
            return new CsvLeaveRecordBuilder();
        }

        public static class CsvLeaveRecordBuilder {
            private String userId = "test-user";
            private String startDate = "2024-01-01";
            private String endDate = "2024-01-05";
            private String type = "ANNUAL_LEAVE";
            private String durationType = null;

            public CsvLeaveRecordBuilder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public CsvLeaveRecordBuilder startDate(String startDate) {
                this.startDate = startDate;
                return this;
            }

            public CsvLeaveRecordBuilder endDate(String endDate) {
                this.endDate = endDate;
                return this;
            }

            public CsvLeaveRecordBuilder type(String type) {
                this.type = type;
                return this;
            }

            public CsvLeaveRecordBuilder durationType(String durationType) {
                this.durationType = durationType;
                return this;
            }

            public CsvLeaveRecordBuilder dates(LocalDate start, LocalDate end) {
                this.startDate = start.toString();
                this.endDate = end.toString();
                return this;
            }

            public CsvLeaveRecord build() {
                return new CsvLeaveRecord(userId, startDate, endDate, type, durationType);
            }
        }
    }
}
