package one.june.leave_management.adapter.inbound.web.csv;

import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.test.util.CsvTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CSV Leave Parser Unit Tests")
class CsvLeaveParserTest {

    private CsvLeaveParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvLeaveParser();
    }

    @Test
    @DisplayName("Should parse valid CSV with required columns only")
    void shouldParseValidCsvWithRequiredColumns() throws IOException {
        // Given
        List<CsvTestUtil.CsvLeaveRecord> records = List.of(
                CsvTestUtil.CsvLeaveRecord.builder()
                        .userId("user1")
                        .startDate("2024-01-01")
                        .endDate("2024-01-05")
                        .type("ANNUAL_LEAVE")
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidCsvFile("test.csv", records);

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1");
        assertThat(commands.get(0).getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        assertThat(commands.get(0).getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY); // Default
        assertThat(commands.get(0).getSourceType()).isEqualTo(SourceType.CSV_BULK);
        assertThat(commands.get(0).getSourceId()).isEqualTo("csv-bulk-job123-2"); // Row 2 (header is row 0, first data is row 1... wait, it's using rowNumber from readNext)
    }

    @Test
    @DisplayName("Should parse valid CSV with all columns including durationType")
    void shouldParseValidCsvWithAllColumns() throws IOException {
        // Given - using same dates for half-day to pass validation
        String csvContent = "userId,startDate,endDate,type,durationType\nuser1,2024-01-01,2024-01-01,ANNUAL_LEAVE,FIRST_HALF\n";
        MultipartFile file = CsvTestUtil.createMultipartFile("test.csv", csvContent);

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1");
        assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
        assertThat(commands.get(0).getSourceId()).isEqualTo("csv-bulk-job123-2");
    }

    @Test
    @DisplayName("Should parse multiple rows successfully")
    void shouldParseMultipleRows() throws IOException {
        // Given
        List<CsvTestUtil.CsvLeaveRecord> records = List.of(
                CsvTestUtil.CsvLeaveRecord.builder().userId("user1").startDate("2024-01-01").endDate("2024-01-05").type("ANNUAL_LEAVE").build(),
                CsvTestUtil.CsvLeaveRecord.builder().userId("user2").startDate("2024-02-01").endDate("2024-02-03").type("OPTIONAL_HOLIDAY").build(),
                CsvTestUtil.CsvLeaveRecord.builder().userId("user3").startDate("2024-03-01").endDate("2024-03-01").type("ANNUAL_LEAVE").durationType("SECOND_HALF").build()
        );
        MultipartFile file = CsvTestUtil.createValidCsvFile("test.csv", records);

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1");
        assertThat(commands.get(0).getSourceId()).isEqualTo("csv-bulk-job123-2");
        assertThat(commands.get(1).getUserId()).isEqualTo("user2");
        assertThat(commands.get(1).getSourceId()).isEqualTo("csv-bulk-job123-3");
        assertThat(commands.get(2).getUserId()).isEqualTo("user3");
        assertThat(commands.get(2).getSourceId()).isEqualTo("csv-bulk-job123-4");
    }

    @Test
    @DisplayName("Should throw exception for empty CSV file")
    void shouldThrowForEmptyCsv() {
        // Given
        MultipartFile file = CsvTestUtil.createEmptyCsvFile("empty.csv");

        // When & Then - Parser wraps CsvValidationException in IOException
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to parse CSV file")
                .cause().isInstanceOf(CsvValidationException.class);
    }

    @Test
    @DisplayName("Should throw exception for missing required headers")
    void shouldThrowForMissingHeaders() {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithMissingHeaders("invalid.csv");

        // When & Then - Parser wraps CsvValidationException in IOException
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("Invalid CSV format");
                });
    }

    @Test
    @DisplayName("Should validate headers case-insensitively")
    void shouldValidateHeadersCaseInsensitively() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithMixedCaseHeaders("mixed.csv");

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1");
    }

    @Test
    @DisplayName("Should validate headers with spaces")
    void shouldValidateHeadersWithSpaces() throws IOException {
        // Given
        String csvContent = "User ID,Start Date,End Date,Type\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
        MultipartFile file = CsvTestUtil.createMultipartFile("spaces.csv", csvContent);

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1");
    }

    @Test
    @DisplayName("Should throw exception for invalid date format")
    void shouldThrowForInvalidDateFormat() {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithInvalidDateFormat("invalid-date.csv");

        // When & Then - Parser wraps CsvValidationException in IOException
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("must be in yyyy-MM-dd format");
                });
    }

    @Test
    @DisplayName("Should throw exception when start date after end date")
    void shouldThrowWhenStartDateAfterEndDate() {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithStartDateAfterEndDate("invalid-range.csv");

        // When & Then - Parser wraps CsvValidationException in IOException
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("Start date cannot be after end date");
                });
    }

    @Test
    @DisplayName("Should throw exception for invalid leave type")
    void shouldThrowForInvalidLeaveType() {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithInvalidLeaveType("invalid-type.csv");

        // When & Then - Parser wraps CsvValidationException in IOException
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("Invalid type");
                    assertThat(cause.getMessage()).contains("Valid values are: ANNUAL_LEAVE, OPTIONAL_HOLIDAY");
                });
    }

    @Test
    @DisplayName("Should parse leave type case-insensitively")
    void shouldParseLeaveTypeCaseInsensitively() throws IOException {
        // Given
        String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-01,2024-01-05,annual_leave\n";
        MultipartFile file = CsvTestUtil.createMultipartFile("case.csv", csvContent);

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
    }

    @Test
    @DisplayName("Should throw exception for invalid duration type")
    void shouldThrowForInvalidDurationType() {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithInvalidDurationType("invalid-duration.csv");

        // When & Then - Parser wraps CsvValidationException in IOException
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("Invalid durationType");
                    assertThat(cause.getMessage()).contains("Valid values are: FULL_DAY, FIRST_HALF, SECOND_HALF");
                });
    }

    @Test
    @DisplayName("Should default to FULL_DAY when durationType not provided")
    void shouldDefaultToFullDayWhenDurationTypeNotProvided() throws IOException {
        // Given
        List<CsvTestUtil.CsvLeaveRecord> records = List.of(
                CsvTestUtil.CsvLeaveRecord.builder()
                        .userId("user1")
                        .startDate("2024-01-01")
                        .endDate("2024-01-05")
                        .type("ANNUAL_LEAVE")
                        .durationType(null) // Not provided
                        .build()
        );
        MultipartFile file = CsvTestUtil.createValidCsvFile("test.csv", records);

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY);
    }

    @Test
    @DisplayName("Should throw exception for half-day leave with different dates")
    void shouldThrowForHalfDayWithDifferentDates() {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithHalfDayMismatch("halfday-mismatch.csv");

        // When & Then - Parser wraps CsvValidationException in IOException
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("Half-day leaves");
                    assertThat(cause.getMessage()).contains("must have the same start and end date");
                });
    }

    @Test
    @DisplayName("Should parse half-day leave with same dates")
    void shouldParseHalfDayWithSameDates() throws IOException {
        // Given
        String csvContent = "userId,startDate,endDate,type,durationType\nuser1,2024-01-01,2024-01-01,ANNUAL_LEAVE,FIRST_HALF\n";
        MultipartFile file = CsvTestUtil.createMultipartFile("halfday-valid.csv", csvContent);

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
    }

    @Test
    @DisplayName("Should throw exception for empty required fields")
    void shouldThrowForEmptyRequiredFields() {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithEmptyFields("empty-field.csv");

        // When & Then - Parser wraps CsvValidationException in IOException
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("userId is required");
                });
    }

    @Test
    @DisplayName("Should trim whitespace from fields")
    void shouldTrimWhitespaceFromFields() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithWhitespace("whitespace.csv");

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1"); // Trimmed
        assertThat(commands.get(0).getType()).isEqualTo(LeaveType.ANNUAL_LEAVE); // Trimmed and uppercased
    }

    @Test
    @DisplayName("Should parse duration type case-insensitively")
    void shouldParseDurationTypeCaseInsensitively() throws IOException {
        // Given
        String csvContent = "userId,startDate,endDate,type,durationType\nuser1,2024-01-01,2024-01-01,ANNUAL_LEAVE,first_half\n";
        MultipartFile file = CsvTestUtil.createMultipartFile("case-duration.csv", csvContent);

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
    }

    @Test
    @DisplayName("Should handle CSV with headers only")
    void shouldHandleCsvWithHeadersOnly() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithHeadersOnly("headers-only.csv");

        // When
        List<LeaveIngestionCommand> commands = parser.parse(file, "job123");

        // Then
        assertThat(commands).isEmpty();
    }
}
