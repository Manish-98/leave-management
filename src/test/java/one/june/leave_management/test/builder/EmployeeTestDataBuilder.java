package one.june.leave_management.test.builder;

import one.june.leave_management.application.employee.command.EmployeeCreateCommand;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.domain.employee.model.Employee;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builder utility for creating test data objects for Employee tests.
 * Provides convenient methods to build test entities with sensible defaults.
 */
public class EmployeeTestDataBuilder {

    /**
     * Creates a default Employee with sensible defaults.
     */
    public static Employee.EmployeeBuilder defaultEmployee() {
        return Employee.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .slackId("U12345")
                .googleId("john.doe@example.com")
                .slackDisplayName("John")
                .dateOfJoining(LocalDate.of(2020, 1, 1))
                .active(true)
                .carryForwardLeaves(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    /**
     * Creates an employee with Slack ID only.
     */
    public static Employee.EmployeeBuilder withSlackIdOnly() {
        return defaultEmployee()
                .googleId(null);
    }

    /**
     * Creates an employee with Google ID only.
     */
    public static Employee.EmployeeBuilder withGoogleIdOnly() {
        return defaultEmployee()
                .slackId(null);
    }

    /**
     * Creates an employee with specific external IDs.
     */
    public static Employee.EmployeeBuilder withExternalIds(String slackId, String googleId) {
        return defaultEmployee()
                .slackId(slackId)
                .googleId(googleId);
    }

    /**
     * Creates an employee with carry forward leaves.
     */
    public static Employee.EmployeeBuilder withCarryForwardLeaves(Map<Integer, Integer> leaves) {
        return defaultEmployee()
                .carryForwardLeaves(new HashMap<>(leaves));
    }

    /**
     * Creates an employee with carry forward leave for a specific year.
     */
    public static Employee.EmployeeBuilder withCarryForwardForYear(int year, int days) {
        Map<Integer, Integer> leaves = new HashMap<>();
        leaves.put(year, days);
        return defaultEmployee()
                .carryForwardLeaves(leaves);
    }

    /**
     * Creates an inactive employee.
     */
    public static Employee.EmployeeBuilder inactiveEmployee() {
        return defaultEmployee()
                .active(false);
    }

    /**
     * Creates an employee with a specific name.
     */
    public static Employee.EmployeeBuilder withName(String name) {
        return defaultEmployee()
                .name(name);
    }

    /**
     * Creates an employee with a specific ID.
     */
    public static Employee.EmployeeBuilder withId(UUID id) {
        return defaultEmployee()
                .id(id);
    }

    /**
     * Creates an employee with specific date of joining.
     */
    public static Employee.EmployeeBuilder withDateOfJoining(LocalDate dateOfJoining) {
        return defaultEmployee()
                .dateOfJoining(dateOfJoining);
    }

    /**
     * Creates a default EmployeeCreateCommand with sensible defaults.
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder defaultCommand() {
        return EmployeeCreateCommand.builder()
                .name("John Doe")
                .slackId("U12345")
                .googleId("john.doe@example.com")
                .slackDisplayName("John")
                .dateOfJoining(LocalDate.of(2020, 1, 1))
                .active(true)
                .carryForwardLeaves(new HashMap<>());
    }

    /**
     * Creates a command with Slack ID only.
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder commandWithSlackIdOnly() {
        return defaultCommand()
                .googleId(null);
    }

    /**
     * Creates a command with Google ID only.
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder commandWithGoogleIdOnly() {
        return defaultCommand()
                .slackId(null);
    }

    /**
     * Creates a command for updating (includes ID).
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder updateCommand(UUID id) {
        return defaultCommand()
                .id(id);
    }

    /**
     * Creates a command with carry forward leaves.
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder commandWithCarryForwardLeaves(
            Map<Integer, Integer> leaves) {
        return defaultCommand()
                .carryForwardLeaves(new HashMap<>(leaves));
    }

    /**
     * Creates a command with carry forward leave for a specific year.
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder commandWithCarryForwardForYear(
            int year, int days) {
        Map<Integer, Integer> leaves = new HashMap<>();
        leaves.put(year, days);
        return defaultCommand()
                .carryForwardLeaves(leaves);
    }

    /**
     * Creates a default EmployeeDto with sensible defaults.
     */
    public static EmployeeDto.EmployeeDtoBuilder defaultDto() {
        return EmployeeDto.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .slackId("U12345")
                .googleId("john.doe@example.com")
                .slackDisplayName("John")
                .dateOfJoining(LocalDate.of(2020, 1, 1))
                .active(true)
                .carryForwardLeaves(new HashMap<>());
    }

    /**
     * Creates a minimal valid employee (required fields only).
     */
    public static Employee minimalEmployee() {
        return Employee.create(
                "Jane Smith",
                "U67890",
                null,
                "Jane",
                LocalDate.of(2021, 6, 15)
        );
    }

    /**
     * Creates a minimal employee with Google ID only.
     */
    public static Employee minimalEmployeeWithGoogle() {
        return Employee.create(
                "Jane Smith",
                null,
                "jane.smith@example.com",
                "Jane",
                LocalDate.of(2021, 6, 15)
        );
    }

    /**
     * Creates an employee with multiple years of carry forward leaves.
     */
    public static Employee employeeWithMultipleCarryForwardYears() {
        Map<Integer, Integer> leaves = new HashMap<>();
        leaves.put(2022, 5);
        leaves.put(2023, 3);
        leaves.put(2024, 2);

        Employee employee = Employee.create(
                "Bob Johnson",
                "U11111",
                "bob@example.com",
                "Bob",
                LocalDate.of(2019, 3, 10)
        );
        employee.setCarryForwardLeaves(leaves);
        return employee;
    }

    /**
     * Creates a command with a specific name.
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder commandWithName(String name) {
        return defaultCommand()
                .name(name);
    }

    /**
     * Creates a command with null name (for validation tests).
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder commandWithNullName() {
        return defaultCommand()
                .name(null);
    }

    /**
     * Creates a command with empty name (for validation tests).
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder commandWithEmptyName() {
        return defaultCommand()
                .name("");
    }

    /**
     * Creates a command with no external IDs (for validation tests).
     */
    public static EmployeeCreateCommand.EmployeeCreateCommandBuilder commandWithNoExternalIds() {
        return EmployeeCreateCommand.builder()
                .name("John Doe")
                .slackId(null)
                .googleId(null)
                .slackDisplayName("John")
                .dateOfJoining(LocalDate.of(2020, 1, 1))
                .active(true)
                .carryForwardLeaves(new HashMap<>());
    }

    /**
     * Creates an employee with future date of joining (for validation tests).
     */
    public static Employee.EmployeeBuilder withFutureDateOfJoining() {
        return defaultEmployee()
                .dateOfJoining(LocalDate.now().plusDays(1));
    }

    /**
     * Creates an employee with null date of joining (for validation tests).
     */
    public static Employee.EmployeeBuilder withNullDateOfJoining() {
        return defaultEmployee()
                .dateOfJoining(null);
    }
}
