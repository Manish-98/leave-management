package one.june.leave_management.application.audit.service;

import one.june.leave_management.adapter.persistence.jpa.entity.AuditLogJpaEntity;
import one.june.leave_management.adapter.persistence.jpa.repository.AuditLogJpaRepository;
import one.june.leave_management.domain.audit.model.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Service Unit Tests")
class AuditServiceTest {

    @Mock
    private AuditLogJpaRepository auditLogJpaRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogJpaRepository);
    }

    @Nested
    @DisplayName("Save Audit Log Tests")
    class SaveAuditLogTests {

        @Test
        @DisplayName("Should save audit log successfully")
        void shouldSaveAuditLogSuccessfully() {
            // Given
            UUID auditLogId = UUID.randomUUID();
            String requestId = "test-request-123";
            String endpoint = "/api/leaves/ingest";
            String httpMethod = "POST";
            String sourceType = "WEB";
            Map<String, Object> requestBody = Map.of(
                    "userId", "user-123",
                    "type", "ANNUAL_LEAVE"
            );
            Integer responseStatus = 201;
            Map<String, Object> responseBody = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "status", "CREATED"
            );
            String userId = "user-123";
            Long executionTimeMs = 150L;
            LocalDateTime timestamp = LocalDateTime.now();

            AuditLog auditLog = AuditLog.builder()
                    .id(auditLogId)
                    .requestId(requestId)
                    .endpoint(endpoint)
                    .httpMethod(httpMethod)
                    .sourceType(sourceType)
                    .requestBody(requestBody)
                    .responseStatus(responseStatus)
                    .responseBody(responseBody)
                    .userId(userId)
                    .executionTimeMs(executionTimeMs)
                    .timestamp(timestamp)
                    .build();

            when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class))).thenReturn(null);

            // When
            auditService.saveAuditLog(auditLog);

            // Then
            ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
            verify(auditLogJpaRepository).save(captor.capture());

            AuditLogJpaEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getId()).isEqualTo(auditLogId);
            assertThat(savedEntity.getRequestId()).isEqualTo(requestId);
            assertThat(savedEntity.getEndpoint()).isEqualTo(endpoint);
            assertThat(savedEntity.getHttpMethod()).isEqualTo(httpMethod);
            assertThat(savedEntity.getSourceType()).isEqualTo(sourceType);
            assertThat(savedEntity.getResponseStatus()).isEqualTo(responseStatus);
            assertThat(savedEntity.getUserId()).isEqualTo(userId);
            assertThat(savedEntity.getExecutionTimeMs()).isEqualTo(executionTimeMs);
            assertThat(savedEntity.getTimestamp()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("Should save audit log with error message")
        void shouldSaveAuditLogWithErrorMessage() {
            // Given
            UUID auditLogId = UUID.randomUUID();
            String errorMessage = "Validation failed: Invalid date range";

            AuditLog auditLog = AuditLog.builder()
                    .id(auditLogId)
                    .requestId("req-1")
                    .endpoint("/api/leaves/ingest")
                    .httpMethod("POST")
                    .sourceType("SLACK")
                    .responseStatus(400)
                    .errorMessage(errorMessage)
                    .userId("user-123")
                    .executionTimeMs(50L)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class))).thenReturn(null);

            // When
            auditService.saveAuditLog(auditLog);

            // Then
            ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
            verify(auditLogJpaRepository).save(captor.capture());

            AuditLogJpaEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(savedEntity.getResponseStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("Should use current timestamp when timestamp is null")
        void shouldUseCurrentTimestampWhenNull() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .requestId("req-2")
                    .endpoint("/api/leaves")
                    .httpMethod("GET")
                    .responseStatus(200)
                    .timestamp(null)
                    .build();

            when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class))).thenReturn(null);

            // When
            auditService.saveAuditLog(auditLog);

            // Then
            ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
            verify(auditLogJpaRepository).save(captor.capture());

            AuditLogJpaEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getTimestamp()).isNotNull();
            assertThat(savedEntity.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        }

        @Test
        @DisplayName("Should handle null request body")
        void shouldHandleNullRequestBody() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .requestId("req-3")
                    .endpoint("/api/leaves")
                    .httpMethod("GET")
                    .requestBody(null)
                    .responseStatus(200)
                    .build();

            when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class))).thenReturn(null);

            // When
            auditService.saveAuditLog(auditLog);

            // Then
            ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
            verify(auditLogJpaRepository).save(captor.capture());

            AuditLogJpaEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getRequestBody()).isNull();
        }

        @Test
        @DisplayName("Should handle null response body")
        void shouldHandleNullResponseBody() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .requestId("req-4")
                    .endpoint("/api/leaves")
                    .httpMethod("DELETE")
                    .responseStatus(204)
                    .responseBody(null)
                    .build();

            when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class))).thenReturn(null);

            // When
            auditService.saveAuditLog(auditLog);

            // Then
            ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
            verify(auditLogJpaRepository).save(captor.capture());

            AuditLogJpaEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getResponseBody()).isNull();
        }

        @Test
        @DisplayName("Should handle audit log save failure gracefully")
        void shouldHandleAuditLogSaveFailureGracefully() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .requestId("req-5")
                    .endpoint("/api/leaves/ingest")
                    .httpMethod("POST")
                    .responseStatus(201)
                    .build();

            when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then - Should not throw exception
            auditService.saveAuditLog(auditLog);

            verify(auditLogJpaRepository).save(any(AuditLogJpaEntity.class));
        }

        @Test
        @DisplayName("Should serialize complex request body to JSON")
        void shouldSerializeComplexRequestBody() {
            // Given
            Map<String, Object> complexBody = new HashMap<>();
            complexBody.put("userId", "user-123");
            complexBody.put("dates", Map.of("start", "2024-01-01", "end", "2024-01-05"));
            complexBody.put("metadata", Map.of("source", "WEB", "version", "1.0"));

            AuditLog auditLog = AuditLog.builder()
                    .requestId("req-6")
                    .endpoint("/api/leaves/ingest")
                    .httpMethod("POST")
                    .requestBody(complexBody)
                    .responseStatus(201)
                    .build();

            when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class))).thenReturn(null);

            // When
            auditService.saveAuditLog(auditLog);

            // Then
            ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
            verify(auditLogJpaRepository).save(captor.capture());

            AuditLogJpaEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getRequestBody()).isNotNull();
            assertThat(savedEntity.getRequestBody()).contains("\"userId\":\"user-123\"");
        }

        @Test
        @DisplayName("Should handle different source types")
        void shouldHandleDifferentSourceTypes() {
            // Given - Test with SLACK source
            AuditLog slackAuditLog = AuditLog.builder()
                    .requestId("req-7")
                    .endpoint("/integrations/slack/command")
                    .httpMethod("POST")
                    .sourceType("SLACK")
                    .responseStatus(200)
                    .build();

            when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class))).thenReturn(null);

            // When
            auditService.saveAuditLog(slackAuditLog);

            // Then
            ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
            verify(auditLogJpaRepository).save(captor.capture());

            AuditLogJpaEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getSourceType()).isEqualTo("SLACK");

            // Given - Test with WEB source
            AuditLog webAuditLog = AuditLog.builder()
                    .requestId("req-8")
                    .endpoint("/api/leaves")
                    .httpMethod("GET")
                    .sourceType("WEB")
                    .responseStatus(200)
                    .build();

            // When
            auditService.saveAuditLog(webAuditLog);

            // Then
            verify(auditLogJpaRepository, times(2)).save(any(AuditLogJpaEntity.class));
        }

        @Test
        @DisplayName("Should handle different HTTP methods")
        void shouldHandleDifferentHttpMethods() {
            // Given
            String[] httpMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};

            for (String method : httpMethods) {
                AuditLog auditLog = AuditLog.builder()
                        .requestId("req-" + method)
                        .endpoint("/api/leaves")
                        .httpMethod(method)
                        .responseStatus(200)
                        .build();

                when(auditLogJpaRepository.save(any(AuditLogJpaEntity.class))).thenReturn(null);

                // When
                auditService.saveAuditLog(auditLog);
            }

            // Then
            verify(auditLogJpaRepository, times(httpMethods.length)).save(any(AuditLogJpaEntity.class));
        }
    }
}
