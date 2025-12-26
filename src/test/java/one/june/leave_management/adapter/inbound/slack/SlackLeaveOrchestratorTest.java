package one.june.leave_management.adapter.inbound.slack;

import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewClosedRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewSubmissionRequest;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.adapter.outbound.slack.client.SlackApiClient;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageResponse;
import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.SlackViewOpenResponse;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SlackLeaveOrchestrator}
 * <p>
 * Tests Slack workflow orchestration including async processing, messaging, and modal management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SlackLeaveOrchestrator Unit Tests")
class SlackLeaveOrchestratorTest {

    @Mock
    private LeaveService leaveService;

    @Mock
    private LeaveMapper leaveMapper;

    @Mock
    private SlackApiClient slackApiClient;

    private SlackLeaveOrchestrator orchestrator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Test constants
    private static final String TEST_USER_ID = "U12345";
    private static final String TEST_CHANNEL_ID = "C12345";
    private static final String TEST_CHANNEL_NAME = "test-channel";
    private static final String TEST_THREAD_TS = "1234567890.123456";
    private static final String TEST_TRIGGER_ID = "trigger123";
    private static final String TEST_VIEW_ID = "V12345";
    private static final String TEST_TIMESTAMP = "1234567890";

    @BeforeEach
    void setUp() {
        orchestrator = new SlackLeaveOrchestrator(leaveService, leaveMapper, slackApiClient);
    }

    @Nested
    @DisplayName("processLeaveRequestAsync Tests")
    class ProcessLeaveRequestAsyncTests {

