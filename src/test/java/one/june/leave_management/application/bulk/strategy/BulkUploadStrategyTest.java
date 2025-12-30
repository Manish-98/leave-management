package one.june.leave_management.application.bulk.strategy;

import one.june.leave_management.domain.leave.model.BulkUploadRecord;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import one.june.leave_management.test.builder.LeaveTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BulkUploadStrategy implementations.
 * Tests result row generation and metadata population for bulk uploads.
 */
@DisplayName("BulkUploadStrategy Tests")
class BulkUploadStrategyTest {

    private EmployeeBulkUploadStrategy employeeStrategy;
    private LeaveBulkUploadStrategy leaveStrategy;

    @BeforeEach
    void setUp() {
        employeeStrategy = new EmployeeBulkUploadStrategy();
        leaveStrategy = new LeaveBulkUploadStrategy();
    }

    // ==================== EmployeeBulkUploadStrategy Tests ====================

    @Nested
    @DisplayName("EmployeeBulkUploadStrategy Tests")
    class EmployeeBulkUploadStrategyTests {

        @Test
        @DisplayName("Should return EMPLOYEE type")
        void shouldReturnEmployeeType() {
            // When
            BulkUploadType type = employeeStrategy.getType();

            // Then
            assertThat(type).isEqualTo(BulkUploadType.EMPLOYEE);
        }

        @Test
        @DisplayName("Should generate result row for successful employee record")
        void shouldGenerateResultRowForSuccessRecord() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("name", "John Doe");
            metadata.put("dateofjoining", "2020-01-01");
            metadata.put("slackid", "U12345");
            metadata.put("googleid", "john@example.com");
            metadata.put("slackdisplayname", "John");

            BulkUploadRecord record = LeaveTestDataBuilder.successRecord(1)
                    .metadata(metadata)
                    .build();

            // When
            String resultRow = employeeStrategy.generateResultRow(record);

            // Then
            assertThat(resultRow).contains("John Doe");
            assertThat(resultRow).contains("2020-01-01");
            assertThat(resultRow).contains("U12345");
            assertThat(resultRow).contains("john@example.com");
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should generate result row for failed employee record")
        void shouldGenerateResultRowForFailedRecord() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("name", "Jane Smith");
            metadata.put("dateofjoining", "2021-06-15");
            metadata.put("slackid", "U67890");
            metadata.put("googleid", "");
            metadata.put("slackdisplayname", "Jane");

            BulkUploadRecord record = LeaveTestDataBuilder.errorRecord(2, "Duplicate Slack ID")
                    .metadata(metadata)
                    .build();

            // When
            String resultRow = employeeStrategy.generateResultRow(record);

            // Then
            assertThat(resultRow).contains("Jane Smith");
            assertThat(resultRow).contains("ERROR: Duplicate Slack ID");
        }

        @Test
        @DisplayName("Should handle null optional fields with defaults")
        void shouldHandleNullOptionalFieldsWithDefaults() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("name", "Test User");
            metadata.put("dateofjoining", "2020-01-01");
            metadata.put("slackid", "U11111");
            metadata.put("googleid", "");
            metadata.put("slackdisplayname", "");
            // active and carryForwardLeaves not provided

            BulkUploadRecord record = LeaveTestDataBuilder.successRecord(1)
                    .metadata(metadata)
                    .build();

            // When
            String resultRow = employeeStrategy.generateResultRow(record);

