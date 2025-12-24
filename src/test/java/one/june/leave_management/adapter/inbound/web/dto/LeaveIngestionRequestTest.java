package one.june.leave_management.adapter.inbound.web.dto;

import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LeaveIngestionRequestTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Test
    void validRequestShouldPassValidation() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .userId("user-456")
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should not have violations");
    }

    @Test
    void missingRequiredFieldsShouldFailValidation() {
        LeaveIngestionRequest request = LeaveIngestionRequest.builder().build();

        Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
        assertEquals(6, violations.size(), "Should have 6 violations for missing required fields");

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("sourceType") &&
                        v.getMessage().equals("Source type is required")));

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("sourceId") &&
                        v.getMessage().equals("Source ID is required")));

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("userId") &&
                        v.getMessage().equals("User ID is required")));

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("dateRange") &&
                        v.getMessage().equals("Date range is required")));

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("type") &&
                        v.getMessage().equals("Leave type is required")));

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("status") &&
                        v.getMessage().equals("Leave status is required")));
    }

    @Test
    void endDateBeforeStartDateShouldFailValidation() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .userId("user-456")
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        ConstraintViolation<LeaveIngestionRequest> violation = violations.iterator().next();
        assertEquals("End date must be after or equal to start date", violation.getMessage());
    }

    @Test
    void sourceIdTooLongShouldFailValidation() {
        String longSourceId = "a".repeat(101); // Exceeds 100 character limit

        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId(longSourceId)
                .userId("user-456")
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        ConstraintViolation<LeaveIngestionRequest> violation = violations.iterator().next();
        assertEquals("sourceId", violation.getPropertyPath().toString());
        assertEquals("Source ID must not exceed 100 characters", violation.getMessage());
    }

    @Test
    void userIdTooLongShouldFailValidation() {
        String longUserId = "a".repeat(51); // Exceeds 50 character limit

        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("source-123")
                .userId(longUserId)
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        ConstraintViolation<LeaveIngestionRequest> violation = violations.iterator().next();
        assertEquals("userId", violation.getPropertyPath().toString());
        assertEquals("User ID must not exceed 50 characters", violation.getMessage());
    }

    @Test
    void blankStringsShouldFailValidation() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                .sourceType(SourceType.WEB)
                .sourceId("   ")  // Blank string
                .userId("")        // Empty string
                .dateRange(dateRange)
                .type(LeaveType.ANNUAL_LEAVE)
                .status(LeaveStatus.REQUESTED)
                .build();

        Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("sourceId") &&
                        v.getMessage().equals("Source ID is required")));

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("userId") &&
                        v.getMessage().equals("User ID is required")));
    }

    @Test
    void validRequestWithAllSourceTypes() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        for (SourceType sourceType : SourceType.values()) {
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(sourceType)
                    .sourceId("source-123")
                    .userId("user-456")
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .build();

            Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Request with source type " + sourceType + " should be valid");
        }
    }

    @Test
    void validRequestWithAllLeaveTypes() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        for (LeaveType leaveType : LeaveType.values()) {
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("source-123")
                    .userId("user-456")
                    .dateRange(dateRange)
                    .type(leaveType)
                    .status(LeaveStatus.REQUESTED)
                    .build();

            Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Request with leave type " + leaveType + " should be valid");
        }
    }

    @Test
    void validRequestWithAllLeaveStatuses() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        for (LeaveStatus leaveStatus : LeaveStatus.values()) {
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("source-123")
                    .userId("user-456")
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(leaveStatus)
                    .build();

            Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Request with leave status " + leaveStatus + " should be valid");
        }
    }

    @Test
    void validRequestWithAllDurationTypes() {
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        for (one.june.leave_management.domain.leave.model.LeaveDurationType durationType :
                one.june.leave_management.domain.leave.model.LeaveDurationType.values()) {
            LeaveIngestionRequest request = LeaveIngestionRequest.builder()
                    .sourceType(SourceType.WEB)
                    .sourceId("source-123")
                    .userId("user-456")
                    .dateRange(dateRange)
                    .type(LeaveType.ANNUAL_LEAVE)
                    .status(LeaveStatus.REQUESTED)
                    .durationType(durationType)
                    .build();

            Set<ConstraintViolation<LeaveIngestionRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Request with duration type " + durationType + " should be valid");
        }
    }
}
