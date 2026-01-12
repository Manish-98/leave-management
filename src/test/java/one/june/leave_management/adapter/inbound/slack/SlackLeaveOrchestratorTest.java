package one.june.leave_management.adapter.inbound.slack;

import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionRequest.SlackTeam;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionRequest.SlackUser;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionRequest.SlackChannel;
import one.june.leave_management.adapter.inbound.slack.dto.SlackBlockActionRequest.SlackMessage;
import one.june.leave_management.adapter.inbound.slack.dto.SlackCommandRequest;
import one.june.leave_management.adapter.inbound.slack.dto.SlackAction;
import one.june.leave_management.adapter.inbound.slack.dto.SlackViewSubmissionRequest;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.adapter.outbound.slack.client.SlackApiClient;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageRequest;
import one.june.leave_management.adapter.outbound.slack.dto.SlackMessageResponse;
import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.SlackViewOpenResponse;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.application.genai.dto.ParseResult;
import one.june.leave_management.application.genai.dto.ParsedLeaveRequest;
import one.june.leave_management.application.genai.service.LeaveParsingService;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.application.leave.service.OptionalHolidayService;
import one.june.leave_management.common.async.AsyncUtility;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.adapter.inbound.slack.mapper.SlackLeaveRequestMapper;
import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Mock
    private OptionalHolidayService optionalHolidayService;

    @Mock
    private SlackLeaveRequestMapper slackLeaveRequestMapper;

    @Mock
    private LeaveParsingService leaveParsingService;

    @Mock
    private AsyncUtility asyncUtility;

    @Mock
    private EmployeeRepository employeeRepository;

    private SlackLeaveOrchestrator orchestrator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Test constants
    private static final String TEST_USER_ID = "U12345";
    private static final String TEST_CHANNEL_ID = "C12345";
    private static final String TEST_CHANNEL_NAME = "test-channel";
    private static final String TEST_THREAD_TS = "1234567890.123456";
    private static final String TEST_TRIGGER_ID = "trigger123";
    private static final String TEST_VIEW_ID = "V12345";
    private static final String TEST_RESPONSE_URL = "https://hooks.slack.com/commands/T123/C123/secret";

    @BeforeEach
    void setUp() {
        orchestrator = new SlackLeaveOrchestrator(
                leaveService,
                leaveMapper,
                slackApiClient,
                optionalHolidayService,
                slackLeaveRequestMapper,
                leaveParsingService,
                asyncUtility,
                employeeRepository
        );
    }

    @Nested
    @DisplayName("processLeaveRequest Tests")
    class ProcessLeaveRequestAsyncTests {

        @BeforeEach
        void setUpProcessLeaveRequestTests() {
            // For these tests, executeAsync should immediately execute the runnable
            lenient().doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(asyncUtility).executeAsync(any());
        }

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
            orchestrator.processLeaveRequest(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

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
            orchestrator.processLeaveRequest(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

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
            orchestrator.processLeaveRequest(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

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
            orchestrator.processLeaveRequest(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

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
            orchestrator.processLeaveRequest(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

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
            orchestrator.processLeaveRequest(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            verify(slackApiClient, times(1)).postThreadReply(eq(TEST_CHANNEL_ID), eq(TEST_THREAD_TS), any());
        }

        @Test
        @DisplayName("Should run asynchronously via AsyncUtility")
        void shouldRunAsynchronously() throws Exception {
            // Given - Set up the mapper for handleViewSubmission
            lenient().when(slackLeaveRequestMapper.toLeaveIngestionRequest(any(SlackViewSubmissionRequest.class)))
                    .thenAnswer(invocation -> {
                        SlackViewSubmissionRequest request = invocation.getArgument(0);
                        // Return a valid request
                        return createValidLeaveIngestionRequest(
                                LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY,
                                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)
                        );
                    });

            String requestBody = createValidViewSubmissionRequestBody();
            LeaveIngestionCommand command = createMockCommand();
            LeaveDto result = createMockLeaveDto();

            lenient().when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(leaveService.ingest(any())).thenAnswer(invocation -> {
                Thread.sleep(100); // Simulate processing
                return result;
            });
            lenient().when(slackApiClient.postThreadReply(any(), any(), any())).thenReturn(new SlackMessageResponse());

            // Override asyncUtility to actually run async for this test
            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                new Thread(() -> {
                    try {
                        runnable.run();
                    } finally {
                        latch.countDown();
                    }
                }).start();
                return null;
            }).when(asyncUtility).executeAsync(any());

            long startTime = System.currentTimeMillis();

            // When - call handleViewSubmission which uses async wrapper
            orchestrator.handleViewSubmission(requestBody);

            long endTime = System.currentTimeMillis();

            // Then - Should return immediately (within 500ms), not wait for processing
            assertThat(endTime - startTime).isLessThan(500);

            // Wait for async processing to complete
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

            // Verify the service was actually called
            verify(leaveService).ingest(any());
        }

        @Test
        @DisplayName("Should verify method signature has @Transactional but not @Async")
        void shouldVerifyAsyncMethodSignature() throws NoSuchMethodException {
            // Then - Verify @Transactional annotation is present, but not @Async
            var method = SlackLeaveOrchestrator.class.getMethod("processLeaveRequest",
                    LeaveIngestionRequest.class, String.class, String.class, String.class);

            assertThat(method.isAnnotationPresent(Async.class)).isFalse();
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
            SlackMessageResponse result = orchestrator.postThreadAnchorMessage(
                    TEST_CHANNEL_ID, TEST_THREAD_TS, userTag, "parsing", "test text"
            );

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
            SlackMessageResponse result = orchestrator.postThreadAnchorMessage(
                    TEST_CHANNEL_ID, TEST_THREAD_TS, userTag, "parsing", "test text"
            );

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
            SlackMessageResponse result = orchestrator.postThreadAnchorMessage(
                    null, TEST_THREAD_TS, userTag, "parsing", "test text"
            );

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

        @BeforeEach
        void setUp() {
            // Mock the mapper to return valid LeaveIngestionRequest based on the request type
            lenient().when(slackLeaveRequestMapper.toLeaveIngestionRequest(any(SlackViewSubmissionRequest.class)))
                    .thenAnswer(invocation -> {
                        SlackViewSubmissionRequest request = invocation.getArgument(0);
                        // Extract the leave type from the state values
                        var stateValues = request.getView().getState().getValues();
                        var leaveTypeBlock = stateValues.get("leave_type_category_block");
                        var leaveTypeAction = leaveTypeBlock.get("leave_type_category_action");
                        String selectedValue = leaveTypeAction.getSelectedOption().getValue();
                        LeaveType leaveType = LeaveType.valueOf(selectedValue);

                        // Extract the duration from the state values (only for ANNUAL_LEAVE)
                        LeaveDurationType duration = LeaveDurationType.FULL_DAY; // default
                        if (leaveType == LeaveType.ANNUAL_LEAVE) {
                            var durationBlock = stateValues.get("leave_duration_block");
                            var durationAction = durationBlock.get("leave_duration_action");
                            String durationValue = durationAction.getSelectedOption().getValue();
                            duration = LeaveDurationType.valueOf(durationValue);
                        }

                        // Extract dates from the state values (only for ANNUAL_LEAVE)
                        LocalDate startDate = null;
                        LocalDate endDate = null;
                        if (leaveType == LeaveType.ANNUAL_LEAVE) {
                            var startDateBlock = stateValues.get("start_date_block");
                            var startDateAction = startDateBlock.get("start_date_action");
                            startDate = LocalDate.parse(startDateAction.getSelectedDate());

                            // Check if end date exists
                            if (stateValues.containsKey("end_date_block")) {
                                var endDateBlock = stateValues.get("end_date_block");
                                var endDateAction = endDateBlock.get("end_date_action");
                                if (endDateAction.getSelectedDate() != null) {
                                    endDate = LocalDate.parse(endDateAction.getSelectedDate());
                                }
                            }

                            // If no end date, set it equal to start date
                            if (endDate == null) {
                                endDate = startDate;
                            }
                        }

                        // Create the LeaveIngestionRequest
                        return createValidLeaveIngestionRequest(leaveType, duration, startDate, endDate);
                    });

            // For these tests, executeAsync should immediately execute the runnable
            lenient().doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(asyncUtility).executeAsync(any());

            // Mock leaveMapper to capture the request and return appropriate command
            lenient().when(leaveMapper.toCommand(any(), any(), any()))
                    .thenAnswer(invocation -> {
                        LeaveIngestionRequest request = invocation.getArgument(0);
                        return LeaveIngestionCommand.builder()
                                .userId(TEST_USER_ID)
                                .dateRange(request.getDateRange())
                                .type(request.getType())
                                .durationType(request.getDurationType())
                                .sourceType(request.getSourceType())
                                .sourceId(request.getSourceId())
                                .build();
                    });
        }

        @Test
        @DisplayName("Should successfully process valid view submission")
        void shouldSuccessfullyProcessViewSubmission() {
            // Given
            String requestBody = createValidViewSubmissionRequestBody();
            when(leaveService.ingest(any())).thenReturn(createMockLeaveDto());

            // When
            orchestrator.handleViewSubmission(requestBody);

            // Then - Verify the service was called
            verify(leaveService).ingest(any());
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
            when(leaveService.ingest(any())).thenReturn(createMockLeaveDto());

            // When
            orchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionCommand> commandCaptor = ArgumentCaptor.forClass(LeaveIngestionCommand.class);
            verify(leaveService).ingest(commandCaptor.capture());
            assertThat(commandCaptor.getValue().getType()).isEqualTo(LeaveType.ANNUAL_LEAVE);
        }

        @Test
        @DisplayName("Should handle OPTIONAL_HOLIDAY type")
        void shouldHandleOptionalHolidayType() {
            // Given
            String requestBody = createViewSubmissionRequestBody(LeaveType.OPTIONAL_HOLIDAY, LeaveDurationType.FULL_DAY);
            when(leaveService.ingest(any())).thenReturn(createMockLeaveDto());

            // When
            orchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionCommand> commandCaptor = ArgumentCaptor.forClass(LeaveIngestionCommand.class);
            verify(leaveService).ingest(commandCaptor.capture());
            assertThat(commandCaptor.getValue().getType()).isEqualTo(LeaveType.OPTIONAL_HOLIDAY);
        }

        @Test
        @DisplayName("Should handle FIRST_HALF duration")
        void shouldHandleFirstHalfDuration() {
            // Given
            String requestBody = createViewSubmissionRequestBody(LeaveType.ANNUAL_LEAVE, LeaveDurationType.FIRST_HALF);
            when(leaveService.ingest(any())).thenReturn(createMockLeaveDto());

            // When
            orchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionCommand> commandCaptor = ArgumentCaptor.forClass(LeaveIngestionCommand.class);
            verify(leaveService).ingest(commandCaptor.capture());
            assertThat(commandCaptor.getValue().getDurationType()).isEqualTo(LeaveDurationType.FIRST_HALF);
        }

        @Test
        @DisplayName("Should handle SECOND_HALF duration")
        void shouldHandleSecondHalfDuration() {
            // Given
            String requestBody = createViewSubmissionRequestBody(LeaveType.ANNUAL_LEAVE, LeaveDurationType.SECOND_HALF);
            when(leaveService.ingest(any())).thenReturn(createMockLeaveDto());

            // When
            orchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionCommand> commandCaptor = ArgumentCaptor.forClass(LeaveIngestionCommand.class);
            verify(leaveService).ingest(commandCaptor.capture());
            assertThat(commandCaptor.getValue().getDurationType()).isEqualTo(LeaveDurationType.SECOND_HALF);
        }

        @Test
        @DisplayName("Should handle single day leave without end date")
        void shouldHandleSingleDayLeave() {
            // Given
            String requestBody = createViewSubmissionRequestBodyWithoutEndDate();
            when(leaveService.ingest(any())).thenReturn(createMockLeaveDto());

            // When
            orchestrator.handleViewSubmission(requestBody);

            // Then
            ArgumentCaptor<LeaveIngestionCommand> commandCaptor = ArgumentCaptor.forClass(LeaveIngestionCommand.class);
            verify(leaveService).ingest(commandCaptor.capture());
            assertThat(commandCaptor.getValue().getDateRange().getEndDate())
                    .isEqualTo(commandCaptor.getValue().getDateRange().getStartDate());
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

        @BeforeEach
        void setUpHandleSlashCommandTests() {
            // For these tests, executeAsync should immediately execute the runnable
            lenient().doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(asyncUtility).executeAsync(any());
        }

        @Test
        @DisplayName("Should successfully handle slash command workflow")
        void shouldSuccessfullyHandleSlashCommand() {
            // Given
            SlackCommandRequest commandRequest = createValidSlackCommandRequest();
            SlackMessageResponse messageResponse = new SlackMessageResponse();
            messageResponse.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(eq(TEST_CHANNEL_ID), any())).thenReturn(messageResponse);

            // When
            orchestrator.handleSlashCommand(commandRequest);

            // Then
            verify(slackApiClient).postMessage(eq(TEST_CHANNEL_ID), any());
            verify(slackApiClient).openModal(eq(commandRequest.getTriggerId()), any());
        }

        @Test
        @DisplayName("Should continue workflow when anchor message fails")
        void shouldContinueWorkflowWhenAnchorFails() {
            // Given
            SlackCommandRequest commandRequest = createValidSlackCommandRequest();
            when(slackApiClient.postMessage(any(), any())).thenReturn(null);

            // When
            orchestrator.handleSlashCommand(commandRequest);

            // Then - Should still attempt to open modal (with null threadTs)
            verify(slackApiClient).openModal(eq(commandRequest.getTriggerId()), any());
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
            doNothing().when(spyOrchestrator).openLeaveApplicationModal(any(), any());

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
            doNothing().when(spyOrchestrator).openLeaveApplicationModal(any(), any());

            // When
            spyOrchestrator.handleSlashCommand(commandRequest);

            // Then - Should continue with modal opening
            verify(spyOrchestrator).openLeaveApplicationModal(eq(commandRequest), isNull());
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
            doNothing().when(spyOrchestrator).openLeaveApplicationModal(any(), any());

            // When
            Thread thread1 = new Thread(() -> spyOrchestrator.handleSlashCommand(command1));
            Thread thread2 = new Thread(() -> spyOrchestrator.handleSlashCommand(command2));

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Then - Should handle both without interference
            verify(spyOrchestrator, times(2)).openLeaveApplicationModal(any(), any());
        }
    }

    @Nested
    @DisplayName("openLeaveApplicationModal Tests")
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
            orchestrator.openLeaveApplicationModal(slackRequest, TEST_THREAD_TS);

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
            orchestrator.openLeaveApplicationModal(slackRequest, TEST_THREAD_TS);

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
            orchestrator.openLeaveApplicationModal(slackRequest, TEST_THREAD_TS);

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

            // Then
            String privateMetadata = modalCaptor.getValue().getPrivateMetadata();
            assertThat(privateMetadata).contains(TEST_USER_ID);
            assertThat(privateMetadata).contains(TEST_CHANNEL_ID);
            assertThat(privateMetadata).contains(TEST_CHANNEL_NAME);
            assertThat(privateMetadata).contains(TEST_THREAD_TS);
        }

        @Test
        @DisplayName("Should run synchronously for no-text modal flow")
        void shouldRunSynchronouslyForNoTextModal() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            SlackMessageResponse messageResponse = new SlackMessageResponse();
            messageResponse.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(any(), any())).thenReturn(messageResponse);
            when(slackApiClient.openModal(any(), any())).thenAnswer(invocation -> {
                Thread.sleep(100);
                return new SlackMessageResponse();
            });

            long startTime = System.currentTimeMillis();

            // When - call handleSlashCommand which posts anchor and opens modal synchronously
            orchestrator.handleSlashCommand(slackRequest);

            long endTime = System.currentTimeMillis();

            // Then - Should complete synchronously (allowing time for message + modal)
            // The modal opening takes 100ms, so total should be > 100ms but reasonable
            assertThat(endTime - startTime).isGreaterThan(100);
            assertThat(endTime - startTime).isLessThan(500);

            // Verify both operations were called
            verify(slackApiClient).postMessage(eq(slackRequest.getChannelId()), any());
            verify(slackApiClient).openModal(eq(slackRequest.getTriggerId()), any());
        }
    }

    @Nested
    @DisplayName("Additional Edge Case Tests")
    class AdditionalEdgeCaseTests {

        @BeforeEach
        void setUpAdditionalEdgeCaseTests() {
            // For these tests, executeAsync should immediately execute the runnable
            lenient().doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(asyncUtility).executeAsync(any());
        }

        @Test
        @DisplayName("Should handle invalid leave type in command text")
        void shouldHandleInvalidLeaveTypeInCommand() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("invalid_type");

            // Mock the AI parsing to fail
            when(leaveParsingService.parseLeaveRequest(anyString(), anyString())).thenReturn(
                    one.june.leave_management.application.genai.dto.ParseResult.builder()
                            .failure("Invalid leave type")
                            .build()
            );

            SlackMessageResponse messageResponse = new SlackMessageResponse();
            messageResponse.setTs(TEST_THREAD_TS);
            when(slackApiClient.postMessage(any(), any())).thenReturn(messageResponse);

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then - Should post thread anchor and error message
            verify(slackApiClient, times(1)).postMessage(any(), any());
            verify(slackApiClient, times(1)).postThreadReply(any(), any(), any());
        }

        @Test
        @DisplayName("Should handle service exception during async processing")
        void shouldHandleServiceExceptionDuringAsyncProcessing() throws Exception {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );
            LeaveIngestionCommand command = createMockCommand();
            RuntimeException serviceException = new RuntimeException("Service error");

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command);
            when(leaveService.ingest(any())).thenThrow(serviceException);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> errorMessage = new AtomicReference<>();
            doAnswer(invocation -> {
                SlackMessageRequest message = invocation.getArgument(2);
                errorMessage.set(message.getText());
                latch.countDown();
                return new SlackMessageResponse();
            }).when(slackApiClient).postThreadReply(any(), any(), any());

            // When
            orchestrator.processLeaveRequest(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then - Should handle exception gracefully
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(errorMessage.get()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle empty command text gracefully")
        void shouldHandleEmptyCommandTextGracefully() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("");

            SlackViewOpenResponse openResponse = new SlackViewOpenResponse();
            openResponse.setOk(true);
            when(slackApiClient.openModal(any(), any())).thenReturn(openResponse);

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then - Should still open modal
            verify(slackApiClient, times(1)).openModal(any(), any());
        }

        @Test
        @DisplayName("Should process multiple leave requests concurrently")
        void shouldProcessMultipleLeaveRequestsConcurrently() throws Exception {
            // Given
            LeaveIngestionRequest request1 = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );
            LeaveIngestionRequest request2 = createValidLeaveIngestionRequest(
                    LeaveType.OPTIONAL_HOLIDAY, LeaveDurationType.FIRST_HALF
            );

            LeaveIngestionCommand command1 = createMockCommand();
            LeaveIngestionCommand command2 = createMockCommand();
            LeaveDto mockDto = createMockLeaveDto();

            when(leaveMapper.toCommand(any(), any(), any())).thenReturn(command1, command2);
            when(leaveService.ingest(any())).thenReturn(mockDto);

            CountDownLatch latch = new CountDownLatch(2);
            doAnswer(invocation -> {
                latch.countDown();
                return new SlackMessageResponse();
            }).when(slackApiClient).postThreadReply(any(), any(), any());

            // When - Process concurrently
            orchestrator.processLeaveRequest(request1, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);
            orchestrator.processLeaveRequest(request2, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then - Both should complete
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            verify(leaveService, times(2)).ingest(any());
        }

        @Test
        @DisplayName("Should handle mapper throwing exception")
        void shouldHandleMapperThrowingException() throws Exception {
            // Given
            LeaveIngestionRequest leaveRequest = createValidLeaveIngestionRequest(
                    LeaveType.ANNUAL_LEAVE, LeaveDurationType.FULL_DAY
            );

            when(leaveMapper.toCommand(any(), any(), any()))
                    .thenThrow(new RuntimeException("Mapper error"));

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> errorMessage = new AtomicReference<>();
            doAnswer(invocation -> {
                SlackMessageRequest message = invocation.getArgument(2);
                errorMessage.set(message.getText());
                latch.countDown();
                return new SlackMessageResponse();
            }).when(slackApiClient).postThreadReply(any(), any(), any());

            // When
            orchestrator.processLeaveRequest(leaveRequest, TEST_CHANNEL_ID, TEST_THREAD_TS, TEST_USER_ID);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(errorMessage.get()).isNotEmpty();
        }

        @Test
        @DisplayName("Should validate thread timestamp format")
        void shouldValidateThreadTimestampFormat() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            String threadTs = "1234567890.123456"; // Valid Slack timestamp format

            SlackViewOpenResponse openResponse = new SlackViewOpenResponse();
            openResponse.setOk(true);

            ArgumentCaptor<SlackModalView> modalCaptor = ArgumentCaptor.forClass(SlackModalView.class);
            when(slackApiClient.openModal(any(), modalCaptor.capture())).thenReturn(openResponse);

            // When
            orchestrator.openLeaveApplicationModal(slackRequest, threadTs);

            Thread.sleep(500); // Wait for async processing

            // Then - Should complete without error
            verify(slackApiClient, times(1)).openModal(any(), any());

            SlackModalView capturedModal = modalCaptor.getValue();
            assertThat(capturedModal).isNotNull();
            assertThat(capturedModal.getPrivateMetadata()).contains(threadTs);
        }
    }

    @Nested
    @DisplayName("handleBlockAction Tests")
    class HandleBlockActionTests {

        @Test
        @DisplayName("Should update modal for ANNUAL_LEAVE selection")
        void shouldUpdateModalForAnnualLeaveSelection() throws Exception {
            // Given
            String requestBody = createBlockActionRequestBody("ANNUAL_LEAVE");

            // When
            orchestrator.handleBlockAction(requestBody);

            // Then
            ArgumentCaptor<SlackModalView> modalCaptor = ArgumentCaptor.forClass(SlackModalView.class);
            verify(slackApiClient).updateModal(eq(TEST_VIEW_ID), modalCaptor.capture(), eq("test-hash"));

            SlackModalView capturedModal = modalCaptor.getValue();
            assertThat(capturedModal).isNotNull();
            assertThat(capturedModal.getPrivateMetadata()).contains(TEST_THREAD_TS);
        }

        @Test
        @DisplayName("Should update modal for OPTIONAL_HOLIDAY selection")
        void shouldUpdateModalForOptionalHolidaySelection() throws Exception {
            // Given
            String requestBody = createBlockActionRequestBody("OPTIONAL_HOLIDAY");

            // When
            orchestrator.handleBlockAction(requestBody);

            // Then
            ArgumentCaptor<SlackModalView> modalCaptor = ArgumentCaptor.forClass(SlackModalView.class);
            verify(slackApiClient).updateModal(eq(TEST_VIEW_ID), modalCaptor.capture(), eq("test-hash"));

            SlackModalView capturedModal = modalCaptor.getValue();
            assertThat(capturedModal).isNotNull();
            assertThat(capturedModal.getPrivateMetadata()).contains(TEST_THREAD_TS);
            verify(optionalHolidayService).getAllHolidaysAsSlackOptions();
        }

        @Test
        @DisplayName("Should throw exception for null actions")
        void shouldThrowExceptionForNullActions() {
            // Given
            String requestBody = createBlockActionRequestBodyWithNullActions();

            // When & Then
            assertThatThrownBy(() -> orchestrator.handleBlockAction(requestBody))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No actions found");
        }

        @Test
        @DisplayName("Should throw exception for empty actions")
        void shouldThrowExceptionForEmptyActions() {
            // Given
            String requestBody = createBlockActionRequestBodyWithEmptyActions();

            // When & Then
            assertThatThrownBy(() -> orchestrator.handleBlockAction(requestBody))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No actions found");
        }

        @Test
        @DisplayName("Should log warning and return for unknown action_id")
        void shouldLogWarningAndReturnForUnknownActionId() throws Exception {
            // Given
            String requestBody = createBlockActionRequestBodyWithUnknownActionId("ANNUAL_LEAVE");

            // When
            orchestrator.handleBlockAction(requestBody);

            // Then
            verify(slackApiClient, never()).updateModal(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception for unknown leave type")
        void shouldThrowExceptionForUnknownLeaveType() {
            // Given
            String requestBody = createBlockActionRequestBody("UNKNOWN_LEAVE_TYPE");

            // When & Then
            assertThatThrownBy(() -> orchestrator.handleBlockAction(requestBody))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unknown leave type: UNKNOWN_LEAVE_TYPE");
        }
    }

    @Nested
    @DisplayName("Modal Builder Tests")
    class ModalBuilderTests {

        @Test
        @DisplayName("Should build Annual Leave modal with correct structure")
        void shouldBuildAnnualLeaveModalWithCorrectStructure() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();

            // When
            SlackModalView modal = orchestrator.buildAnnualLeaveModal(slackRequest, TEST_THREAD_TS);

            // Then
            assertThat(modal).isNotNull();
            assertThat(modal.getBlocks()).isNotNull();
            assertThat(modal.getBlocks()).hasSize(5); // Leave type, duration, start date, end date, reason
            assertThat(modal.getPrivateMetadata()).contains(TEST_THREAD_TS);
            assertThat(modal.getPrivateMetadata()).contains(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should build Annual Leave modal with thread context")
        void shouldBuildAnnualLeaveModalWithThreadContext() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();

            // When
            SlackModalView modal = orchestrator.buildAnnualLeaveModal(slackRequest, TEST_THREAD_TS);

            // Then
            assertThat(modal.getPrivateMetadata()).contains(TEST_THREAD_TS);
            assertThat(modal.getPrivateMetadata()).contains(TEST_CHANNEL_ID);
            assertThat(modal.getPrivateMetadata()).contains(TEST_CHANNEL_NAME);
        }

        @Test
        @DisplayName("Should build Optional Holiday modal with correct structure")
        void shouldBuildOptionalHolidayModalWithCorrectStructure() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            when(optionalHolidayService.getAllHolidaysAsSlackOptions())
                    .thenReturn(java.util.List.of());

            // When
            SlackModalView modal = orchestrator.buildOptionalHolidayModal(slackRequest, TEST_THREAD_TS);

            // Then
            assertThat(modal).isNotNull();
            assertThat(modal.getBlocks()).isNotNull();
            assertThat(modal.getBlocks()).hasSize(2); // Leave type, holiday dropdown only
            assertThat(modal.getPrivateMetadata()).contains(TEST_THREAD_TS);
        }

        @Test
        @DisplayName("Should build Optional Holiday modal without duration fields")
        void shouldBuildOptionalHolidayModalWithoutDurationFields() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            when(optionalHolidayService.getAllHolidaysAsSlackOptions())
                    .thenReturn(java.util.List.of());

            // When
            SlackModalView modal = orchestrator.buildOptionalHolidayModal(slackRequest, TEST_THREAD_TS);

            // Then
            assertThat(modal).isNotNull();
            // Verify that only 2 blocks exist (leave type and holiday dropdown)
            // Duration fields (FIRST_HALF, SECOND_HALF) and date pickers should NOT be present
            assertThat(modal.getBlocks()).hasSize(2);
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
        request.setStatus(LeaveStatus.REQUESTED);
        return request;
    }

    private LeaveIngestionRequest createValidLeaveIngestionRequest(LeaveType type, LeaveDurationType duration, LocalDate startDate, LocalDate endDate) {
        LeaveIngestionRequest request = new LeaveIngestionRequest();
        request.setUserId(TEST_USER_ID);
        request.setType(type);
        request.setDurationType(duration);
        request.setDateRange(new DateRange(startDate, endDate));
        request.setSourceType(SourceType.SLACK);
        request.setSourceId("slack-source-123");
        request.setStatus(LeaveStatus.REQUESTED);
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

        // Create employee instead of setting userId
        EmployeeDto employee = new EmployeeDto();
        employee.setId(UUID.randomUUID());
        employee.setName("Test User");
        employee.setSlackId(TEST_USER_ID);
        dto.setEmployee(employee);

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

    private String createBlockActionRequestBody(String selectedLeaveType) {
        try {
            // Create metadata object
            var metadata = Map.of(
                "userId", TEST_USER_ID,
                "channelId", TEST_CHANNEL_ID,
                "channelName", TEST_CHANNEL_NAME,
                "threadTs", TEST_THREAD_TS
            );
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // Build the block action payload
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", "block_actions");

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

            // Create container with view_id
            var container = new java.util.LinkedHashMap<String, Object>();
            container.put("type", "view");
            container.put("view_id", TEST_VIEW_ID);
            payload.put("container", container);

            // Create view with hash and metadata
            var view = new java.util.LinkedHashMap<String, Object>();
            view.put("id", TEST_VIEW_ID);
            view.put("team_id", "T12345");
            view.put("type", "modal");
            view.put("hash", "test-hash");
            view.put("private_metadata", metadataJson);
            payload.put("view", view);

            // Create action with selected option
            var action = new java.util.LinkedHashMap<String, Object>();
            action.put("action_id", "leave_type_category_action");
            action.put("block_id", "leave_type_category_block");
            action.put("type", "static_select");

            var selectedOption = Map.of(
                "text", Map.of("type", "plain_text", "text", selectedLeaveType),
                "value", selectedLeaveType
            );
            action.put("selected_option", selectedOption);

            payload.put("actions", java.util.List.of(action));

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return "payload=" + urlEncode(jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create block action payload", e);
        }
    }

    private String createBlockActionRequestBodyWithNullActions() {
        try {
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", "block_actions");

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

            var container = new java.util.LinkedHashMap<String, Object>();
            container.put("type", "view");
            container.put("view_id", TEST_VIEW_ID);
            payload.put("container", container);

            var view = new java.util.LinkedHashMap<String, Object>();
            view.put("id", TEST_VIEW_ID);
            view.put("team_id", "T12345");
            view.put("type", "modal");
            view.put("hash", "test-hash");
            view.put("private_metadata", "{}");
            payload.put("view", view);

            payload.put("actions", null);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return "payload=" + urlEncode(jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create block action payload with null actions", e);
        }
    }

    private String createBlockActionRequestBodyWithEmptyActions() {
        try {
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", "block_actions");

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

            var container = new java.util.LinkedHashMap<String, Object>();
            container.put("type", "view");
            container.put("view_id", TEST_VIEW_ID);
            payload.put("container", container);

            var view = new java.util.LinkedHashMap<String, Object>();
            view.put("id", TEST_VIEW_ID);
            view.put("team_id", "T12345");
            view.put("type", "modal");
            view.put("hash", "test-hash");
            view.put("private_metadata", "{}");
            payload.put("view", view);

            payload.put("actions", java.util.List.of());

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return "payload=" + urlEncode(jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create block action payload with empty actions", e);
        }
    }

    private String createBlockActionRequestBodyWithUnknownActionId(String selectedLeaveType) {
        try {
            // Create metadata object
            var metadata = Map.of(
                "userId", TEST_USER_ID,
                "channelId", TEST_CHANNEL_ID,
                "channelName", TEST_CHANNEL_NAME,
                "threadTs", TEST_THREAD_TS
            );
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // Build the block action payload
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", "block_actions");

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

            // Create container with view_id
            var container = new java.util.LinkedHashMap<String, Object>();
            container.put("type", "view");
            container.put("view_id", TEST_VIEW_ID);
            payload.put("container", container);

            // Create view with hash and metadata
            var view = new java.util.LinkedHashMap<String, Object>();
            view.put("id", TEST_VIEW_ID);
            view.put("team_id", "T12345");
            view.put("type", "modal");
            view.put("hash", "test-hash");
            view.put("private_metadata", metadataJson);
            payload.put("view", view);

            // Create action with UNKNOWN action_id
            var action = new java.util.LinkedHashMap<String, Object>();
            action.put("action_id", "unknown_action_id"); // Wrong action_id
            action.put("block_id", "leave_type_category_block");
            action.put("type", "static_select");

            var selectedOption = Map.of(
                "text", Map.of("type", "plain_text", "text", selectedLeaveType),
                "value", selectedLeaveType
            );
            action.put("selected_option", selectedOption);

            payload.put("actions", java.util.List.of(action));

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return "payload=" + urlEncode(jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create block action payload with unknown action_id", e);
        }
    }

    // Helper methods for new tests

    private SlackTeam createSlackTeam(String teamId) {
        return SlackTeam.builder()
                .id(teamId)
                .domain("example")
                .build();
    }

    private SlackUser createSlackUser(String userId) {
        return SlackUser.builder()
                .id(userId)
                .username("testuser")
                .name("Test User")
                .teamId("T12345")
                .build();
    }

    private SlackChannel createSlackChannel(String channelId) {
        return SlackChannel.builder()
                .id(channelId)
                .name("test-channel")
                .build();
    }

    private SlackMessage createSlackMessage() {
        return SlackMessage.builder()
                .type("message")
                .subtype("bot_message")
                .ts("1234567890.123456")
                .botId("B12345")
                .text("Test message")
                .build();
    }

    private LeaveIngestionCommand createValidLeaveIngestionCommand() {
        return LeaveIngestionCommand.builder()
                .sourceType(SourceType.SLACK)
                .sourceId("slack-req-12345")
                .userId(TEST_USER_ID)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.of(2025, 1, 15))
                        .endDate(LocalDate.of(2025, 1, 15))
                        .build())
                .durationType(LeaveDurationType.FULL_DAY)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();
    }

    private LeaveDto createLeaveDto() {
        EmployeeDto employee = EmployeeDto.builder()
                .id(UUID.randomUUID())
                .slackId(TEST_USER_ID)
                .build();

        return LeaveDto.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.of(2025, 1, 15))
                        .endDate(LocalDate.of(2025, 1, 15))
                        .build())
                .durationType(LeaveDurationType.FULL_DAY)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();
    }

    @Nested
    @DisplayName("Slash Command Text vs No-Text Tests")
    class SlashCommandTextTests {

        @BeforeEach
        void setUpForTextTests() {
            // No special setup needed - AI flow is triggered by presence of text in command
        }

        @Test
        @DisplayName("Should open modal immediately when no text provided")
        void shouldOpenModalWhenNoText() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText(""); // No text

            SlackMessageResponse response = new SlackMessageResponse();
            response.setOk(true);
            response.setTs("1234567890.123456");

            when(slackApiClient.openModal(any(), any()))
                    .thenReturn(SlackViewOpenResponse.builder()
                            .view(SlackViewOpenResponse.SlackViewResponse.builder().build())
                            .build());

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then
            verify(slackApiClient).openModal(eq(slackRequest.getTriggerId()), any());
        }

        @Test
        @DisplayName("Should trigger AI flow when text is provided")
        void shouldTriggerAiFlowWhenTextProvided() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("tomorrow");

            SlackMessageResponse anchorResponse = new SlackMessageResponse();
            anchorResponse.setOk(true);
            anchorResponse.setChannel(TEST_CHANNEL_ID);
            anchorResponse.setTs("1234567890.123456");

            when(slackApiClient.postMessage(eq(TEST_CHANNEL_ID), any()))
                    .thenReturn(anchorResponse);

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then
            verify(slackApiClient).postMessage(eq(TEST_CHANNEL_ID), any());
        }

        @Test
        @DisplayName("Should post anchor message before AI parsing")
        void shouldPostAnchorMessageBeforeAiParsing() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("annual leave next week");

            SlackMessageResponse response = new SlackMessageResponse();
            response.setOk(true);
            response.setChannel(TEST_CHANNEL_ID);
            response.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(eq(TEST_CHANNEL_ID), any()))
                    .thenReturn(response);

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then
            verify(slackApiClient).postMessage(eq(TEST_CHANNEL_ID), any());
        }

        @Test
        @DisplayName("Should use AI when leave type is specified in text")
        void shouldUseAiWhenLeaveTypeSpecified() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("annual leave");

            SlackMessageResponse response = new SlackMessageResponse();
            response.setOk(true);
            response.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(eq(TEST_CHANNEL_ID), any()))
                    .thenReturn(response);

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then
            verify(slackApiClient).postMessage(eq(TEST_CHANNEL_ID), any());
        }

        @Test
        @DisplayName("Should use AI when dates are specified in text")
        void shouldUseAiWhenDatesSpecified() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("2024-01-15 2024-01-16");

            SlackMessageResponse response = new SlackMessageResponse();
            response.setOk(true);
            response.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(eq(TEST_CHANNEL_ID), any()))
                    .thenReturn(response);

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then
            verify(slackApiClient).postMessage(eq(TEST_CHANNEL_ID), any());
        }

        @Test
        @DisplayName("Should use AI when natural language date is used")
        void shouldUseAiWithNaturalLanguageDate() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("tomorrow through Friday");

            SlackMessageResponse response = new SlackMessageResponse();
            response.setOk(true);
            response.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(eq(TEST_CHANNEL_ID), any()))
                    .thenReturn(response);

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then
            verify(slackApiClient).postMessage(eq(TEST_CHANNEL_ID), any());
        }

        @Test
        @DisplayName("Should handle empty text as no text")
        void shouldHandleEmptyTextAsNoText() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("");
            SlackMessageResponse messageResponse = new SlackMessageResponse();
            messageResponse.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(any(), any())).thenReturn(messageResponse);
            when(slackApiClient.openModal(any(), any()))
                    .thenReturn(SlackViewOpenResponse.builder()
                            .view(SlackViewOpenResponse.SlackViewResponse.builder().build())
                            .build());

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then
            // Should post thread anchor message first
            verify(slackApiClient).postMessage(eq(slackRequest.getChannelId()), any());
            // Should open modal with thread timestamp from anchor message
            verify(slackApiClient).openModal(eq(slackRequest.getTriggerId()), any());
        }

        @Test
        @DisplayName("Should handle whitespace-only text as no text")
        void shouldHandleWhitespaceOnlyTextAsNoText() {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            slackRequest.setText("   ");
            SlackMessageResponse messageResponse = new SlackMessageResponse();
            messageResponse.setTs(TEST_THREAD_TS);

            when(slackApiClient.postMessage(any(), any())).thenReturn(messageResponse);
            when(slackApiClient.openModal(any(), any()))
                    .thenReturn(SlackViewOpenResponse.builder()
                            .view(SlackViewOpenResponse.SlackViewResponse.builder().build())
                            .build());

            // When
            orchestrator.handleSlashCommand(slackRequest);

            // Then
            // Should post thread anchor message first
            verify(slackApiClient).postMessage(eq(slackRequest.getChannelId()), any());
            // Should open modal with thread timestamp from anchor message
            verify(slackApiClient).openModal(eq(slackRequest.getTriggerId()), any());
        }
    }

    @Nested
    @DisplayName("Modal Builder with Data Tests")
    class ModalBuilderWithDataTests {

        @Test
        @DisplayName("Should build annual leave modal with parsed data")
        void shouldBuildAnnualLeaveModalWithData() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            String threadTs = "1234567890.123456";

            ParsedLeaveRequest parsedRequest = ParsedLeaveRequest.builder()
                    .startDate(java.time.LocalDate.of(2025, 1, 15))
                    .endDate(java.time.LocalDate.of(2025, 1, 15))
                    .durationType(LeaveDurationType.FULL_DAY)
                    .leaveType(LeaveType.ANNUAL_LEAVE)
                    .reason("Test reason")
                    .build();

            // When
            java.lang.reflect.Method method = SlackLeaveOrchestrator.class.getDeclaredMethod(
                    "buildAnnualLeaveModalWithData", SlackCommandRequest.class, String.class, ParsedLeaveRequest.class);
            method.setAccessible(true);
            SlackModalView modalView = (SlackModalView) method.invoke(orchestrator, slackRequest, threadTs, parsedRequest);

            // Then
            assertThat(modalView).isNotNull();
            assertThat(modalView.getTitle()).isNotNull();
        }

        @Test
        @DisplayName("Should build optional holiday modal with parsed data")
        void shouldBuildOptionalHolidayModalWithData() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            String threadTs = "1234567890.123456";

            ParsedLeaveRequest parsedRequest = ParsedLeaveRequest.builder()
                    .startDate(java.time.LocalDate.of(2025, 1, 15))
                    .endDate(java.time.LocalDate.of(2025, 1, 15))
                    .durationType(LeaveDurationType.FULL_DAY)
                    .leaveType(LeaveType.OPTIONAL_HOLIDAY)
                    .reason("Optional holiday")
                    .optionalHolidayName("Special Holiday")
                    .build();

            // When
            java.lang.reflect.Method method = SlackLeaveOrchestrator.class.getDeclaredMethod(
                    "buildOptionalHolidayModalWithData", SlackCommandRequest.class, String.class, ParsedLeaveRequest.class);
            method.setAccessible(true);
            SlackModalView modalView = (SlackModalView) method.invoke(orchestrator, slackRequest, threadTs, parsedRequest);

            // Then
            assertThat(modalView).isNotNull();
            assertThat(modalView.getTitle()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Confirmation Dialog Tests")
    class ConfirmationDialogTests {

        @Test
        @DisplayName("Should show confirmation dialog")
        void shouldShowConfirmationDialog() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            String threadTs = "1234567890.123456";

            ParsedLeaveRequest parsedRequest = ParsedLeaveRequest.builder()
                    .startDate(java.time.LocalDate.of(2025, 1, 15))
                    .endDate(java.time.LocalDate.of(2025, 1, 15))
                    .durationType(LeaveDurationType.FULL_DAY)
                    .leaveType(LeaveType.ANNUAL_LEAVE)
                    .reason("Test reason")
                    .build();

            ParseResult parseResult = ParseResult.builder()
                    .parsedRequest(parsedRequest)
                    .isSuccess(true)
                    .confidenceScore(0.95)
                    .build();

            // When
            java.lang.reflect.Method method = SlackLeaveOrchestrator.class.getDeclaredMethod(
                    "showConfirmationDialog", SlackCommandRequest.class, ParseResult.class, String.class);
            method.setAccessible(true);
            method.invoke(orchestrator, slackRequest, parseResult, threadTs);

            // Then
            verify(slackApiClient).postThreadReply(eq(TEST_CHANNEL_ID), eq(threadTs), any(SlackMessageRequest.class));
        }

        @Test
        @DisplayName("Should show confirmation dialog via response URL")
        void shouldShowConfirmationDialogViaResponseUrl() throws Exception {
            // Given
            SlackCommandRequest slackRequest = createValidSlackCommandRequest();
            String responseUrl = TEST_RESPONSE_URL;

            ParsedLeaveRequest parsedRequest = ParsedLeaveRequest.builder()
                    .startDate(java.time.LocalDate.of(2025, 1, 15))
                    .endDate(java.time.LocalDate.of(2025, 1, 15))
                    .durationType(LeaveDurationType.FULL_DAY)
                    .leaveType(LeaveType.ANNUAL_LEAVE)
                    .reason("Test reason")
                    .build();

            ParseResult parseResult = ParseResult.builder()
                    .parsedRequest(parsedRequest)
                    .isSuccess(true)
                    .confidenceScore(0.95)
                    .build();

            // When
            java.lang.reflect.Method method = SlackLeaveOrchestrator.class.getDeclaredMethod(
                    "showConfirmationDialogViaResponseUrl", SlackCommandRequest.class, ParseResult.class, String.class);
            method.setAccessible(true);
            method.invoke(orchestrator, slackRequest, parseResult, responseUrl);

            // Then
            verify(slackApiClient).sendViaResponseUrl(eq(responseUrl), any(SlackMessageRequest.class));
        }
    }

    @Nested
    @DisplayName("Message Update Tests")
    class MessageUpdateTests {

        @Test
        @DisplayName("Should update confirmation message successfully")
        void shouldUpdateConfirmationMessage() throws Exception {
            // Given
            String channelId = TEST_CHANNEL_ID;
            String messageTs = "1234567890.123456";
            SlackMessageRequest message = SlackMessageRequest.builder()
                    .text("Updated message")
                    .build();

            when(slackApiClient.updateMessage(eq(channelId), eq(messageTs), any(SlackMessageRequest.class)))
                    .thenReturn(new SlackMessageResponse());

            // When
            java.lang.reflect.Method method = SlackLeaveOrchestrator.class.getDeclaredMethod(
                    "updateConfirmationMessage", String.class, String.class, SlackMessageRequest.class);
            method.setAccessible(true);
            method.invoke(orchestrator, channelId, messageTs, message);

            // Then
            verify(slackApiClient).updateMessage(eq(channelId), eq(messageTs), any(SlackMessageRequest.class));
        }

        @Test
        @DisplayName("Should build confirmation message with all details")
        void shouldBuildConfirmationMessage() throws Exception {
            // Given
            ParsedLeaveRequest request = ParsedLeaveRequest.builder()
                    .startDate(java.time.LocalDate.of(2025, 1, 15))
                    .endDate(java.time.LocalDate.of(2025, 1, 15))
                    .durationType(LeaveDurationType.FULL_DAY)
                    .leaveType(LeaveType.ANNUAL_LEAVE)
                    .reason("Test reason for leave")
                    .build();

            // When
            java.lang.reflect.Method method = SlackLeaveOrchestrator.class.getDeclaredMethod(
                    "buildConfirmationMessage", ParsedLeaveRequest.class);
            method.setAccessible(true);
            String message = (String) method.invoke(orchestrator, request);

            // Then
            assertThat(message).isNotNull();
            assertThat(message).contains("*Leave Request Details*");
            assertThat(message).contains("Jan 15, 2025");
            assertThat(message).contains("Test reason for leave");
        }
    }
}