            // Then
            assertThat(resultRow).contains("true"); // Default active
            assertThat(resultRow).contains("0");    // Default carryForwardLeaves
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should handle empty metadata gracefully")
        void shouldHandleEmptyMetadata() {
            // Given
            Map<String, String> metadata = new HashMap<>();

            BulkUploadRecord record = LeaveTestDataBuilder.successRecord(1)
                    .metadata(metadata)
                    .build();

            // When
            String resultRow = employeeStrategy.generateResultRow(record);

            // Then
            assertThat(resultRow).isNotNull();
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should return correct result headers")
        void shouldReturnCorrectResultHeaders() {
            // When
            String[] headers = employeeStrategy.getResultHeaders();

            // Then
            assertThat(headers).hasSize(8);
            assertThat(headers).containsExactly(
                    "name", "dateOfJoining", "slackId", "googleId",
                    "slackDisplayName", "active", "carryForwardLeaves", "status"
            );
        }

        @Test
        @DisplayName("Should populate metadata from command with all fields")
        void shouldPopulateMetadataFromCommand() {
            // Given - Set carry forward leaves for current year
            int currentYear = java.time.LocalDate.now().getYear();
            Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
            carryForwardLeaves.put(currentYear, 5);

            // When
            Map<String, String> metadata = employeeStrategy.populateMetadataFromCommand(
                    "John Doe",
                    "2020-01-01",
                    "U12345",
                    "john@example.com",
                    "John",
                    true,
                    carryForwardLeaves
            );

            // Then
            assertThat(metadata).hasSize(7);
            assertThat(metadata.get("name")).isEqualTo("John Doe");
            assertThat(metadata.get("dateofjoining")).isEqualTo("2020-01-01");
            assertThat(metadata.get("slackid")).isEqualTo("U12345");
            assertThat(metadata.get("googleid")).isEqualTo("john@example.com");
            assertThat(metadata.get("slackdisplayname")).isEqualTo("John");
            assertThat(metadata.get("active")).isEqualTo("true");
            assertThat(metadata.get("carryforwardleaves")).isEqualTo("5"); // Only current year
        }

        @Test
        @DisplayName("Should populate metadata with null values handled")
        void shouldPopulateMetadataWithNullValues() {
            // When
            Map<String, String> metadata = employeeStrategy.populateMetadataFromCommand(
                    "Jane Smith",
                    "2021-06-15",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Then
            assertThat(metadata.get("name")).isEqualTo("Jane Smith");
            assertThat(metadata.get("slackid")).isEmpty();
            assertThat(metadata.get("googleid")).isEmpty();
            assertThat(metadata.get("slackdisplayname")).isEmpty();
        }
    }

    // ==================== LeaveBulkUploadStrategy Tests ====================

    @Nested
    @DisplayName("LeaveBulkUploadStrategy Tests")
    class LeaveBulkUploadStrategyTests {

        @Test
        @DisplayName("Should return LEAVE type")
        void shouldReturnLeaveType() {
            // When
            BulkUploadType type = leaveStrategy.getType();

            // Then
            assertThat(type).isEqualTo(BulkUploadType.LEAVE);
        }

        @Test
        @DisplayName("Should generate result row for successful leave record")
        void shouldGenerateResultRowForSuccessRecord() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userid", "user123");
            metadata.put("startdate", "2024-01-01");
            metadata.put("enddate", "2024-01-05");
            metadata.put("type", "ANNUAL_LEAVE");
            metadata.put("durationtype", "FULL_DAY");

            BulkUploadRecord record = LeaveTestDataBuilder.successRecord(1)
                    .metadata(metadata)
                    .build();

            // When
            String resultRow = leaveStrategy.generateResultRow(record);

            // Then
            assertThat(resultRow).contains("user123");
            assertThat(resultRow).contains("2024-01-01");
            assertThat(resultRow).contains("2024-01-05");
            assertThat(resultRow).contains("ANNUAL_LEAVE");
            assertThat(resultRow).contains("FULL_DAY");
            assertThat(resultRow).contains("SUCCESS");
        }

        @Test
        @DisplayName("Should generate result row for failed leave record")
        void shouldGenerateResultRowForFailedRecord() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userid", "user456");
            metadata.put("startdate", "2024-02-01");
            metadata.put("enddate", "2024-02-05");
            metadata.put("type", "SICK_LEAVE");
            metadata.put("durationtype", "");

            BulkUploadRecord record = LeaveTestDataBuilder.errorRecord(2, "Employee not found")
                    .metadata(metadata)
                    .build();

            // When
            String resultRow = leaveStrategy.generateResultRow(record);

            // Then
            assertThat(resultRow).contains("user456");
            assertThat(resultRow).contains("ERROR: Employee not found");
        }

        @Test
        @DisplayName("Should default duration type to FULL_DAY when not provided")
        void shouldDefaultDurationType() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userid", "user789");
            metadata.put("startdate", "2024-03-01");
            metadata.put("enddate", "2024-03-05");
            metadata.put("type", "ANNUAL_LEAVE");
            // durationtype not provided

            BulkUploadRecord record = LeaveTestDataBuilder.successRecord(1)
                    .metadata(metadata)
                    .build();

            // When
            String resultRow = leaveStrategy.generateResultRow(record);

            // Then
            assertThat(resultRow).contains("FULL_DAY");
        }

        @Test
        @DisplayName("Should return correct result headers for leaves")
        void shouldReturnCorrectResultHeadersForLeaves() {
            // When
            String[] headers = leaveStrategy.getResultHeaders();

            // Then
            assertThat(headers).hasSize(6);
            assertThat(headers).containsExactly(
                    "userId", "startDate", "endDate", "type", "durationType", "status"
            );
        }

        @Test
        @DisplayName("Should populate metadata from leave command")
        void shouldPopulateMetadataFromLeaveCommand() {
            // When
            Map<String, String> metadata = leaveStrategy.populateMetadataFromCommand(
                    "user123",
                    "2024-01-01",
                    "2024-01-05",
                    "ANNUAL_LEAVE",
                    "FULL_DAY"
            );

            // Then
            assertThat(metadata).hasSize(5);
            assertThat(metadata.get("userid")).isEqualTo("user123");
            assertThat(metadata.get("startdate")).isEqualTo("2024-01-01");
            assertThat(metadata.get("enddate")).isEqualTo("2024-01-05");
            assertThat(metadata.get("type")).isEqualTo("ANNUAL_LEAVE");
            assertThat(metadata.get("durationtype")).isEqualTo("FULL_DAY");
        }

        @Test
        @DisplayName("Should populate metadata with null duration type defaulted")
        void shouldPopulateMetadataWithNullDurationType() {
            // When
            Map<String, String> metadata = leaveStrategy.populateMetadataFromCommand(
                    "user456",
                    "2024-02-01",
                    "2024-02-05",
                    "SICK_LEAVE",
                    null
            );

            // Then
            assertThat(metadata.get("durationtype")).isEqualTo("FULL_DAY");
        }
    }
}
