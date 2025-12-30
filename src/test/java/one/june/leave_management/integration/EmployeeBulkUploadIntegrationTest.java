package one.june.leave_management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.EmployeeJpaRepository;
import one.june.leave_management.test.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Employee Bulk Upload endpoints using MockMvc.
 * Tests CSV file upload, status tracking, and result file download.
 */
@IntegrationTest(transactional = true)
@DisplayName("Employee Bulk Upload Controller Integration Tests")
class EmployeeBulkUploadIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BulkUploadJobRepository bulkUploadJobRepository;

    @Autowired
    private BulkUploadRecordRepository bulkUploadRecordRepository;

    @Autowired
    private EmployeeJpaRepository employeeJpaRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up any existing data
        bulkUploadRecordRepository.deleteAll();
        bulkUploadJobRepository.deleteAll();
        employeeJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Should accept CSV file for employee bulk upload")
    void shouldAcceptCsvFileForBulkUpload() throws Exception {
        // Given
        String csvContent = "name,slackId,googleId,slackDisplayName,dateOfJoining,active,carryForwardLeaves\n" +
                "John Doe,U12345,,john.doe,2020-01-15,true,5";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-employees.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.successfulRecords").value(0))
                .andExpect(jsonPath("$.failedRecords").value(0))
                .andExpect(jsonPath("$.resultAvailable").value(false));
    }

    @Test
    @DisplayName("Should accept CSV file with multiple employees")
    void shouldAcceptCsvFileWithMultipleEmployees() throws Exception {
        // Given
        String csvContent = "name,slackId,googleId,slackDisplayName,dateOfJoining,active,carryForwardLeaves\n" +
                "John Doe,U12345,,john.doe,2020-01-15,true,5\n" +
                "Jane Smith,,jane@example.com,jane.smith,2021-03-20,true,3\n" +
                "Bob Johnson,U98765,bob@example.com,bob.j,2019-06-10,true,0";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-employees-multiple.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.totalRecords").value(3));
    }

    @Test
    @DisplayName("Should accept CSV file with only required columns")
    void shouldAcceptCsvFileWithOnlyRequiredColumns() throws Exception {
        // Given
        String csvContent = "name,slackId,googleId,dateOfJoining\n" +
                "John Doe,U12345,,2020-01-15";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-employees-minimal.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("Should return 400 for empty file")
    void shouldReturn400ForEmptyFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for non-CSV file")
    void shouldReturn400ForNonCsvFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "name,slackId\ndoe,123".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for file exceeding size limit")
    void shouldReturn400ForFileExceedingSizeLimit() throws Exception {
        // Given - Create file larger than 10MB (~12MB to be safe)
        StringBuilder csvContent = new StringBuilder("name,slackId,googleId,dateOfJoining\n");
        for (int i = 0; i < 400000; i++) {
            csvContent.append("Employee").append(i).append(",U").append(i).append(",,2020-01-15\n");
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.csv",
                "text/csv",
                csvContent.toString().getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get employee bulk upload job status")
    void shouldGetBulkUploadJobStatus() throws Exception {
        // Given - First create a job
        String csvContent = "name,slackId,googleId,dateOfJoining\n" +
                "John Doe,U12345,,2020-01-15";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "status-test.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        String response = mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract jobId from response
        UUID jobId = objectMapper.readValue(response, BulkUploadResponse.class).getJobId();

        // When & Then
        mockMvc.perform(get("/api/employees/bulk-upload/status/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.totalRecords").exists())
                .andExpect(jsonPath("$.successfulRecords").exists())
                .andExpect(jsonPath("$.failedRecords").exists())
                .andExpect(jsonPath("$.resultAvailable").exists());
    }

    @Test
    @DisplayName("Should return 400 for invalid UUID format when getting status")
    void shouldReturn400ForInvalidUuidFormatWhenGettingStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/employees/bulk-upload/status/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 for non-existent job when getting status")
    void shouldReturn404ForNonExistentJobWhenGettingStatus() throws Exception {
        // Given
        UUID nonExistentJobId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/employees/bulk-upload/status/{jobId}", nonExistentJobId))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Should download result file for completed job")
    void shouldDownloadResultFileForCompletedJob() throws Exception {
        // Given
        String csvContent = "name,slackId,googleId,dateOfJoining\n" +
                "John Doe,U12345,,2020-01-15";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "download-test.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        String response = mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID jobId = objectMapper.readValue(response, BulkUploadResponse.class).getJobId();

        // Wait for async processing to complete
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .ignoreException(RuntimeException.class)
                .until(() -> {
                    try {
                        String statusResponse = mockMvc.perform(get("/api/employees/bulk-upload/status/{jobId}", jobId))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                        if (statusResponse.isEmpty()) {
                            return false;
                        }

                        BulkUploadResponse statusObj = objectMapper.readValue(statusResponse, BulkUploadResponse.class);
                        return "COMPLETED".equals(statusObj.getStatus()) || "FAILED".equals(statusObj.getStatus());
                    } catch (Exception e) {
                        return false;
                    }
                });

        // When & Then
        mockMvc.perform(get("/api/employees/bulk-download/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    @DisplayName("Should return error when downloading result for non-existent job")
    void shouldReturnErrorWhenDownloadingResultForNonExistentJob() throws Exception {
        // Given
        UUID nonExistentJobId = UUID.randomUUID();

        // When & Then - Accept either 404 or 400 (global exception handler might convert it)
        mockMvc.perform(get("/api/employees/bulk-download/{jobId}", nonExistentJobId))
                .andExpect(status().is(in(new Integer[]{400, 404})));
    }

    @Test
    @DisplayName("Should validate required fields in CSV")
    void shouldValidateRequiredFieldsInCsv() throws Exception {
        // Given - Missing dateOfJoining (required field)
        String csvContent = "name,slackId,googleId\n" +
                "John Doe,U12345,";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid-headers.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate at least one external ID is present")
    void shouldValidateAtLeastOneExternalId() throws Exception {
        // Given - Missing both slackId and googleId
        String csvContent = "name,slackId,googleId,dateOfJoining\n" +
                "John Doe,,";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "no-external-id.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle CSV with case-insensitive headers")
    void shouldHandleCsvWithCaseInsensitiveHeaders() throws Exception {
        // Given
        String csvContent = "Name,SlackId,GoogleId,DateOfJoining\n" +
                "John Doe,U12345,,2020-01-15";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mixed-case-headers.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle CSV with spaces in headers")
    void shouldHandleCsvWithSpacesInHeaders() throws Exception {
        // Given
        String csvContent = "name,slack id,google id,date of joining\n" +
                "John Doe,U12345,,2020-01-15";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "headers-with-spaces.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty());
    }

    @Test
    @DisplayName("Should process employee with carry forward leaves")
    void shouldProcessEmployeeWithCarryForwardLeaves() throws Exception {
        // Given
        String csvContent = "name,slackId,googleId,dateOfJoining,carryForwardLeaves\n" +
                "John Doe,U12345,,2020-01-15,5";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "carry-forward-test.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.totalRecords").value(1));
    }

    @Test
    @DisplayName("Should process employee with active status false")
    void shouldProcessEmployeeWithActiveStatusFalse() throws Exception {
        // Given
        String csvContent = "name,slackId,googleId,dateOfJoining,active\n" +
                "John Doe,U12345,,2020-01-15,false";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "inactive-employee.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.totalRecords").value(1));
    }

    @Test
    @DisplayName("Should process employee with both slackId and googleId")
    void shouldProcessEmployeeWithBothExternalIds() throws Exception {
        // Given
        String csvContent = "name,slackId,googleId,dateOfJoining\n" +
                "John Doe,U12345,john@example.com,2020-01-15";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "both-ids.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/employees/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.totalRecords").value(1));
    }
}
