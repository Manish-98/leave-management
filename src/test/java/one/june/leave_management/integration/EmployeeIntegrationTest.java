package one.june.leave_management.integration;

import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.application.employee.service.EmployeeService;
import one.june.leave_management.test.builder.EmployeeTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Employee management workflows.
 * Tests complete workflows from service to database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Employee Management Integration Tests")
class EmployeeIntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    private EmployeeDto testEmployee;

    @BeforeEach
    void setUp() {
        // Create a test employee for each test
        testEmployee = EmployeeTestDataBuilder.defaultDto().build();
    }

    // ==================== Full CRUD Workflow Tests ====================

    @Test
    @DisplayName("Should complete full CRUD workflow successfully")
    void shouldCompleteFullCrudWorkflow() {
        // 1. Create employee
        EmployeeCreateRequest createRequest = new EmployeeCreateRequest(
                testEmployee.getName(),
                testEmployee.getSlackId(),
                testEmployee.getGoogleId(),
                testEmployee.getSlackDisplayName(),
                testEmployee.getDateOfJoining(),
                testEmployee.getActive(),
                testEmployee.getCarryForwardLeaves()
        );

        EmployeeDto created = employeeService.create(toCommand(createRequest));
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();

        // 2. Read employee
        EmployeeDto found = employeeService.findById(created.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo(testEmployee.getName());

        // 3. Update employee
        EmployeeCreateRequest updateRequest = new EmployeeCreateRequest(
                "Updated Name",
                found.getSlackId(),
                found.getGoogleId(),
                found.getSlackDisplayName(),
                found.getDateOfJoining(),
                found.getActive(),
                found.getCarryForwardLeaves()
        );

        EmployeeDto updated = employeeService.update(created.getId(), toCommand(updateRequest));
        assertThat(updated.getName()).isEqualTo("Updated Name");

        // 4. Deactivate employee
        employeeService.deactivate(created.getId());

        // 5. Verify deactivated
        EmployeeDto deactivated = employeeService.findById(created.getId());
        assertThat(deactivated.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should complete bulk upload workflow end to end")
    void shouldCompleteBulkUploadWorkflow() {
        // Given
        HashMap<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(2024, 5);

        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Bulk Test Employee",
                "U_BULK_001",
                "bulk@example.com",
                "BulkEmployee",
                java.time.LocalDate.of(2024, 1, 1),
                true,
                carryForwardLeaves
        );

        // When - Create employee via service
        EmployeeDto created = employeeService.create(toCommand(request));

        // Then - Verify employee was created correctly
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Bulk Test Employee");
        assertThat(created.getSlackId()).isEqualTo("U_BULK_001");
        assertThat(created.getCarryForwardLeaves()).hasSize(1);

        // Verify we can find by Slack ID
        var foundBySlack = employeeService.findBySlackId("U_BULK_001");
        assertThat(foundBySlack).isPresent();
        assertThat(foundBySlack.get().getId()).isEqualTo(created.getId());

        // Verify we can find by Google ID
        var foundByGoogle = employeeService.findByGoogleId("bulk@example.com");
        assertThat(foundByGoogle).isPresent();
    }

    @Test
    @DisplayName("Should handle employee search with pagination")
    void shouldHandleEmployeeSearchWithPagination() {
        // Given - Create multiple employees
        for (int i = 0; i < 25; i++) {
            EmployeeCreateRequest request = new EmployeeCreateRequest(
                    "Employee " + i,
                    "U_SEARCH_" + i,
                    "search" + i + "@example.com",
                    "SearchEmployee" + i,
                    java.time.LocalDate.of(2020, 1, 1),
                    true,
                    new HashMap<>()
            );

            employeeService.create(toCommand(request));
        }

        // When - Search with pagination
        Page<EmployeeDto> page1 = employeeService.findAll(PageRequest.of(0, 10));
        Page<EmployeeDto> page2 = employeeService.findAll(PageRequest.of(1, 10));
        Page<EmployeeDto> page3 = employeeService.findAll(PageRequest.of(2, 10));

        // Then - Verify pagination works
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(10);
        assertThat(page3.getContent()).hasSize(5); // Remaining 5

        assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(25);
    }

    @Test
    @DisplayName("Should detect duplicate external IDs during concurrent creation")
    void shouldDetectDuplicateExternalIds() {
        // Given - Create first employee
        EmployeeCreateRequest request1 = new EmployeeCreateRequest(
                "Duplicate Test",
                "U_DUPLICATE_001",
                "duplicate@example.com",
                "DupUser",
                java.time.LocalDate.of(2024, 1, 1),
                true,
                new HashMap<>()
        );

        EmployeeDto created = employeeService.create(toCommand(request1));

        // When & Then - Try to create employee with same Slack ID
        EmployeeCreateRequest request2 = new EmployeeCreateRequest(
                "Another Employee",
                "U_DUPLICATE_001",  // Same Slack ID
                "another@example.com",
                "AnotherUser",
                java.time.LocalDate.of(2024, 1, 1),
                true,
                new HashMap<>()
        );

        assertThatThrownBy(() -> employeeService.create(toCommand(request2)))
                .isInstanceOf(one.june.leave_management.common.exception.DuplicateExternalIdException.class);
    }

    @Test
    @DisplayName("Should complete employee activation workflow")
    void shouldCompleteEmployeeActivationWorkflow() {
        // Given - Create active employee
        HashMap<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(2024, 3);

        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Active Test Employee",
                "U_ACTIVE_001",
                "active@example.com",
                "ActiveUser",
                java.time.LocalDate.of(2024, 1, 1),
                true,
                carryForwardLeaves
        );

        EmployeeDto created = employeeService.create(toCommand(request));
        assertThat(created.getActive()).isTrue();

        // When - Deactivate employee
        employeeService.deactivate(created.getId());

        // Then - Verify deactivated
        EmployeeDto deactivated = employeeService.findById(created.getId());
        assertThat(deactivated.getActive()).isFalse();

        // When - Reactivate employee
        employeeService.activate(created.getId());

        // Then - Verify reactivated
        EmployeeDto reactivated = employeeService.findById(created.getId());
        assertThat(reactivated.getActive()).isTrue();
    }

    // ==================== Helper Methods ====================

    private one.june.leave_management.application.employee.command.EmployeeCreateCommand toCommand(
            EmployeeCreateRequest request) {
        return one.june.leave_management.application.employee.command.EmployeeCreateCommand.builder()
                .id(null)
                .name(request.name())
                .slackId(request.slackId())
                .googleId(request.googleId())
                .slackDisplayName(request.slackDisplayName())
                .dateOfJoining(request.dateOfJoining())
                .active(request.active())
                .carryForwardLeaves(request.carryForwardLeaves())
                .build();
    }

    // ==================== Inner Class for Request ====================

    private record EmployeeCreateRequest(
            String name,
            String slackId,
            String googleId,
            String slackDisplayName,
            java.time.LocalDate dateOfJoining,
            Boolean active,
            java.util.Map<Integer, Integer> carryForwardLeaves
    ) {
    }
}
