package one.june.leave_management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.june.leave_management.adapter.inbound.web.dto.BulkUploadResponse;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository;
import one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadRecordRepository;
import one.june.leave_management.test.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Bulk Upload endpoints using MockMvc.
 * Tests CSV file upload, status tracking, and result file download.
 */
@IntegrationTest(transactional = true)
@DisplayName("Bulk Upload Controller Integration Tests")
class BulkUploadControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BulkUploadJobRepository bulkUploadJobRepository;

    @Autowired
    private BulkUploadRecordRepository bulkUploadRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Test employee IDs
    private static final String USER1_ID = "123e4567-e89b-12d3-a456-426614174300";
    private static final String USER1_SLACK_ID = "U301";

    @BeforeTransaction
    void setUpTestData() {
        // Clean up and create test employee
        jdbcTemplate.update("DELETE FROM employee WHERE id = ?", USER1_ID);
        jdbcTemplate.update(
                "INSERT INTO employee (id, name, slack_id, date_of_joining, active, region, created_at, updated_at) " +
                "VALUES (?, 'Bulk Upload User 1', ?, '2020-01-01', true, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER1_ID, USER1_SLACK_ID
        );
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up any existing data
        bulkUploadRecordRepository.deleteAll();
        bulkUploadJobRepository.deleteAll();
    }

    @Test
    @DisplayName("Should accept CSV file for bulk upload")
    void shouldAcceptCsvFileForBulkUpload() throws Exception {
        // Given
        String csvContent = "userId,startDate,endDate,type,durationType\n" +
                "U301,2024-01-01,2024-01-05,ANNUAL_LEAVE,FULL_DAY";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-upload.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/leaves/bulk-upload")
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
        mockMvc.perform(multipart("/api/leaves/bulk-upload")
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
                "userId,startDate\nU301,2024-01-01".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/leaves/bulk-upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for file exceeding size limit")
    void shouldReturn400ForFileExceedingSizeLimit() throws Exception {
        // Given - Create file larger than 10MB
        StringBuilder csvContent = new StringBuilder("userId,startDate,endDate,type,durationType\n");
        for (int i = 0; i < 250000; i++) {
            csvContent.append("user").append(i).append(",2024-01-01,2024-01-05,ANNUAL_LEAVE,FULL_DAY\n");
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.csv",
                "text/csv",
                csvContent.toString().getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/leaves/bulk-upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get bulk upload job status")
    void shouldGetBulkUploadJobStatus() throws Exception {
        // Given - First create a job
        String csvContent = "userId,startDate,endDate,type,durationType\n" +
                "U301,2024-01-01,2024-01-05,ANNUAL_LEAVE,FULL_DAY";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "status-test.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        String response = mockMvc.perform(multipart("/api/leaves/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract jobId from response
        UUID jobId = objectMapper.readValue(response, BulkUploadResponse.class).getJobId();

        // When & Then
        mockMvc.perform(get("/api/leaves/bulk-upload/status/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("Should return 400 for invalid UUID format when getting status")
    void shouldReturn400ForInvalidUuidFormatWhenGettingStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/leaves/bulk-upload/status/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Should download result file for completed job")
    void shouldDownloadResultFileForCompletedJob() throws Exception {
        // Given - Ensure employee exists (NOT_SUPPORTED test needs explicit setup)
        jdbcTemplate.update("DELETE FROM employee WHERE id = ?", USER1_ID);
        jdbcTemplate.update(
                "INSERT INTO employee (id, name, slack_id, date_of_joining, active, region, created_at, updated_at) " +
                "VALUES (?, 'Bulk Upload User 1', ?, '2020-01-01', true, 'PUNE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER1_ID, USER1_SLACK_ID
        );

        String csvContent = "userId,startDate,endDate,type,durationType\n" +
                "U301,2024-01-01,2024-01-05,ANNUAL_LEAVE,FULL_DAY";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "download-test.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        String response = mockMvc.perform(multipart("/api/leaves/bulk-upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID jobId = objectMapper.readValue(response, BulkUploadResponse.class).getJobId();

        // Wait for async processing to complete - shorter timeout for test
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .ignoreException(RuntimeException.class)
                .until(() -> {
                    try {
                        String statusResponse = mockMvc.perform(get("/api/leaves/bulk-upload/status/{jobId}", jobId))
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
        mockMvc.perform(get("/api/leaves/bulk-download/{jobId}", jobId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return error when downloading result for non-existent job")
    void shouldReturnErrorWhenDownloadingResultForNonExistentJob() throws Exception {
        // Given
        UUID nonExistentJobId = UUID.randomUUID();

        // When & Then - Accept either 404 or 400 (global exception handler might convert it)
        mockMvc.perform(get("/api/leaves/bulk-download/{jobId}", nonExistentJobId))
                .andExpect(status().is(in(new Integer[]{400, 404})));
    }
}
