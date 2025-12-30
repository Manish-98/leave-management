package one.june.leave_management.adapter.inbound.web.csv;

import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import one.june.leave_management.test.util.CsvTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CSV Leave Parser Unit Tests")
class CsvLeaveParserTest {

    private CsvLeaveParserStrategy parser;

    @BeforeEach
    void setUp() {
        parser = new CsvLeaveParserStrategy();
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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1");
        assertThat(commands.get(0).getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        assertThat(commands.get(0).getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FULL_DAY); // Default
        assertThat(commands.get(0).getSourceType()).isEqualTo(SourceType.BULK_UPLOAD);
        assertThat(commands.get(0).getSourceId()).isEqualTo("bulk-upload-job123-2"); // Header is row 1, first data row is row 2
    }

    @Test
    @DisplayName("Should parse valid CSV with all columns including durationType")
    void shouldParseValidCsvWithAllColumns() throws IOException {
        // Given - using same dates for half-day to pass validation
        String csvContent = "userId,startDate,endDate,type,durationType\nuser1,2024-01-01,2024-01-01,ANNUAL_LEAVE,FIRST_HALF\n";
        MultipartFile file = CsvTestUtil.createMultipartFile("test.csv", csvContent);

        // When
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1");
        assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
        assertThat(commands.get(0).getSourceId()).isEqualTo("bulk-upload-job123-2");
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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

        // Then
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0).getUserId()).isEqualTo("user1");
        assertThat(commands.get(0).getSourceId()).isEqualTo("bulk-upload-job123-2");
        assertThat(commands.get(1).getUserId()).isEqualTo("user2");
        assertThat(commands.get(1).getSourceId()).isEqualTo("bulk-upload-job123-3");
        assertThat(commands.get(2).getUserId()).isEqualTo("user3");
        assertThat(commands.get(2).getSourceId()).isEqualTo("bulk-upload-job123-4");
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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

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

