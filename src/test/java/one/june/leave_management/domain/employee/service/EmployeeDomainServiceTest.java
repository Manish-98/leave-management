package one.june.leave_management.domain.employee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import one.june.leave_management.common.exception.DuplicateExternalIdException;
import one.june.leave_management.domain.employee.model.Employee;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.test.builder.EmployeeTestDataBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmployeeDomainService.
 * Tests the domain service's validation logic for employee business rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeDomainService Tests")
class EmployeeDomainServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeDomainService employeeDomainService;

    private Employee testEmployee;
    private UUID existingEmployeeId;

    @BeforeEach
    void setUp() {
        existingEmployeeId = UUID.randomUUID();
        testEmployee = EmployeeTestDataBuilder.defaultEmployee()
                .id(existingEmployeeId)
                .slackId("U12345")
                .googleId("john.doe@example.com")
                .build();
    }

    // ==================== validateExternalIdUniqueness Tests ====================

    @Test
    @DisplayName("Should pass validation for new employee with unique Slack ID")
    void testValidateExternalIdUniqueness_NewEmployee_Success() {
        // Arrange
        Employee newEmployee = EmployeeTestDataBuilder.minimalEmployee();
        when(employeeRepository.existsBySlackId("U67890")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() ->
            employeeDomainService.validateExternalIdUniqueness(newEmployee)
        );

        verify(employeeRepository).existsBySlackId("U67890");
    }

    @Test
    @DisplayName("Should throw exception for new employee with duplicate Slack ID")
    void testValidateExternalIdUniqueness_NewEmployee_DuplicateSlackId() {
        // Arrange
        Employee newEmployee = EmployeeTestDataBuilder.minimalEmployee();
        when(employeeRepository.existsBySlackId("U67890")).thenReturn(true);

        // Act & Assert
        DuplicateExternalIdException exception = assertThrows(
            DuplicateExternalIdException.class,
            () -> employeeDomainService.validateExternalIdUniqueness(newEmployee)
        );

        assertEquals("slackId", exception.getExternalIdType());
        assertEquals("U67890", exception.getExternalIdValue());
        assertNull(exception.getEmployeeId());

        verify(employeeRepository).existsBySlackId("U67890");
    }

    @Test
    @DisplayName("Should throw exception for new employee with duplicate Google ID")
    void testValidateExternalIdUniqueness_NewEmployee_DuplicateGoogleId() {
        // Arrange
        Employee newEmployee = EmployeeTestDataBuilder.minimalEmployeeWithGoogle();
        when(employeeRepository.existsByGoogleId("jane.smith@example.com")).thenReturn(true);

        // Act & Assert
        DuplicateExternalIdException exception = assertThrows(
            DuplicateExternalIdException.class,
            () -> employeeDomainService.validateExternalIdUniqueness(newEmployee)
        );

        assertEquals("googleId", exception.getExternalIdType());
        assertEquals("jane.smith@example.com", exception.getExternalIdValue());
        assertNull(exception.getEmployeeId());

        verify(employeeRepository).existsByGoogleId("jane.smith@example.com");
    }

    @Test
    @DisplayName("Should pass validation for existing employee updating their own data")
    void testValidateExternalIdUniqueness_ExistingEmployee_Success() {
        // Arrange
        when(employeeRepository.existsBySlackIdAndIdNot("U12345", existingEmployeeId))
            .thenReturn(false);
        when(employeeRepository.existsByGoogleIdAndIdNot("john.doe@example.com", existingEmployeeId))
            .thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() ->
            employeeDomainService.validateExternalIdUniqueness(testEmployee)
        );

        verify(employeeRepository).existsBySlackIdAndIdNot("U12345", existingEmployeeId);
        verify(employeeRepository).existsByGoogleIdAndIdNot("john.doe@example.com", existingEmployeeId);
    }

    @Test
    @DisplayName("Should throw exception when existing employee's Slack ID conflicts with another employee")
    void testValidateExternalIdUniqueness_ExistingEmployee_DuplicateSlackId() {
        // Arrange
        when(employeeRepository.existsBySlackIdAndIdNot("U12345", existingEmployeeId))
            .thenReturn(true);

        // Act & Assert
        DuplicateExternalIdException exception = assertThrows(
            DuplicateExternalIdException.class,
            () -> employeeDomainService.validateExternalIdUniqueness(testEmployee)
        );

        assertEquals("slackId", exception.getExternalIdType());
        assertEquals("U12345", exception.getExternalIdValue());
        assertEquals(existingEmployeeId, exception.getEmployeeId());

        verify(employeeRepository).existsBySlackIdAndIdNot("U12345", existingEmployeeId);
    }

    @Test
    @DisplayName("Should throw exception when existing employee's Google ID conflicts with another employee")
    void testValidateExternalIdUniqueness_ExistingEmployee_DuplicateGoogleId() {
        // Arrange
        when(employeeRepository.existsBySlackIdAndIdNot("U12345", existingEmployeeId))
            .thenReturn(false);
        when(employeeRepository.existsByGoogleIdAndIdNot("john.doe@example.com", existingEmployeeId))
            .thenReturn(true);

        // Act & Assert
        DuplicateExternalIdException exception = assertThrows(
            DuplicateExternalIdException.class,
            () -> employeeDomainService.validateExternalIdUniqueness(testEmployee)
        );

        assertEquals("googleId", exception.getExternalIdType());
        assertEquals("john.doe@example.com", exception.getExternalIdValue());
        assertEquals(existingEmployeeId, exception.getEmployeeId());

        verify(employeeRepository).existsByGoogleIdAndIdNot("john.doe@example.com", existingEmployeeId);
    }

    @Test
    @DisplayName("Should pass validation when Slack ID is null")
    void testValidateExternalIdUniqueness_NullSlackId() {
        // Arrange - create employee without ID so it's treated as new
        Employee employee = Employee.create(
            "Test Employee",
            null,
            "test@example.com",
            "Test",
            java.time.LocalDate.of(2020, 1, 1)
        );
        when(employeeRepository.existsByGoogleId("test@example.com")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() ->
            employeeDomainService.validateExternalIdUniqueness(employee)
        );

        verify(employeeRepository, never()).existsBySlackId(anyString());
        verify(employeeRepository).existsByGoogleId("test@example.com");
    }

    @Test
    @DisplayName("Should pass validation when Slack ID is empty")
    void testValidateExternalIdUniqueness_EmptySlackId() {
        // Arrange
        Employee employee = Employee.create(
            "Test Employee",
            "",
            "test@example.com",
            "Test",
            java.time.LocalDate.of(2020, 1, 1)
        );
        when(employeeRepository.existsByGoogleId("test@example.com")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() ->
            employeeDomainService.validateExternalIdUniqueness(employee)
        );

        verify(employeeRepository, never()).existsBySlackId(anyString());
    }

    @Test
    @DisplayName("Should pass validation when Google ID is null")
    void testValidateExternalIdUniqueness_NullGoogleId() {
        // Arrange - create employee without ID so it's treated as new
        Employee employee = Employee.create(
            "Test Employee",
            "U12345",
            null,
            "Test",
            java.time.LocalDate.of(2020, 1, 1)
        );
        when(employeeRepository.existsBySlackId("U12345")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() ->
            employeeDomainService.validateExternalIdUniqueness(employee)
        );

        verify(employeeRepository).existsBySlackId("U12345");
        verify(employeeRepository, never()).existsByGoogleId(anyString());
    }

    @Test
    @DisplayName("Should pass validation when Google ID is empty")
    void testValidateExternalIdUniqueness_EmptyGoogleId() {
        // Arrange
        Employee employee = Employee.create(
            "Test Employee",
            "U12345",
            "",
            "Test",
            java.time.LocalDate.of(2020, 1, 1)
        );
        when(employeeRepository.existsBySlackId("U12345")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() ->
            employeeDomainService.validateExternalIdUniqueness(employee)
        );

        verify(employeeRepository).existsBySlackId("U12345");
        verify(employeeRepository, never()).existsByGoogleId(anyString());
    }

    @Test
    @DisplayName("Should throw exception when employee is null")
    void testValidateExternalIdUniqueness_NullEmployee_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> employeeDomainService.validateExternalIdUniqueness(null)
        );

        assertEquals("Employee cannot be null", exception.getMessage());

        verifyNoInteractions(employeeRepository);
    }

    // ==================== validateEmployeeForPersistence Tests ====================

    @Test
    @DisplayName("Should pass all validations for valid employee")
    void testValidateEmployeeForPersistence_Success() {
        // Arrange
        Employee validEmployee = EmployeeTestDataBuilder.minimalEmployee();
        when(employeeRepository.existsBySlackId(anyString())).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() ->
            employeeDomainService.validateEmployeeForPersistence(validEmployee)
        );

        verify(employeeRepository).existsBySlackId(anyString());
    }

    @Test
    @DisplayName("Should fail when domain validation fails")
    void testValidateEmployeeForPersistence_DomainValidationFails() {
        // Arrange - Employee with future date of joining will fail domain validation
        Employee invalidEmployee = Employee.builder()
                .name("Test Employee")
                .slackId("U12345")
                .googleId(null)
                .slackDisplayName(null)
                .dateOfJoining(java.time.LocalDate.now().plusDays(1))
                .active(true)
                .carryForwardLeaves(new java.util.HashMap<>())
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> employeeDomainService.validateEmployeeForPersistence(invalidEmployee)
        );

        assertTrue(exception.getMessage().contains("dateOfJoining"));

        // Domain validation happens before external ID check
        verifyNoInteractions(employeeRepository);
    }

    @Test
    @DisplayName("Should fail when external ID validation fails")
    void testValidateEmployeeForPersistence_ExternalIdValidationFails() {
        // Arrange
        Employee employee = EmployeeTestDataBuilder.minimalEmployee();
        when(employeeRepository.existsBySlackId(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(
            DuplicateExternalIdException.class,
            () -> employeeDomainService.validateEmployeeForPersistence(employee)
        );

        verify(employeeRepository).existsBySlackId(anyString());
    }

    @Test
    @DisplayName("Should throw exception when validating null employee for persistence")
    void testValidateEmployeeForPersistence_NullEmployee_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> employeeDomainService.validateEmployeeForPersistence(null)
        );

        assertEquals("Employee cannot be null", exception.getMessage());

        verifyNoInteractions(employeeRepository);
    }
}
