package one.june.leave_management.domain.leave.validation;

import one.june.leave_management.common.model.DateRange;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveSourceRef;
import one.june.leave_management.domain.leave.model.LeaveStatus;
import one.june.leave_management.domain.leave.model.LeaveType;
import one.june.leave_management.domain.leave.model.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveValidationStrategyBase - User ID Validation Tests")
class LeaveValidationStrategyBaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private TestValidationStrategy strategy;

    private UUID validEmployeeId = UUID.randomUUID();
    private String nonExistentEmployeeId = "123e4567-e89b-12d3-a456-426614174999";

    @BeforeEach
    void setUp() {
        strategy = new TestValidationStrategy(employeeRepository);
    }

    @Test
    @DisplayName("Should validate successfully when employee exists with UUID")
    void shouldValidateWhenEmployeeExistsWithSlackId() {
        // Given
        Leave leave = createValidLeave(validEmployeeId.toString());

        when(employeeRepository.findById(validEmployeeId))
                .thenReturn(Optional.of(one.june.leave_management.domain.employee.model.Employee.builder().id(validEmployeeId).build()));

        // When
        LeaveValidationResult result = strategy.validateBasicRequirements(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should validate successfully when employee exists with UUID")
    void shouldValidateWhenEmployeeExistsWithGoogleId() {
        // Given
        UUID anotherEmployeeId = UUID.randomUUID();
        Leave leave = createValidLeave(anotherEmployeeId.toString());

        when(employeeRepository.findById(anotherEmployeeId))
                .thenReturn(Optional.of(one.june.leave_management.domain.employee.model.Employee.builder().id(anotherEmployeeId).build()));

        // When
        LeaveValidationResult result = strategy.validateBasicRequirements(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when employee does not exist")
    void shouldFailValidationWhenEmployeeNotExists() {
        // Given
        Leave leave = createValidLeave(nonExistentEmployeeId);

        when(employeeRepository.findById(UUID.fromString(nonExistentEmployeeId)))
                .thenReturn(Optional.empty());

        // When
        LeaveValidationResult result = strategy.validateBasicRequirements(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .startsWith("Employee not found with ID: ")
                .contains(nonExistentEmployeeId);
    }

    @Test
    @DisplayName("Should validate successfully when userId is null")
    void shouldValidateWhenUserIdIsNull() {
        // Given
        Leave leave = createLeaveWithNullUserId();

        // When
        LeaveValidationResult result = strategy.validateBasicRequirements(leave);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when leave is null")
    void shouldFailValidationWhenLeaveIsNull() {
        // Given
        Leave leave = null;

        // When
        LeaveValidationResult result = strategy.validateBasicRequirements(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Leave cannot be null");
    }

    @Test
    @DisplayName("Should validate basic requirements before checking employee existence")
    void shouldValidateBasicRequirementsBeforeEmployeeCheck() {
        // Given - leave with null status should fail before employee check
        Leave leave = Leave.builder()
                .userId(validEmployeeId.toString())
                .type(LeaveType.ANNUAL_LEAVE)
                .durationType(LeaveDurationType.FULL_DAY)
                .status(null) // Invalid: null status
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .build();

        // When
        LeaveValidationResult result = strategy.validateBasicRequirements(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Leave status cannot be null");
    }

    @Test
    @DisplayName("Should check employee existence for all other validations passing")
    void shouldCheckEmployeeExistenceWhenOtherValidationsPass() {
        // Given - all basic validations pass, but employee doesn't exist
        Leave leave = Leave.builder()
                .userId(nonExistentEmployeeId)
                .type(LeaveType.ANNUAL_LEAVE)
                .durationType(LeaveDurationType.FULL_DAY)
                .status(LeaveStatus.REQUESTED)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .sourceRefs(List.of(createSourceRef()))
                .build();

        when(employeeRepository.findById(UUID.fromString(nonExistentEmployeeId)))
                .thenReturn(Optional.empty());

        // When
        LeaveValidationResult result = strategy.validateBasicRequirements(leave);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
                .startsWith("Employee not found with ID: ")
                .contains(nonExistentEmployeeId);
    }

    // Helper methods

    private Leave createValidLeave(String userId) {
        return Leave.builder()
                .userId(userId)
                .type(LeaveType.ANNUAL_LEAVE)
                .durationType(LeaveDurationType.FULL_DAY)
                .status(LeaveStatus.REQUESTED)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .sourceRefs(List.of(createSourceRef()))
                .build();
    }

    private Leave createLeaveWithNullUserId() {
        return Leave.builder()
                .userId(null)
                .type(LeaveType.ANNUAL_LEAVE)
                .durationType(LeaveDurationType.FULL_DAY)
                .status(LeaveStatus.REQUESTED)
                .dateRange(DateRange.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build())
                .sourceRefs(List.of(createSourceRef()))
                .build();
    }

    private LeaveSourceRef createSourceRef() {
        return LeaveSourceRef.builder()
                .sourceType(SourceType.SLACK)
                .sourceId("source-123")
                .build();
    }

    /**
     * Test implementation of LeaveValidationStrategyBase for testing purposes
     */
    private static class TestValidationStrategy extends LeaveValidationStrategyBase {

        protected TestValidationStrategy(EmployeeRepository employeeRepository) {
            super(employeeRepository);
        }

        @Override
        public LeaveType getType() {
            return LeaveType.ANNUAL_LEAVE;
        }

        @Override
        public LeaveValidationResult validate(Leave leave) {
            return LeaveValidationResult.success();
        }
    }
}