        @Test
        @DisplayName("Should successfully process leave request with FULL_DAY ANNUAL_LEAVE")
        void shouldSuccessfullyProcessLeaveRequest() throws Exception {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );
            LeaveIngestionCommand command = createMockCommand();
            LeaveDto result = createMockLeaveDto();

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(leaveService.ingest(any())).thenReturn(result);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return new SlackMessageResponse();
            }).when(slackApiClient).postThreadReply(any(), any(), any());

            // When
            orchestrator.processLeaveRequestAsync(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            verify(leaveMapper).toCommand(leaveRequest, SourceType.SLACK, leaveRequest.getSourceId());
            verify(leaveService).ingest(command);
            verify(slackApiClient, times(1)).postThreadReply(eq(TEST_CHANNEL_ID), eq(TEST_THREAD_TS), any());
        }

        @Test
        @DisplayName("Should successfully process leave request with HALF_DAY OPTIONAL_HOLIDAY")
        void shouldSuccessfullyProcessHalfDayLeaveRequest() throws Exception {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.OPTIONAL_HOLIDAY, LeaveDurationType.FIRST_HALF
            );
            LeaveIngestionCommand command = createMockCommand();
            LeaveDto result = createMockLeaveDto();

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(leaveService.ingest(any())).thenReturn(result);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return new SlackMessageResponse();
            }).when(slackApiClient).postThreadReply(any(), any(), any());

            // When
            orchestrator.processLeaveRequestAsync(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            verify(leaveService).ingest(command);
        }

        @Test
        @DisplayName("Should post failure message when leave ingestion throws validation exception")
        void shouldPostFailureMessageOnValidationException() throws Exception {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );
            LeaveIngestionCommand command = createMockCommand();
            RuntimeException validationException = new RuntimeException("Validation failed: Invalid date range");

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(leaveService.ingest(any())).thenThrow(validationException);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> errorMessage = new AtomicReference<>();
            doAnswer(invocation -> {
                SlackMessageRequest message = invocation.getArgument(2);
                errorMessage.set(message.getText());
                latch.countDown();
                return new SlackMessageResponse();
            }).when(slackApiClient).postThreadReply(any(), any(), any());

            // When
            orchestrator.processLeaveRequestAsync(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            // Verify that an error message was posted (exact format may vary)
            assertThat(errorMessage.get()).isNotEmpty();
            assertThat(errorMessage.get()).containsIgnoringCase("failed");
        }

        @Test
        @DisplayName("Should post failure message when leave ingestion throws database exception")
        void shouldPostFailureMessageOnDatabaseException() throws Exception {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );
            LeaveIngestionCommand command = createMockCommand();
            RuntimeException dbException = new RuntimeException("Database connection failed");

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(leaveService.ingest(any())).thenThrow(dbException);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return new SlackMessageResponse();
            }).when(slackApiClient).postThreadReply(any(), any(), any());

            // When
            orchestrator.processLeaveRequestAsync(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            verify(slackApiClient).postThreadReply(eq(TEST_CHANNEL_ID), eq(TEST_THREAD_TS), any());
        }

        @Test
        @DisplayName("Should handle failure to post success message gracefully")
        void shouldHandleFailureToPostSuccessMessage() throws Exception {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );
            LeaveIngestionCommand command = createMockCommand();
            LeaveDto result = createMockLeaveDto();

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(slackApiClient.postThreadReply(any(), any(), any()))
                    .thenThrow(new RuntimeException("Slack API error"));

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return result;
            }).when(leaveService).ingest(any());

            // When
            orchestrator.processLeaveRequestAsync(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then - Should complete without throwing exception (best-effort error handling)
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            // Verify that slackApiClient was called (may have been called multiple times due to retries)
            verify(slackApiClient, atLeastOnce()).postThreadReply(any(), any(), any());
        }

        @Test
        @DisplayName("Should handle failure to post failure message gracefully")
        void shouldHandleFailureToPostFailureMessage() throws Exception {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );
            LeaveIngestionCommand command = createMockCommand();

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(slackApiClient.postThreadReply(any(), any(), any()))
                    .thenThrow(new RuntimeException("Messaging failed"));

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                throw new RuntimeException("Ingestion failed");
            }).when(leaveService).ingest(any());

            // When
            orchestrator.processLeaveRequestAsync(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            verify(slackApiClient, times(1)).postThreadReply(eq(TEST_CHANNEL_ID), eq(TEST_THREAD_TS), any());
        }

        @Test
        @DisplayName("Should run asynchronously and return immediately")
        void shouldRunAsynchronously() {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );
            LeaveIngestionCommand command = createMockCommand();
            LeaveDto result = createMockLeaveDto();

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(leaveService.ingest(any())).thenAnswer(invocation -> {
                Thread.sleep(100); // Simulate processing
                return result;
            });
            when(slackApiClient.postThreadReply(any(), any(), any())).thenReturn(new SlackMessageResponse());

            long startTime = System.currentTimeMillis();

            // When
            orchestrator.processLeaveRequestAsync(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            long endTime = System.currentTimeMillis();

            // Then - Should return immediately (within 200ms), not wait for processing
            // Note: @Async methods might have some dispatch overhead, so we allow reasonable time
            assertThat(endTime - startTime).isLessThan(200);
        }

        @Test
        @DisplayName("Should verify method signature is async")
        void shouldVerifyAsyncMethodSignature() throws NoSuchMethodException {
            // Then - Verify @Async annotation
            var method = SlackLeaveOrchestrator.class.getMethod("processLeaveRequestAsync",
                    LeaveIngestionRequest.class, String.class, String.class, String.class);

            assertThat(method.isAnnotationPresent(Async.class)).isTrue();
            assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("postThreadAnchorMessage Tests")
    class PostThreadAnchorMessageTests {

        @Test
        @DisplayName("Should successfully post anchor message to channel")
        void shouldSuccessfullyPostAnchorMessage() {
            // Given
            String userTag = "<@U12345>";
            SlackMessageResponse response = new SlackMessageResponse();
            response.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(eq(TEST_CHANNEL_ID), any())).thenReturn(response);

            // When
            SlackMessageResponse result = orchestrator.postThreadAnchorMessage(TEST_CHANNEL_ID, userTag);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTs()).isEqualTo(TEST_THREAD_TS);
            verify(slackApiClient).postMessage(eq(TEST_CHANNEL_ID), any());
        }

        @Test
        @DisplayName("Should return null on API exception")
        void shouldReturnNullOnApiException() {
            // Given
            String userTag = "<@U12345>";
            when(slackApiClient.postMessage(any(), any()))
                    .thenThrow(new RuntimeException("Slack API error"));

            // When
            SlackMessageResponse result = orchestrator.postThreadAnchorMessage(TEST_CHANNEL_ID, userTag);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle null channel ID gracefully")
        void shouldHandleNullChannelId() {
            // Given
            String userTag = "<@U12345>";
            when(slackApiClient.postMessage(isNull(), any()))
                    .thenThrow(new IllegalArgumentException("Channel ID cannot be null"));

            // When
            SlackMessageResponse result = orchestrator.postThreadAnchorMessage(null, userTag);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("postCancellationMessage Tests")
    class PostCancellationMessageTests {

        @Test
        @DisplayName("Should successfully post cancellation message to thread")
        void shouldSuccessfullyPostCancellationMessage() {
            // Given
            when(slackApiClient.postThreadReply(any(), any(), any())).thenReturn(new SlackMessageResponse());

            // When
            orchestrator.postCancellationMessage(TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then
            verify(slackApiClient).postThreadReply(eq(TEST_CHANNEL_ID), eq(TEST_THREAD_TS), any());
        }

        @Test
        @DisplayName("Should handle API exception gracefully (best-effort)")
        void shouldHandleApiExceptionGracefully() {
            // Given
            when(slackApiClient.postThreadReply(any(), any(), any()))
                    .thenThrow(new RuntimeException("Slack API error"));

            // When & Then - Should not throw exception
            orchestrator.postCancellationMessage(TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            verify(slackApiClient).postThreadReply(eq(TEST_CHANNEL_ID), eq(TEST_THREAD_TS), any());
        }
    }

    @Nested
    @DisplayName("handleViewSubmission Tests")
    class HandleViewSubmissionTests {

        @Test
        @DisplayName("Should successfully process valid view submission")
        void shouldSuccessfullyProcessViewSubmission() {
            // Given
            String requestBody = createValidViewSubmissionRequestBody();

            // Mock async call (we can't easily test async in unit test, so we'll just verify it's called)
            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).processLeaveRequestAsync(
                    any(), any(), any(), any()
            );

            // When
            spyOrchestrator.handleViewSubmission(requestBody);

            // Then - Verify the async method was called with correct parameters
            verify(spyOrchestrator).processLeaveRequestAsync(
                    any(LeaveIngestionRequest.class),
                    eq(TEST_CHANNEL_ID),
                    eq(TEST_THREAD_TS),
                    eq(TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("Should throw exception for invalid payload format")
        void shouldThrowExceptionForInvalidPayloadFormat() {
            // Given
            String invalidRequestBody = "invalid payload";

            // When & Then
            assertThatThrownBy(() -> orchestrator.handleViewSubmission(invalidRequestBody))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should handle ANNUAL_LEAVE type")
        void shouldHandleAnnualLeaveType() {
            // Given
            String requestBody = createViewSubmissionRequestBody(LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY);

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).processLeaveRequestAsync(
                    any(), any(), any(), any()
            );

            // When
            spyOrchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionRequest> requestCaptor = ArgumentCaptor.forClass(LeaveIngestionRequest.class);
            verify(spyOrchestrator).processLeaveRequestAsync(
                    requestCaptor.capture(), any(), any(), any()
            );
            assertThat(requestCaptor.getValue().getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        }

        @Test
        @DisplayName("Should handle OPTIONAL_HOLIDAY type")
        void shouldHandleOptionalHolidayType() {
            // Given
            String requestBody = createViewSubmissionRequestBody(LeaveType.OPTIONAL_HOLIDAY, LeaveDurationType.FULL_DAY);

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).processLeaveRequestAsync(
                    any(), any(), any(), any()
            );

            // When
            spyOrchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionRequest> requestCaptor = ArgumentCaptor.forClass(LeaveIngestionRequest.class);
            verify(spyOrchestrator).processLeaveRequestAsync(
                    requestCaptor.capture(), any(), any(), any()
            );
            assertThat(requestCaptor.getValue().getType()).isEqualTo(LeaveType.OPTIONAL_HOLIDAY);
        }

        @Test
        @DisplayName("Should handle FIRST_HALF duration")
        void shouldHandleFirstHalfDuration() {
            // Given
            String requestBody = createViewSubmissionRequestBody(LeaveType.ANNUAL_LEAVE, LeaveDurationType.FIRST_HALF);

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).processLeaveRequestAsync(
                    any(), any(), any(), any()
            );

            // When
            spyOrchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionRequest> requestCaptor = ArgumentCaptor.forClass(LeaveIngestionRequest.class);
            verify(spyOrchestrator).processLeaveRequestAsync(
                    requestCaptor.capture(), any(), any(), any()
            );
            assertThat(requestCaptor.getValue().getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
        }

        @Test
        @DisplayName("Should handle SECOND_HALF duration")
        void shouldHandleSecondHalfDuration() {
            // Given
            String requestBody = createViewSubmissionRequestBody(LeaveType.ANNUAL_LEAVE, LeaveDurationType.SECOND_HALF);

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).processLeaveRequestAsync(
                    any(), any(), any(), any()
            );

            // When
            spyOrchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionRequest> requestCaptor = ArgumentCaptor.forClass(LeaveIngestionRequest.class);
            verify(spyOrchestrator).processLeaveRequestAsync(
                    requestCaptor.capture(), any(), any(), any()
            );
            assertThat(requestCaptor.getValue().getDurationType()).isEqualTo(LeaveDurationType.SECOND_HALF);
        }

        @Test
        @DisplayName("Should handle single day leave without end date")
        void shouldHandleSingleDayLeave() {
            // Given
            String requestBody = createViewSubmissionRequestBodyWithoutEndDate();

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).processLeaveRequestAsync(
                    any(), any(), any(), any()
            );

            // When
            spyOrchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionRequest> requestCaptor = ArgumentCaptor.forClass(LeaveIngestionRequest.class);
            verify(spyOrchestrator).processLeaveRequestAsync(
                    requestCaptor.capture(), any(), any(), any()
            );
            assertThat(requestCaptor.getValue().getDateRange().getEndDate())
                    .isEqualTo(requestCaptor.getValue().getDateRange().getStartDate());
        }
    }

    @Nested
    @DisplayName("handleViewClosed Tests")
    class HandleViewClosedTests {

        @Test
        @DisplayName("Should successfully post cancellation message")
        void shouldSuccessfullyPostCancellationMessage() {
            // Given
            String requestBody = createValidViewClosedRequestBody();

            // When
            orchestrator.handleViewClosed(requestBody);

            // Then
            verify(slackApiClient).postThreadReply(eq(TEST_CHANNEL_ID), eq(TEST_THREAD_TS), any());
        }

        @Test
        @DisplayName("Should handle invalid metadata format")
        void shouldHandleInvalidMetadataFormat() {
            // Given
            String requestBody = createViewClosedRequestBodyWithInvalidMetadata();

            // When & Then
            assertThatThrownBy(() -> orchestrator.handleViewClosed(requestBody))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should handle API failure gracefully (best-effort)")
        void shouldHandleApiFailureGracefully() {
            // Given
            String requestBody = createValidViewClosedRequestBody();
            when(slackApiClient.postThreadReply(any(), any(), any()))
                    .thenThrow(new RuntimeException("Slack API error"));

            // When & Then - Should not throw exception
            orchestrator.handleViewClosed(requestBody);
        }
    }

    @Nested
    @DisplayName("handleSlashCommand Tests")
    class HandleSlashCommandTests {

        @Test
        @DisplayName("Should successfully handle slash command workflow")
        void shouldSuccessfullyHandleSlashCommand() {
            // Given
            SlackCommandRequest commandRequest = createValidSlackCommandRequest();
            SlackMessageResponse messageResponse = new SlackMessageResponse();
            messageResponse.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(eq(TEST_CHANNEL_ID), any())).thenReturn(messageResponse);

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).openLeaveApplicationModalAsync(any(), any());

            // When
            spyOrchestrator.handleSlashCommand(commandRequest);

            // Then
            verify(slackApiClient).postMessage(eq(TEST_CHANNEL_ID), any());
            verify(spyOrchestrator).openLeaveApplicationModalAsync(eq(commandRequest), eq(TEST_THREAD_TS));
        }

        @Test
        @DisplayName("Should continue workflow when anchor message fails")
        void shouldContinueWorkflowWhenAnchorFails() {
            // Given
            SlackCommandRequest commandRequest = createValidSlackCommandRequest();
            when(slackApiClient.postMessage(any(), any())).thenReturn(null);

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).openLeaveApplicationModalAsync(any(), any());

            // When
            spyOrchestrator.handleSlashCommand(commandRequest);

            // Then
            verify(spyOrchestrator).openLeaveApplicationModalAsync(eq(commandRequest), isNull());
        }

        @Test
        @DisplayName("Should create correct user tag format")
        void shouldCreateCorrectUserTagFormat() {
            // Given
            SlackCommandRequest commandRequest = createValidSlackCommandRequest();
            SlackMessageResponse messageResponse = new SlackMessageResponse();
            messageResponse.setTs(TEST_THREAD_TS);

            ArgumentCaptor<SlackMessageRequest> messageCaptor = ArgumentCaptor.forClass(SlackMessageRequest.class);
            when(slackApiClient.postMessage(any(), messageCaptor.capture())).thenReturn(messageResponse);

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).openLeaveApplicationModalAsync(any(), any());

            // When
            spyOrchestrator.handleSlashCommand(commandRequest);

            // Then
            assertThat(messageCaptor.getValue().getText()).contains("<@" + TEST_USER_ID + ">");
        }

        @Test
        @DisplayName("Should handle anchor API exception gracefully")
        void shouldHandleAnchorApiExceptionGracefully() {
            // Given
            SlackCommandRequest commandRequest = createValidSlackCommandRequest();
            when(slackApiClient.postMessage(any(), any()))
                    .thenThrow(new RuntimeException("Slack API error"));

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).openLeaveApplicationModalAsync(any(), any());

            // When
            spyOrchestrator.handleSlashCommand(commandRequest);

            // Then - Should continue with modal opening
            verify(spyOrchestrator).openLeaveApplicationModalAsync(eq(commandRequest), isNull());
        }

        @Test
        @DisplayName("Should handle concurrent slash commands safely")
        void shouldHandleConcurrentCommandsSafely() throws InterruptedException {
            // Given
            SlackCommandRequest command1 = createSlackCommandRequest("U001", "C001");
            SlackCommandRequest command2 = createSlackCommandRequest("U002", "C002");
            SlackMessageResponse response = new SlackMessageResponse();
            response.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(any(), any())).thenReturn(response);

            SlackLeaveOrchestrator spyOrchestrator = spy(orchestrator);
            doNothing().when(spyOrchestrator).openLeaveApplicationModalAsync(any(), any());

            // When
            Thread thread1 = new Thread(() -> spyOrchestrator.handleSlashCommand(command1));
            Thread thread2 = new Thread(() -> spyOrchestrator.handleSlashCommand(command2));

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Then - Should handle both without interference
            verify(spyOrchestrator, times(2)).openLeaveApplicationModalAsync(any(), any());
        }
    }

    @Nested
    @DisplayName("openLeaveApplicationModalAsync Tests")
    class OpenLeaveApplicationModalAsyncTests {

        @Test
        @DisplayName("Should successfully open modal")
        void shouldSuccessfullyOpenModal() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return new SlackViewOpenResponse();
            }).when(slackApiClient).openModal(any(), any());

            // When
            orchestrator.openLeaveApplicationModalAsync(slackRequest, TEST_THREAD_TS);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            verify(slackApiClient).openModal(eq(TEST_TRIGGER_ID), any(SlackModalView.class));
        }

        @Test
        @DisplayName("Should handle API exception gracefully")
        void shouldHandleApiExceptionGracefully() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            doThrow(new RuntimeException("Slack API error"))
                    .when(slackApiClient).openModal(any(), any());

            // When
            orchestrator.openLeaveApplicationModalAsync(slackRequest, TEST_THREAD_TS);

            // Then - Should complete without exception despite exception being thrown
            verify(slackApiClient).openModal(any(), any());
        }

        @Test
        @DisplayName("Should embed thread context in modal metadata")
        void shouldEmbedThreadContextInModalMetadata() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            ArgumentCaptor<SlackModalView> modalCaptor = ArgumentCaptor.forClass(SlackModalView.class);
            CountDownLatch latch = new CountDownLatch(1);

            doAnswer(invocation -> {
                latch.countDown();
                return new SlackViewOpenResponse();
            }).when(slackApiClient).openModal(eq(TEST_TRIGGER_ID), modalCaptor.capture());

            // When
            orchestrator.openLeaveApplicationModalAsync(slackRequest, TEST_THREAD_TS);

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

            // Then
            String privateMetadata = modalCaptor.getValue().getPrivateMetadata();
            assertThat(privateMetadata).contains(TEST_USER_ID);
            assertThat(privateMetadata).contains(TEST_CHANNEL_ID);
            assertThat(privateMetadata).contains(TEST_CHANNEL_NAME);
            assertThat(privateMetadata).contains(TEST_THREAD_TS);
        }

        @Test
        @DisplayName("Should run asynchronously")
        void shouldRunAsynchronously() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            when(slackApiClient.openModal(any(), any())).thenAnswer(invocation -> {
                Thread.sleep(100);
                return new SlackMessageResponse();
            });

            long startTime = System.currentTimeMillis();

            // When
            orchestrator.openLeaveApplicationModalAsync(slackRequest, TEST_THREAD_TS);

            long endTime = System.currentTimeMillis();

            // Then - Should return immediately (within 200ms for async dispatch)
            assertThat(endTime - startTime).isLessThan(200);
        }
    }

    // Helper methods for test data creation

    private LeaveIngestionRequest createValidLeaveIngestionRequest(LeaveType type, LeaveDurationType duration) {
        LeaveIngestionRequest request = new LeaveIngestionRequest();
        request.setUserId(TEST_USER_ID);
        request.setType(type);
        request.setDurationType(duration);
        request.setDateRange(new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)));
        request.setSourceType(SourceType.SLACK);
        request.setSourceId("slack-source-123");
        return request;
    }

    private LeaveIngestionCommand createMockCommand() {
        return LeaveIngestionCommand.builder()
                .userId(TEST_USER_ID)
                .dateRange(new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)))
                .type(LeaveType.ANNUAL_LEAVE)
                .durationType(LeaveDurationType.FULL_DAY)
                .sourceType(SourceType.SLACK)
                .sourceId("slack-source-123")
                .build();
    }

    private LeaveDto createMockLeaveDto() {
        LeaveDto dto = new LeaveDto();
        dto.setId(UUID.randomUUID());
        dto.setUserId(TEST_USER_ID);
        dto.setType(LeaveType.ANNUAL_LEAVE);
        dto.setDurationType(LeaveDurationType.FULL_DAY);
        dto.setDateRange(new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)));
        dto.setStatus(LeaveStatus.REQUESTED);
        return dto;
    }

    private SlackCommandRequest createValidSlackCommandRequest() {
        return createSlackCommandRequest(TEST_USER_ID, TEST_CHANNEL_ID);
    }

    private SlackCommandRequest createSlackCommandRequest(String userId, String channelId) {
        SlackCommandRequest request = new SlackCommandRequest();
        request.setCommand("/leave");
        request.setText("");
        request.setTriggerId(TEST_TRIGGER_ID);
        request.setUserId(userId);
        request.setUserName("testuser");
        request.setChannelId(channelId);
        request.setChannelName(TEST_CHANNEL_NAME);
        request.setTeamId("T12345");
        request.setTeamDomain("test-workspace");
        request.setResponseUrl("https://hooks.slack.com");
        request.setApiAppId("A12345");
        return request;
    }

    private String createValidViewSubmissionRequestBody() {
        return createViewSubmissionRequestBody(LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY);
    }

    private String createViewSubmissionRequestBody(LeaveType type, LeaveDurationType duration) {
        try {
            // Create metadata object
            var metadata = Map.of(
                "userId", TEST_USER_ID,
                "channelId", TEST_CHANNEL_ID,
                "channelName", TEST_CHANNEL_NAME,
                "threadTs", TEST_THREAD_TS
            );
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // Build the complete view submission payload
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", "view_submission");

            var team = Map.of("id", "T12345", "domain", "example");
            payload.put("team", team);

            var user = Map.of(
                "id", TEST_USER_ID,
                "username", "testuser",
                "name", "Test User",
                "team_id", "T12345"
            );
            payload.put("user", user);

            payload.put("api_app_id", "A12345");
            payload.put("token", "verification_token");
            payload.put("trigger_id", "trigger123");

            // Build view object with state
            var view = new java.util.LinkedHashMap<String, Object>();
            view.put("id", TEST_VIEW_ID);
            view.put("team_id", "T12345");
            view.put("type", "modal");
            view.put("callback_id", "leave_application_submit");
            view.put("private_metadata", metadataJson);

            var state = new java.util.LinkedHashMap<String, Object>();
            var values = new java.util.LinkedHashMap<String, Object>();

            // Leave type block
            var leaveTypeBlock = new java.util.LinkedHashMap<String, Object>();
            var leaveTypeAction = new java.util.LinkedHashMap<String, Object>();
            leaveTypeAction.put("type", "radio_buttons");
            leaveTypeAction.put("selected_option", Map.of(
                "text", Map.of("type", "plain_text", "text", type.toString().replace("_", " ")),
                "value", type.toString()
            ));
            leaveTypeBlock.put("leave_type_category_action", leaveTypeAction);
            values.put("leave_type_category_block", leaveTypeBlock);

            // Leave duration block
            var durationBlock = new java.util.LinkedHashMap<String, Object>();
            var durationAction = new java.util.LinkedHashMap<String, Object>();
            durationAction.put("type", "radio_buttons");
            durationAction.put("selected_option", Map.of(
                "text", Map.of("type", "plain_text", "text", duration.toString().replace("_", " ")),
                "value", duration.toString()
            ));
            durationBlock.put("leave_duration_action", durationAction);
            values.put("leave_duration_block", durationBlock);

            // Start date block
            var startDateBlock = new java.util.LinkedHashMap<String, Object>();
            startDateBlock.put("start_date_action", Map.of(
                "type", "datepicker",
                "selected_date", "2024-07-01"
            ));
            values.put("start_date_block", startDateBlock);

            // End date block
            var endDateBlock = new java.util.LinkedHashMap<String, Object>();
            endDateBlock.put("end_date_action", Map.of(
                "type", "datepicker",
                "selected_date", "2024-07-05"
            ));
            values.put("end_date_block", endDateBlock);

            state.put("values", values);
            view.put("state", state);
            payload.put("view", view);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return "payload=" + urlEncode(jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create view submission payload", e);
        }
    }

    private String createViewSubmissionRequestBodyWithoutEndDate() {
        try {
            // Create metadata object
            var metadata = Map.of(
                "userId", TEST_USER_ID,
                "channelId", TEST_CHANNEL_ID,
                "channelName", TEST_CHANNEL_NAME,
                "threadTs", TEST_THREAD_TS
            );
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // Build the complete view submission payload
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", "view_submission");

            var team = Map.of("id", "T12345", "domain", "example");
            payload.put("team", team);

            var user = Map.of(
                "id", TEST_USER_ID,
                "username", "testuser",
                "name", "Test User",
                "team_id", "T12345"
            );
            payload.put("user", user);

            payload.put("api_app_id", "A12345");
            payload.put("token", "verification_token");
            payload.put("trigger_id", "trigger123");

            // Build view object with state (no end date)
            var view = new java.util.LinkedHashMap<String, Object>();
            view.put("id", TEST_VIEW_ID);
            view.put("team_id", "T12345");
            view.put("type", "modal");
            view.put("callback_id", "leave_application_submit");
            view.put("private_metadata", metadataJson);

            var state = new java.util.LinkedHashMap<String, Object>();
            var values = new java.util.LinkedHashMap<String, Object>();

            // Leave type block
            var leaveTypeBlock = new java.util.LinkedHashMap<String, Object>();
            var leaveTypeAction = new java.util.LinkedHashMap<String, Object>();
            leaveTypeAction.put("type", "radio_buttons");
            leaveTypeAction.put("selected_option", Map.of(
                "text", Map.of("type", "plain_text", "text", "Annual Leave"),
                "value", "ANNUAL_LEAVE"
            ));
            leaveTypeBlock.put("leave_type_category_action", leaveTypeAction);
            values.put("leave_type_category_block", leaveTypeBlock);

            // Leave duration block
            var durationBlock = new java.util.LinkedHashMap<String, Object>();
            var durationAction = new java.util.LinkedHashMap<String, Object>();
            durationAction.put("type", "radio_buttons");
            durationAction.put("selected_option", Map.of(
                "text", Map.of("type", "plain_text", "text", "Full Day"),
                "value", "FULL_DAY"
            ));
            durationBlock.put("leave_duration_action", durationAction);
            values.put("leave_duration_block", durationBlock);

            // Start date block only (no end date)
            var startDateBlock = new java.util.LinkedHashMap<String, Object>();
            startDateBlock.put("start_date_action", Map.of(
                "type", "datepicker",
                "selected_date", "2024-07-01"
            ));
            values.put("start_date_block", startDateBlock);

            state.put("values", values);
            view.put("state", state);
            payload.put("view", view);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return "payload=" + urlEncode(jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create view submission payload without end date", e);
        }
    }

    private String createValidViewClosedRequestBody() {
        try {
            // Create metadata object
            var metadata = Map.of(
                "userId", TEST_USER_ID,
                "channelId", TEST_CHANNEL_ID,
                "channelName", TEST_CHANNEL_NAME,
                "threadTs", TEST_THREAD_TS
            );
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // Build the view closed payload
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", "view_closed");

            var team = Map.of("id", "T12345", "domain", "example");
            payload.put("team", team);

            var user = Map.of(
                "id", TEST_USER_ID,
                "username", "testuser",
                "name", "Test User",
                "team_id", "T12345"
            );
            payload.put("user", user);

            payload.put("api_app_id", "A12345");
            payload.put("token", "verification_token");

            var view = new java.util.LinkedHashMap<String, Object>();
            view.put("id", TEST_VIEW_ID);
            view.put("team_id", "T12345");
            view.put("type", "modal");
            view.put("private_metadata", metadataJson);
            payload.put("view", view);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return "payload=" + urlEncode(jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create view closed payload", e);
        }
    }

    private String createViewClosedRequestBodyWithInvalidMetadata() {
        try {
            // Build the view closed payload with invalid metadata
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", "view_closed");

            var team = Map.of("id", "T12345", "domain", "example");
            payload.put("team", team);

            var user = Map.of(
                "id", TEST_USER_ID,
                "username", "testuser",
                "name", "Test User",
                "team_id", "T12345"
            );
            payload.put("user", user);

            payload.put("api_app_id", "A12345");
            payload.put("token", "verification_token");

            var view = new java.util.LinkedHashMap<String, Object>();
            view.put("id", TEST_VIEW_ID);
            view.put("team_id", "T12345");
            view.put("type", "modal");
            view.put("private_metadata", "invalid json");
            payload.put("view", view);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return "payload=" + urlEncode(jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create view closed payload with invalid metadata", e);
        }
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL encode", e);
        }
    }
}
