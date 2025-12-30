package one.june.leave_management.application.employee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import one.june.leave_management.application.employee.command.EmployeeCreateCommand;
import one.june.leave_management.application.employee.dto.EmployeeDto;
import one.june.leave_management.common.exception.EmployeeNotFoundException;
import one.june.leave_management.common.mapper.EmployeeMapper;
import one.june.leave_management.domain.employee.model.Employee;
import one.june.leave_management.domain.employee.port.EmployeeRepository;
import one.june.leave_management.domain.employee.service.EmployeeDomainService;
import one.june.leave_management.test.builder.EmployeeTestDataBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmployeeService.
 * Tests the application service layer for employee CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeDomainService employeeDomainService;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private one.june.leave_management.adapter.persistence.jpa.repository.BulkUploadJobRepository bulkUploadJobRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private EmployeeDto testEmployeeDto;
    private UUID testEmployeeId;

    @BeforeEach
    void setUp() {
        testEmployeeId = UUID.randomUUID();
        testEmployee = EmployeeTestDataBuilder.defaultEmployee()
                .id(testEmployeeId)
                .build();

        testEmployeeDto = EmployeeTestDataBuilder.defaultDto()
                .id(testEmployeeId)
                .build();
    }

    // ==================== Create Employee Tests ====================

    @Test
    @DisplayName("Should create employee with valid data")
    void testCreateEmployee_Success() {
        // Arrange
        EmployeeCreateCommand command = EmployeeTestDataBuilder.defaultCommand().build();
        Employee savedEmployee = EmployeeTestDataBuilder.defaultEmployee().id(testEmployeeId).build();
        EmployeeDto expectedDto = EmployeeTestDataBuilder.defaultDto().id(testEmployeeId).build();

        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
        when(employeeMapper.toDto(savedEmployee)).thenReturn(expectedDto);

        // Act
        EmployeeDto result = employeeService.create(command);

        // Assert
        assertNotNull(result);
        assertEquals(testEmployeeId, result.getId());
        assertEquals(command.getName(), result.getName());

        verify(employeeDomainService).validateEmployeeForPersistence(any(Employee.class));
        verify(employeeRepository).save(any(Employee.class));
        verify(employeeMapper).toDto(savedEmployee);
    }

    @Test
    @DisplayName("Should create employee with optional fields")
    void testCreateEmployee_WithOptionalFields() {
        // Arrange
        HashMap<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(2023, 5);

        EmployeeCreateCommand command = EmployeeTestDataBuilder.defaultCommand()
                .active(false)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        Employee savedEmployee = EmployeeTestDataBuilder.defaultEmployee()
                .id(testEmployeeId)
                .active(false)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        EmployeeDto expectedDto = EmployeeTestDataBuilder.defaultDto()
                .id(testEmployeeId)
                .active(false)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
        when(employeeMapper.toDto(savedEmployee)).thenReturn(expectedDto);

        // Act
        EmployeeDto result = employeeService.create(command);

        // Assert
        assertNotNull(result);
        assertEquals(false, result.getActive());
        assertEquals(carryForwardLeaves, result.getCarryForwardLeaves());

        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should create employee with carry forward leaves")
    void testCreateEmployee_WithCarryForwardLeaves() {
        // Arrange
        HashMap<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(2024, 3);

        EmployeeCreateCommand command = EmployeeTestDataBuilder.defaultCommand()
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        Employee savedEmployee = EmployeeTestDataBuilder.defaultEmployee()
                .id(testEmployeeId)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        EmployeeDto expectedDto = EmployeeTestDataBuilder.defaultDto()
                .id(testEmployeeId)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
        when(employeeMapper.toDto(savedEmployee)).thenReturn(expectedDto);

        // Act
        EmployeeDto result = employeeService.create(command);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getCarryForwardLeaves().size());
        assertEquals(3, result.getCarryForwardLeaves().get(2024));

        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when creating employee with duplicate Slack ID")
    void testCreateEmployee_DuplicateSlackId_ThrowsException() {
        // Arrange
        EmployeeCreateCommand command = EmployeeTestDataBuilder.defaultCommand().build();

        doThrow(new one.june.leave_management.common.exception.DuplicateExternalIdException(
                "slackId", "U12345", null))
            .when(employeeDomainService).validateEmployeeForPersistence(any(Employee.class));

        // Act & Assert
        assertThrows(
            one.june.leave_management.common.exception.DuplicateExternalIdException.class,
            () -> employeeService.create(command)
        );

        verify(employeeDomainService).validateEmployeeForPersistence(any(Employee.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when creating employee with duplicate Google ID")
    void testCreateEmployee_DuplicateGoogleId_ThrowsException() {
        // Arrange
        EmployeeCreateCommand command = EmployeeTestDataBuilder.defaultCommand().build();

        doThrow(new one.june.leave_management.common.exception.DuplicateExternalIdException(
                "googleId", "john.doe@example.com", null))
            .when(employeeDomainService).validateEmployeeForPersistence(any(Employee.class));

        // Act & Assert
        assertThrows(
            one.june.leave_management.common.exception.DuplicateExternalIdException.class,
            () -> employeeService.create(command)
        );

        verify(employeeDomainService).validateEmployeeForPersistence(any(Employee.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ==================== Find Employee Tests ====================

    @Test
    @DisplayName("Should find employee by ID")
    void testFindById_Success() {
        // Arrange
        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeMapper.toDto(testEmployee)).thenReturn(testEmployeeDto);

        // Act
        EmployeeDto result = employeeService.findById(testEmployeeId);

        // Assert
        assertNotNull(result);
        assertEquals(testEmployeeId, result.getId());
        assertEquals(testEmployee.getName(), result.getName());

        verify(employeeRepository).findById(testEmployeeId);
        verify(employeeMapper).toDto(testEmployee);
    }

    @Test
    @DisplayName("Should throw exception when finding non-existent employee by ID")
    void testFindById_NotFound_ThrowsException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(employeeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            EmployeeNotFoundException.class,
            () -> employeeService.findById(nonExistentId)
        );

        verify(employeeRepository).findById(nonExistentId);
        verify(employeeMapper, never()).toDto(any(Employee.class));
    }

    @Test
    @DisplayName("Should find employee by Slack ID")
    void testFindBySlackId_Success() {
        // Arrange
        String slackId = "U12345";
        when(employeeRepository.findBySlackId(slackId)).thenReturn(Optional.of(testEmployee));
        when(employeeMapper.toDto(testEmployee)).thenReturn(testEmployeeDto);

        // Act
        Optional<EmployeeDto> result = employeeService.findBySlackId(slackId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testEmployeeId, result.get().getId());

        verify(employeeRepository).findBySlackId(slackId);
        verify(employeeMapper).toDto(testEmployee);
    }

    @Test
    @DisplayName("Should return empty when finding by non-existent Slack ID")
    void testFindBySlackId_NotFound_ReturnsEmpty() {
        // Arrange
        String slackId = "NONEXISTENT";
        when(employeeRepository.findBySlackId(slackId)).thenReturn(Optional.empty());

        // Act
        Optional<EmployeeDto> result = employeeService.findBySlackId(slackId);

        // Assert
        assertFalse(result.isPresent());
        verify(employeeRepository).findBySlackId(slackId);
        verify(employeeMapper, never()).toDto(any(Employee.class));
    }

    @Test
    @DisplayName("Should find employee by Google ID")
    void testFindByGoogleId_Success() {
        // Arrange
        String googleId = "john.doe@example.com";
        when(employeeRepository.findByGoogleId(googleId)).thenReturn(Optional.of(testEmployee));
        when(employeeMapper.toDto(testEmployee)).thenReturn(testEmployeeDto);

        // Act
        Optional<EmployeeDto> result = employeeService.findByGoogleId(googleId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testEmployeeId, result.get().getId());

        verify(employeeRepository).findByGoogleId(googleId);
        verify(employeeMapper).toDto(testEmployee);
    }

    @Test
    @DisplayName("Should return empty when finding by non-existent Google ID")
    void testFindByGoogleId_NotFound_ReturnsEmpty() {
        // Arrange
        String googleId = "nonexistent@example.com";
        when(employeeRepository.findByGoogleId(googleId)).thenReturn(Optional.empty());

        // Act
        Optional<EmployeeDto> result = employeeService.findByGoogleId(googleId);

        // Assert
        assertFalse(result.isPresent());
        verify(employeeRepository).findByGoogleId(googleId);
        verify(employeeMapper, never()).toDto(any(Employee.class));
    }

    // ==================== List Employees Tests ====================

    @Test
    @DisplayName("Should get all employees with pagination")
    void testFindAll_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee));
        Page<EmployeeDto> dtoPage = new PageImpl<>(List.of(testEmployeeDto));

        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);
        when(employeeMapper.toDto(testEmployee)).thenReturn(testEmployeeDto);

        // Act
        Page<EmployeeDto> result = employeeService.findAll(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testEmployeeId, result.getContent().get(0).getId());

        verify(employeeRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should get all active employees")
    void testFindAllActive_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Employee activeEmployee = EmployeeTestDataBuilder.defaultEmployee()
                .active(true)
                .build();
        Page<Employee> employeePage = new PageImpl<>(List.of(activeEmployee));

        EmployeeDto activeDto = EmployeeTestDataBuilder.defaultDto()
                .active(true)
                .build();
        Page<EmployeeDto> dtoPage = new PageImpl<>(List.of(activeDto));

        when(employeeRepository.findAllActive(pageable)).thenReturn(employeePage);
        when(employeeMapper.toDto(activeEmployee)).thenReturn(activeDto);

        // Act
        Page<EmployeeDto> result = employeeService.findAllActive(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getActive());

        verify(employeeRepository).findAllActive(pageable);
    }

    @Test
    @DisplayName("Should filter employees by active status")
    void testFindByActiveStatus_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Boolean activeStatus = true;
        Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee));
        Page<EmployeeDto> dtoPage = new PageImpl<>(List.of(testEmployeeDto));

        when(employeeRepository.findByActiveStatus(activeStatus, pageable)).thenReturn(employeePage);
        when(employeeMapper.toDto(testEmployee)).thenReturn(testEmployeeDto);

        // Act
        Page<EmployeeDto> result = employeeService.findByActiveStatus(activeStatus, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(employeeRepository).findByActiveStatus(activeStatus, pageable);
    }

    @Test
    @DisplayName("Should search employees by name")
    void testSearchByName_Success() {
        // Arrange
        String name = "John";
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee));
        Page<EmployeeDto> dtoPage = new PageImpl<>(List.of(testEmployeeDto));

        when(employeeRepository.searchByName(name, pageable)).thenReturn(employeePage);
        when(employeeMapper.toDto(testEmployee)).thenReturn(testEmployeeDto);

        // Act
        Page<EmployeeDto> result = employeeService.searchByName(name, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(employeeRepository).searchByName(name, pageable);
    }

    // ==================== Update Employee Tests ====================

    @Test
    @DisplayName("Should update existing employee")
    void testUpdateEmployee_Success() {
        // Arrange
        EmployeeCreateCommand command = EmployeeTestDataBuilder.updateCommand(testEmployeeId)
                .name("Updated Name")
                .build();

        Employee updatedEmployee = EmployeeTestDataBuilder.defaultEmployee()
                .id(testEmployeeId)
                .name("Updated Name")
                .build();

        EmployeeDto expectedDto = EmployeeTestDataBuilder.defaultDto()
                .id(testEmployeeId)
                .name("Updated Name")
                .build();

        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);
        when(employeeMapper.toDto(updatedEmployee)).thenReturn(expectedDto);

        // Act
        EmployeeDto result = employeeService.update(testEmployeeId, command);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());

        verify(employeeRepository).findById(testEmployeeId);
        verify(employeeDomainService).validateEmployeeForPersistence(any(Employee.class));
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent employee")
    void testUpdateEmployee_NotFound_ThrowsException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        EmployeeCreateCommand command = EmployeeTestDataBuilder.updateCommand(nonExistentId).build();

        when(employeeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            EmployeeNotFoundException.class,
            () -> employeeService.update(nonExistentId, command)
        );

        verify(employeeRepository).findById(nonExistentId);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should update employee with optional fields")
    void testUpdateEmployee_WithOptionalFields() {
        // Arrange
        HashMap<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(2024, 10);

        EmployeeCreateCommand command = EmployeeTestDataBuilder.updateCommand(testEmployeeId)
                .active(false)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        Employee updatedEmployee = EmployeeTestDataBuilder.defaultEmployee()
                .id(testEmployeeId)
                .active(false)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        EmployeeDto expectedDto = EmployeeTestDataBuilder.defaultDto()
                .id(testEmployeeId)
                .active(false)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);
        when(employeeMapper.toDto(updatedEmployee)).thenReturn(expectedDto);

        // Act
        EmployeeDto result = employeeService.update(testEmployeeId, command);

        // Assert
        assertNotNull(result);
        assertEquals(false, result.getActive());
        assertEquals(carryForwardLeaves, result.getCarryForwardLeaves());

        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should update carry forward leaves")
    void testUpdateEmployee_CarryForwardLeaves() {
        // Arrange
        HashMap<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(2024, 7);

        EmployeeCreateCommand command = EmployeeTestDataBuilder.updateCommand(testEmployeeId)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        Employee updatedEmployee = EmployeeTestDataBuilder.defaultEmployee()
                .id(testEmployeeId)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        EmployeeDto expectedDto = EmployeeTestDataBuilder.defaultDto()
                .id(testEmployeeId)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);
        when(employeeMapper.toDto(updatedEmployee)).thenReturn(expectedDto);

        // Act
        EmployeeDto result = employeeService.update(testEmployeeId, command);

        // Assert
        assertNotNull(result);
        assertEquals(carryForwardLeaves, result.getCarryForwardLeaves());

        verify(employeeRepository).save(any(Employee.class));
    }

    // ==================== Activate/Deactivate Tests ====================

    @Test
    @DisplayName("Should deactivate employee")
    void testDeactivateEmployee_Success() {
        // Arrange
        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // Act
        employeeService.deactivate(testEmployeeId);

        // Assert
        verify(employeeRepository).findById(testEmployeeId);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent employee")
    void testDeactivateEmployee_NotFound_ThrowsException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(employeeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            EmployeeNotFoundException.class,
            () -> employeeService.deactivate(nonExistentId)
        );

        verify(employeeRepository).findById(nonExistentId);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should activate employee")
    void testActivateEmployee_Success() {
        // Arrange
        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // Act
        employeeService.activate(testEmployeeId);

        // Assert
        verify(employeeRepository).findById(testEmployeeId);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when activating non-existent employee")
    void testActivateEmployee_NotFound_ThrowsException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(employeeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            EmployeeNotFoundException.class,
            () -> employeeService.activate(nonExistentId)
        );

        verify(employeeRepository).findById(nonExistentId);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ==================== Carry Forward Leaves Tests ====================

    @Test
    @DisplayName("Should update carry forward leaves for employee")
    void testUpdateCarryForwardLeaves_Success() {
        // Arrange
        Integer year = 2024;
        Integer days = 5;

        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // Act
        employeeService.updateCarryForwardLeaves(testEmployeeId, year, days);

        // Assert
        verify(employeeRepository).findById(testEmployeeId);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when updating carry forward leaves for non-existent employee")
    void testUpdateCarryForwardLeaves_NotFound_ThrowsException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(employeeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            EmployeeNotFoundException.class,
            () -> employeeService.updateCarryForwardLeaves(nonExistentId, 2024, 5)
        );

        verify(employeeRepository).findById(nonExistentId);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should add carry forward leaves for new year")
    void testUpdateCarryForwardLeaves_AddNewYear() {
        // Arrange
        Integer year = 2025;
        Integer days = 8;

        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // Act
        employeeService.updateCarryForwardLeaves(testEmployeeId, year, days);

        // Assert
        verify(employeeRepository).findById(testEmployeeId);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should update existing year's carry forward leaves")
    void testUpdateCarryForwardLeaves_UpdateExistingYear() {
        // Arrange
        HashMap<Integer, Integer> existingLeaves = new HashMap<>();
        existingLeaves.put(2024, 3);

        Employee employeeWithLeaves = EmployeeTestDataBuilder.defaultEmployee()
                .id(testEmployeeId)
                .carryForwardLeaves(existingLeaves)
                .build();

        Integer year = 2024;
        Integer newDays = 7;

        when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(employeeWithLeaves));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employeeWithLeaves);

        // Act
        employeeService.updateCarryForwardLeaves(testEmployeeId, year, newDays);

        // Assert
        verify(employeeRepository).findById(testEmployeeId);
        verify(employeeRepository).save(any(Employee.class));
    }
}