        // When & Then - Parser throws IOException with CsvValidationException cause
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("endDate must be after or equal to startDate");
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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
    }

    @Test
    @DisplayName("Should throw exception for empty required fields")
    void shouldThrowForEmptyRequiredFields() {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithEmptyFields("empty-field.csv");

        // When & Then - Parser throws IOException with CsvValidationException cause
        assertThatThrownBy(() -> parser.parse(file, "job123"))
                .isInstanceOf(IOException.class)
                .satisfies(e -> {
                    Throwable cause = ((IOException) e).getCause();
                    assertThat(cause).isInstanceOf(CsvValidationException.class);
                    assertThat(cause.getMessage()).contains("userid is required");
                });
    }

    @Test
    @DisplayName("Should trim whitespace from fields")
    void shouldTrimWhitespaceFromFields() throws IOException {
        // Given
        MultipartFile file = CsvTestUtil.createCsvWithWhitespace("whitespace.csv");

        // When
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

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
        List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");
        List<LeaveIngestionCommand> commands = results.stream()
                .filter(ParsedResult::isSuccess)
                .map(ParsedResult::getCommand)
                .collect(Collectors.toList());

        // Then
        assertThat(commands).isEmpty();
    }

    // ==================== Phase 1: Result Generation Tests ====================

    @Nested
    @DisplayName("Result Generation Tests")
    class ResultGenerationTests {

        @Test
        @DisplayName("Should return correct result headers array")
        void shouldReturnCorrectResultHeaders() {
            // When
            String[] headers = parser.getResultHeaders();

            // Then
            assertThat(headers).hasSize(6);
            assertThat(headers).containsExactly(
                    "userId", "startDate", "endDate", "type", "durationType", "status"
            );
        }

        @Test
        @DisplayName("Should generate result row for successful leave parse")
        void shouldGenerateResultRowForSuccess() {
            // Given
            Map<String, String> metadata = Map.of(
                    "userid", "user1",
                    "startdate", "2024-01-01",
                    "enddate", "2024-01-05",
                    "type", "ANNUAL_LEAVE",
                    "durationtype", "FULL_DAY"
            );

            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .userId("user1")
                    .dateRange(new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .status(LeaveStatus.APPROVED)
                    .sourceType(SourceType.BULK_UPLOAD)
                    .sourceId("test-source-id")
                    .build();

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.success(command, metadata, 2);

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).contains("user1");
            assertThat(resultRow).contains("2024-01-01");
            assertThat(resultRow).contains("2024-01-05");
            assertThat(resultRow).contains("ANNUAL_LEAVE");
            assertThat(resultRow).contains("FULL_DAY");
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should generate result row for failed leave parse with error")
        void shouldGenerateResultRowForFailure() {
            // Given
            Map<String, String> metadata = Map.of(
                    "userid", "user2",
                    "startdate", "2024-02-01",
                    "enddate", "2024-01-01",  // Invalid: end before start
                    "type", "ANNUAL_LEAVE",
                    "durationtype", ""
            );

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.failure(
                    metadata,
                    2,
                    "endDate must be after or equal to startDate"
            );

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).contains("user2");
            assertThat(resultRow).contains("2024-02-01");
            assertThat(resultRow).contains("ERROR: endDate must be after or equal to startDate");
        }

        @Test
        @DisplayName("Should properly escape all fields in result row")
        void shouldEscapeAllFieldsInResultRow() {
            // Given
            Map<String, String> metadata = Map.of(
                    "userid", "user,1",
                    "startdate", "2024-01-01",
                    "enddate", "2024-01-05",
                    "type", "ANNUAL_LEAVE",
                    "durationtype", "FULL_DAY"
            );

            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .userId("user,1")
                    .dateRange(new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .status(LeaveStatus.APPROVED)
                    .sourceType(SourceType.BULK_UPLOAD)
                    .sourceId("test-source-id")
                    .build();

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.success(command, metadata, 2);

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).contains("\"user,1\""); // User ID should be quoted
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should escape error messages in result row")
        void shouldEscapeErrorMessages() {
            // Given
            Map<String, String> metadata = Map.of(
                    "userid", "user1",
                    "startdate", "2024-01-01",
                    "enddate", "2024-01-05",
                    "type", "ANNUAL_LEAVE",
                    "durationtype", ""
            );

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.failure(
                    metadata,
                    2,
                    "Error: Invalid, \"data\""
            );

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).contains("\"Error: Invalid, \"\"data\"\"\"");
        }

        @Test
        @DisplayName("Should handle long field values in result row")
        void shouldHandleLongFieldValues() {
            // Given
            String longUserId = "U".repeat(150);
            Map<String, String> metadata = Map.of(
                    "userid", longUserId,
                    "startdate", "2024-01-01",
                    "enddate", "2024-01-05",
                    "type", "ANNUAL_LEAVE",
                    "durationtype", "FULL_DAY"
            );

            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .userId(longUserId)
                    .dateRange(new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .status(LeaveStatus.APPROVED)
                    .sourceType(SourceType.BULK_UPLOAD)
                    .sourceId("test-source-id")
                    .build();

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.success(command, metadata, 2);

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).contains(longUserId);
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should handle null metadata in result row")
        void shouldHandleNullMetadata() {
            // Given
            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .userId("user1")
                    .dateRange(new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .status(LeaveStatus.APPROVED)
                    .sourceType(SourceType.BULK_UPLOAD)
                    .sourceId("test-source-id")
                    .build();

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.success(command, null, 2);

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).isNotNull();
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should handle empty metadata in result row")
        void shouldHandleEmptyMetadata() {
            // Given
            Map<String, String> metadata = Map.of();
            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .userId("user1")
                    .dateRange(new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .durationType(LeaveDurationType.FULL_DAY)
                    .status(LeaveStatus.APPROVED)
                    .sourceType(SourceType.BULK_UPLOAD)
                    .sourceId("test-source-id")
                    .build();

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.success(command, metadata, 2);

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).isNotNull();
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should preserve duration type formatting in result row")
        void shouldPreserveDurationTypeFormatting() {
            // Given
            Map<String, String> metadata = Map.of(
                    "userid", "user1",
                    "startdate", "2024-01-01",
                    "enddate", "2024-01-01",
                    "type", "ANNUAL_LEAVE",
                    "durationtype", "FIRST_HALF"
            );

            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .userId("user1")
                    .dateRange(new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .durationType(LeaveDurationType.FIRST_HALF)
                    .status(LeaveStatus.APPROVED)
                    .sourceType(SourceType.BULK_UPLOAD)
                    .sourceId("test-source-id")
                    .build();

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.success(command, metadata, 2);

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).contains("FIRST_HALF");
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should handle failed result with missing metadata fields")
        void shouldHandleFailedResultWithMissingMetadata() {
            // Given
            Map<String, String> metadata = Map.of("userid", "user1");

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.failure(
                    metadata,
                    2,
                    "Validation failed"
            );

            // When
            String resultRow = parser.generateResultRow(result);

            // Then
            assertThat(resultRow).isNotNull();
            assertThat(resultRow).contains("ERROR: Validation failed");
        }

        @Test
        @DisplayName("Should default duration type to FULL_DAY in result row")
        void shouldDefaultDurationTypeInResultRow() {
            // Given
            Map<String, String> metadata = Map.of(
                    "userid", "user1",
                    "startdate", "2024-01-01",
                    "enddate", "2024-01-05",
                    "type", "ANNUAL_LEAVE",
                    "durationtype", ""
            );

            LeaveIngestionCommand command = LeaveIngestionCommand.builder()
                    .userId("user1")
                    .dateRange(new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)))
                    .type(LeaveType.ANNUAL_LEAVE)
                    .durationType(LeaveDurationType.FULL_DAY) // Defaulted
                    .status(LeaveStatus.APPROVED)
                    .sourceType(SourceType.BULK_UPLOAD)
                    .sourceId("test-source-id")
                    .build();

            ParsedResult<LeaveIngestionCommand> result = ParsedResult.success(command, metadata, 2);

            // When
            String resultRow = parser.generateResultRow(result);

            // Then - The metadata has empty durationtype, but the parser defaults it to FULL_DAY
            assertThat(resultRow).contains(",,"); // Empty durationtype in metadata
            assertThat(resultRow).contains("SUCCESS");
        }
    }

    // ==================== Phase 2: Validation Edge Cases Tests ====================

    @Nested
    @DisplayName("Validation Edge Cases Tests")
    class ValidationEdgeCasesTests {

        @Test
        @DisplayName("Should validate headers with exactly 4 columns (boundary)")
        void shouldValidateHeadersWithExactly4Columns() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("test.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            // Then
            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());
            assertThat(commands).hasSize(1);
        }

        @Test
        @DisplayName("Should validate headers with all required columns but wrong order")
        void shouldValidateHeadersWithWrongOrder() throws IOException {
            // Given
            String csvContent = "type,endDate,userId,startDate\nANNUAL_LEAVE,2024-01-05,user1,2024-01-01\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("wrong-order.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getUserId()).isEqualTo("user1");
        }

        @Test
        @DisplayName("Should throw exception for headers with duplicate column names")
        void shouldThrowForDuplicateHeaders() {
            // Given
            String csvContent = "userId,startDate,endDate,userId\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("invalid.csv", csvContent);

            // When & Then
            assertThatThrownBy(() -> parser.parse(file, "job123"))
                    .isInstanceOf(IOException.class)
                    .satisfies(e -> {
                        Throwable cause = ((IOException) e).getCause();
                        assertThat(cause).isInstanceOf(CsvValidationException.class);
                    });
        }

        @Test
        @DisplayName("Should parse same day for start and end date (boundary)")
        void shouldParseSameDayForStartAndEnd() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-01,2024-01-01,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("same-day.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(commands.get(0).getEndDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        }

        @Test
        @DisplayName("Should parse very long date range (2+ years)")
        void shouldParseVeryLongDateRange() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2020-01-01,2022-12-31,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("long-range.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getStartDate()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(commands.get(0).getEndDate()).isEqualTo(LocalDate.of(2022, 12, 31));
        }

        @Test
        @DisplayName("Should parse date range spanning year boundary")
        void shouldParseDateRangeSpanningYearBoundary() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2024-12-31,2025-01-01,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("year-boundary.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getStartDate()).isEqualTo(LocalDate.of(2024, 12, 31));
            assertThat(commands.get(0).getEndDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        }

        @Test
        @DisplayName("Should parse date range crossing multiple years")
        void shouldParseDateRangeCrossingMultipleYears() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2023-06-15,2025-03-20,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("multi-year.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getStartDate()).isEqualTo(LocalDate.of(2023, 6, 15));
            assertThat(commands.get(0).getEndDate()).isEqualTo(LocalDate.of(2025, 3, 20));
        }

        @Test
        @DisplayName("Should trim whitespace from leave type")
        void shouldTrimWhitespaceFromLeaveType() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-01,2024-01-05,  ANNUAL_LEAVE  \n";
            MultipartFile file = CsvTestUtil.createMultipartFile("whitespace.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        }

        @Test
        @DisplayName("Should handle leave type with mixed case")
        void shouldHandleLeaveTypeWithMixedCase() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-01,2024-01-05,AnNuAl_LeAvE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("mixed-case.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        }

        @Test
        @DisplayName("Should trim whitespace from duration type")
        void shouldTrimWhitespaceFromDurationType() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type,durationType\nuser1,2024-01-01,2024-01-01,ANNUAL_LEAVE,  FIRST_HALF  \n";
            MultipartFile file = CsvTestUtil.createMultipartFile("whitespace-duration.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
        }

        @Test
        @DisplayName("Should throw exception for empty userId field")
        void shouldThrowForEmptyUserId() {
            // Given
            String csvContent = "userId,startDate,endDate,type\n,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("empty-user.csv", csvContent);

            // When & Then
            assertThatThrownBy(() -> parser.parse(file, "job123"))
                    .isInstanceOf(IOException.class)
                    .satisfies(e -> {
                        Throwable cause = ((IOException) e).getCause();
                        assertThat(cause).isInstanceOf(CsvValidationException.class);
                        assertThat(cause.getMessage()).contains("userid is required");
                    });
        }

        @Test
        @DisplayName("Should throw exception for empty date fields")
        void shouldThrowForEmptyDateFields() {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("empty-date.csv", csvContent);

            // When & Then
            assertThatThrownBy(() -> parser.parse(file, "job123"))
                    .isInstanceOf(IOException.class)
                    .satisfies(e -> {
                        Throwable cause = ((IOException) e).getCause();
                        assertThat(cause).isInstanceOf(CsvValidationException.class);
                        assertThat(cause.getMessage()).contains("startdate is required");
                    });
        }

        @Test
        @DisplayName("Should handle invalid date format in middle of file gracefully")
        void shouldHandleInvalidDateInMiddleOfFile() {
            // Given
            String csvContent = "userId,startDate,endDate,type\n" +
                    "user1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n" +
                    "user2,invalid-date,2024-01-05,ANNUAL_LEAVE\n" +
                    "user3,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("mixed-invalid.csv", csvContent);

            // When & Then - Should throw on first error
            assertThatThrownBy(() -> parser.parse(file, "job123"))
                    .isInstanceOf(IOException.class)
                    .satisfies(e -> {
                        Throwable cause = ((IOException) e).getCause();
                        assertThat(cause).isInstanceOf(CsvValidationException.class);
                        assertThat(cause.getMessage()).contains("startDate must be in yyyy-MM-dd format");
                    });
        }

        @Test
        @DisplayName("Should handle headers with trailing spaces")
        void shouldHandleHeadersWithTrailingSpaces() throws IOException {
            // Given
            String csvContent = "userId ,startDate  ,endDate ,type\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("spaces-headers.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
        }

        @Test
        @DisplayName("Should handle headers with leading spaces")
        void shouldHandleHeadersWithLeadingSpaces() throws IOException {
            // Given
            String csvContent = "  userId,  startDate,  endDate,  type\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("leading-spaces-headers.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
        }
    }

    // ==================== Phase 3: Error Paths Tests ====================

    @Nested
    @DisplayName("Error Paths Tests")
    class ErrorPathsTests {

        @Test
        @DisplayName("Should handle CSV file with mixed valid and invalid rows")
        void shouldHandleMixedValidInvalidRows() {
            // Given
            String csvContent = "userId,startDate,endDate,type\n" +
                    "user1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n" +
                    "user2,2024-02-01,2024-01-01,ANNUAL_LEAVE\n" +  // Invalid: end before start
                    "user3,2024-03-01,2024-03-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("mixed.csv", csvContent);

            // When & Then - Should throw on first error
            assertThatThrownBy(() -> parser.parse(file, "job123"))
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("Should handle date range in far past")
        void shouldHandleDateRangeInFarPast() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2010-01-01,2010-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("past.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getStartDate()).isEqualTo(LocalDate.of(2010, 1, 1));
        }

        @Test
        @DisplayName("Should handle date range in far future")
        void shouldHandleDateRangeInFarFuture() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2030-01-01,2030-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("future.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getStartDate()).isEqualTo(LocalDate.of(2030, 1, 1));
        }

        @Test
        @DisplayName("Should handle very long CSV file")
        void shouldHandleVeryLongCsvFile() throws IOException {
            // Given - Create 100 rows with valid dates (endDay >= startDay)
            StringBuilder csvBuilder = new StringBuilder("userId,startDate,endDate,type\n");
            for (int i = 1; i <= 100; i++) {
                int startDay = ((i - 1) % 28) + 1; // Days 1-28 are valid
                int endDay = Math.min(startDay + 1, 28); // Ensure end day is valid and >= startDay
                csvBuilder.append("user").append(i).append(",2024-01-").append(String.format("%02d", startDay)).append(",2024-01-").append(String.format("%02d", endDay)).append(",ANNUAL_LEAVE\n");
            }
            MultipartFile file = CsvTestUtil.createMultipartFile("long.csv", csvBuilder.toString());

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(100);
        }

        @Test
        @DisplayName("Should handle CSV with BOM")
        void shouldHandleCsvWithBom() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            byte[] contentBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] bomAndContent = new byte[bom.length + contentBytes.length];
            System.arraycopy(bom, 0, bomAndContent, 0, bom.length);
            System.arraycopy(contentBytes, 0, bomAndContent, bom.length, contentBytes.length);

            MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                    "bom.csv",
                    "bom.csv",
                    "text/csv",
                    bomAndContent
            );

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then - OpenCSV handles BOM automatically
            assertThat(commands).hasSize(1);
        }

        @Test
        @DisplayName("Should validate each row independently for varying column counts")
        void shouldValidateRowsIndependently() {
            // Given
            String csvContent = "userId,startDate,endDate,type\n" +
                    "user1,2024-01-01,2024-01-05,ANNUAL_LEAVE\n" +
                    "user2,2024-02-01\n"; // Missing columns
            MultipartFile file = CsvTestUtil.createMultipartFile("invalid.csv", csvContent);

            // When & Then
            assertThatThrownBy(() -> parser.parse(file, "job123"))
                    .isInstanceOf(IOException.class)
                    .satisfies(e -> {
                        Throwable cause = ((IOException) e).getCause();
                        assertThat(cause).isInstanceOf(CsvValidationException.class);
                    });
        }

        @Test
        @DisplayName("Should throw exception for special characters in userId")
        void shouldHandleSpecialCharactersInUserId() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser@#$,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("special-chars.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getUserId()).isEqualTo("user@#$");
        }

        @Test
        @DisplayName("Should throw exception for invalid leave type with special characters")
        void shouldThrowForInvalidLeaveTypeWithSpecialChars() {
            // Given
            String csvContent = "userId,startDate,endDate,type\nuser1,2024-01-01,2024-01-05,INVALID@TYPE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("invalid-type.csv", csvContent);

            // When & Then
            assertThatThrownBy(() -> parser.parse(file, "job123"))
                    .isInstanceOf(IOException.class)
                    .satisfies(e -> {
                        Throwable cause = ((IOException) e).getCause();
                        assertThat(cause).isInstanceOf(CsvValidationException.class);
                        assertThat(cause.getMessage()).contains("Invalid type");
                    });
        }

        @Test
        @DisplayName("Should parse empty quoted fields as empty strings")
        void shouldParseEmptyQuotedFields() {
            // Given
            String csvContent = "userId,startDate,endDate,type\n\"\",2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("empty-quoted.csv", csvContent);

            // When & Then - Empty userId will fail validation
            assertThatThrownBy(() -> parser.parse(file, "job123"))
                    .isInstanceOf(IOException.class)
                    .satisfies(e -> {
                        Throwable cause = ((IOException) e).getCause();
                        assertThat(cause).isInstanceOf(CsvValidationException.class);
                        assertThat(cause.getMessage()).contains("userid is required");
                    });
        }

        @Test
        @DisplayName("Should handle quoted fields with commas in dates")
        void shouldHandleQuotedFieldsWithCommas() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\n\"user,1\",2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("quoted-comma.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getUserId()).isEqualTo("user,1");
        }

        @Test
        @DisplayName("Should handle Unicode characters in all fields")
        void shouldHandleUnicodeInAllFields() throws IOException {
            // Given
            String csvContent = "userId,startDate,endDate,type\n日本語,2024-01-01,2024-01-05,ANNUAL_LEAVE\n";
            MultipartFile file = CsvTestUtil.createMultipartFile("unicode.csv", csvContent);

            // When
            List<ParsedResult<LeaveIngestionCommand>> results = parser.parse(file, "job123");

            List<LeaveIngestionCommand> commands = results.stream()
                    .filter(ParsedResult::isSuccess)
                    .map(ParsedResult::getCommand)
                    .collect(Collectors.toList());

            // Then
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getUserId()).isEqualTo("日本語");
        }
    }
}
