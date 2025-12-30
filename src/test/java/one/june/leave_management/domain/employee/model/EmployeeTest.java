package one.june.leave_management.domain.employee.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {

    private static final String TEST_NAME = "John Doe";
    private static final String TEST_SLACK_ID = "U12345";
    private static final String TEST_GOOGLE_ID = "john.doe@example.com";
    private static final String TEST_SLACK_DISPLAY_NAME = "John D";
    private static final LocalDate TEST_DATE_OF_JOINING = LocalDate.of(2020, 1, 15);

    @Test
    void builderShouldCreateEmployeeWithAllFields() {
        UUID id = UUID.randomUUID();
        Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(2024, 5);
        carryForwardLeaves.put(2025, 3);

        Employee employee = Employee.builder()
                .id(id)
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .googleId(TEST_GOOGLE_ID)
                .slackDisplayName(TEST_SLACK_DISPLAY_NAME)
                .dateOfJoining(TEST_DATE_OF_JOINING)
                .active(true)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        assertEquals(id, employee.getId());
        assertEquals(TEST_NAME, employee.getName());
        assertEquals(TEST_SLACK_ID, employee.getSlackId());
        assertEquals(TEST_GOOGLE_ID, employee.getGoogleId());
        assertEquals(TEST_SLACK_DISPLAY_NAME, employee.getSlackDisplayName());
        assertEquals(TEST_DATE_OF_JOINING, employee.getDateOfJoining());
        assertTrue(employee.isActive());
        assertEquals(carryForwardLeaves, employee.getCarryForwardLeaves());
    }

    @Test
    void createFactoryMethodShouldCreateValidEmployeeWithSlackId() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertNotNull(employee);
        assertEquals(TEST_NAME, employee.getName());
        assertEquals(TEST_SLACK_ID, employee.getSlackId());
        assertNull(employee.getGoogleId());
        assertEquals(TEST_SLACK_DISPLAY_NAME, employee.getSlackDisplayName());
        assertEquals(TEST_DATE_OF_JOINING, employee.getDateOfJoining());
        assertTrue(employee.isActive());
        assertNotNull(employee.getCarryForwardLeaves());
        assertTrue(employee.getCarryForwardLeaves().isEmpty());
    }

    @Test
    void createFactoryMethodShouldCreateValidEmployeeWithGoogleId() {
        Employee employee = Employee.create(
                TEST_NAME,
                null,
                TEST_GOOGLE_ID,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertNotNull(employee);
        assertEquals(TEST_NAME, employee.getName());
        assertNull(employee.getSlackId());
        assertEquals(TEST_GOOGLE_ID, employee.getGoogleId());
        assertEquals(TEST_SLACK_DISPLAY_NAME, employee.getSlackDisplayName());
        assertEquals(TEST_DATE_OF_JOINING, employee.getDateOfJoining());
        assertTrue(employee.isActive());
        assertNotNull(employee.getCarryForwardLeaves());
        assertTrue(employee.getCarryForwardLeaves().isEmpty());
    }

    @Test
    void createFactoryMethodShouldCreateValidEmployeeWithBothIds() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                TEST_GOOGLE_ID,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertNotNull(employee);
        assertEquals(TEST_NAME, employee.getName());
        assertEquals(TEST_SLACK_ID, employee.getSlackId());
        assertEquals(TEST_GOOGLE_ID, employee.getGoogleId());
        assertEquals(TEST_SLACK_DISPLAY_NAME, employee.getSlackDisplayName());
        assertEquals(TEST_DATE_OF_JOINING, employee.getDateOfJoining());
        assertTrue(employee.isActive());
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Employee.create(null, TEST_SLACK_ID, null, TEST_SLACK_DISPLAY_NAME, TEST_DATE_OF_JOINING));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenNameIsEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> Employee.create("  ", TEST_SLACK_ID, null, TEST_SLACK_DISPLAY_NAME, TEST_DATE_OF_JOINING));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenDateOfJoiningIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Employee.create(TEST_NAME, TEST_SLACK_ID, null, TEST_SLACK_DISPLAY_NAME, null));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenNoExternalIdProvided() {
        assertThrows(IllegalArgumentException.class,
                () -> Employee.create(TEST_NAME, null, null, TEST_SLACK_DISPLAY_NAME, TEST_DATE_OF_JOINING));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenBothExternalIdsAreEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> Employee.create(TEST_NAME, "  ", "  ", TEST_SLACK_DISPLAY_NAME, TEST_DATE_OF_JOINING));
    }

    @Test
    void createFactoryMethodShouldThrowExceptionWhenDateOfJoiningIsInFuture() {
        assertThrows(IllegalArgumentException.class,
                () -> Employee.create(TEST_NAME, TEST_SLACK_ID, null, TEST_SLACK_DISPLAY_NAME,
                        LocalDate.now().plusDays(1)));
    }

    @Test
    void updateShouldUpdateAllFields() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        LocalDate newDateOfJoining = LocalDate.of(2021, 3, 20);
        employee.update(
                "Jane Smith",
                null,
                "jane.smith@example.com",
                "Jane S",
                newDateOfJoining
        );

        assertEquals("Jane Smith", employee.getName());
        assertNull(employee.getSlackId());
        assertEquals("jane.smith@example.com", employee.getGoogleId());
        assertEquals("Jane S", employee.getSlackDisplayName());
        assertEquals(newDateOfJoining, employee.getDateOfJoining());
    }

    @Test
    void updateShouldThrowExceptionWhenNameIsNull() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertThrows(IllegalArgumentException.class,
                () -> employee.update(null, TEST_SLACK_ID, null, TEST_SLACK_DISPLAY_NAME, TEST_DATE_OF_JOINING));
    }

    @Test
    void updateShouldThrowExceptionWhenNoExternalIdProvided() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertThrows(IllegalArgumentException.class,
                () -> employee.update(TEST_NAME, null, null, TEST_SLACK_DISPLAY_NAME, TEST_DATE_OF_JOINING));
    }

    @Test
    void updateCarryForwardLeavesShouldAddNewYear() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        employee.updateCarryForwardLeaves(2024, 5);

        assertEquals(5, employee.getCarryForwardLeavesForYear(2024));
        assertEquals(1, employee.getCarryForwardLeaves().size());
    }

    @Test
    void updateCarryForwardLeavesShouldUpdateExistingYear() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        employee.updateCarryForwardLeaves(2024, 5);
        employee.updateCarryForwardLeaves(2024, 7);

        assertEquals(7, employee.getCarryForwardLeavesForYear(2024));
        assertEquals(1, employee.getCarryForwardLeaves().size());
    }

    @Test
    void updateCarryForwardLeavesShouldRemoveYearWhenDaysIsZero() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        employee.updateCarryForwardLeaves(2024, 5);
        employee.updateCarryForwardLeaves(2024, 0);

        assertEquals(0, employee.getCarryForwardLeavesForYear(2024));
        assertTrue(employee.getCarryForwardLeaves().isEmpty());
    }

    @Test
    void updateCarryForwardLeavesShouldThrowExceptionWhenYearIsNull() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertThrows(IllegalArgumentException.class,
                () -> employee.updateCarryForwardLeaves(null, 5));
    }

    @Test
    void updateCarryForwardLeavesShouldThrowExceptionWhenDaysIsNull() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertThrows(IllegalArgumentException.class,
                () -> employee.updateCarryForwardLeaves(2024, null));
    }

    @Test
    void updateCarryForwardLeavesShouldThrowExceptionWhenDaysAreNegative() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertThrows(IllegalArgumentException.class,
                () -> employee.updateCarryForwardLeaves(2024, -1));
    }

    @Test
    void getCarryForwardLeavesForYearShouldReturnCorrectDays() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        employee.updateCarryForwardLeaves(2024, 5);
        employee.updateCarryForwardLeaves(2025, 3);

        assertEquals(5, employee.getCarryForwardLeavesForYear(2024));
        assertEquals(3, employee.getCarryForwardLeavesForYear(2025));
        assertEquals(0, employee.getCarryForwardLeavesForYear(2023));
    }

    @Test
    void getCarryForwardLeavesForYearShouldThrowExceptionWhenYearIsNull() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertThrows(IllegalArgumentException.class,
                () -> employee.getCarryForwardLeavesForYear(null));
    }

    @Test
    void deactivateShouldSetActiveToFalse() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        employee.deactivate();

        assertFalse(employee.isActive());
    }

    @Test
    void activateShouldSetActiveToTrue() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        employee.deactivate();
        employee.activate();

        assertTrue(employee.isActive());
    }

    @Test
    void hasMatchingExternalIdShouldReturnTrueForMatchingSlackId() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                null,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertTrue(employee.hasMatchingExternalId(TEST_SLACK_ID, null));
        assertTrue(employee.hasMatchingExternalId(TEST_SLACK_ID, "other@example.com"));
    }

    @Test
    void hasMatchingExternalIdShouldReturnTrueForMatchingGoogleId() {
        Employee employee = Employee.create(
                TEST_NAME,
                null,
                TEST_GOOGLE_ID,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertTrue(employee.hasMatchingExternalId(null, TEST_GOOGLE_ID));
        assertTrue(employee.hasMatchingExternalId("U99999", TEST_GOOGLE_ID));
    }

    @Test
    void hasMatchingExternalIdShouldReturnFalseWhenNoMatch() {
        Employee employee = Employee.create(
                TEST_NAME,
                TEST_SLACK_ID,
                TEST_GOOGLE_ID,
                TEST_SLACK_DISPLAY_NAME,
                TEST_DATE_OF_JOINING
        );

        assertFalse(employee.hasMatchingExternalId("U99999", "other@example.com"));
        assertFalse(employee.hasMatchingExternalId(null, null));
    }

    @Test
    void getCarryForwardLeavesShouldReturnDefensiveCopy() {
        Map<Integer, Integer> originalMap = new HashMap<>();
        originalMap.put(2024, 5);

        Employee employee = Employee.builder()
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(TEST_DATE_OF_JOINING)
                .carryForwardLeaves(originalMap)
                .build();

        Map<Integer, Integer> retrievedMap = employee.getCarryForwardLeaves();
        retrievedMap.put(2025, 10);

        assertEquals(1, employee.getCarryForwardLeaves().size());
        assertEquals(5, employee.getCarryForwardLeavesForYear(2024));
    }

    @Test
    void setCarryForwardLeavesShouldCreateDefensiveCopy() {
        Map<Integer, Integer> originalMap = new HashMap<>();
        originalMap.put(2024, 5);

        Employee employee = Employee.builder()
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(TEST_DATE_OF_JOINING)
                .build();

        employee.setCarryForwardLeaves(originalMap);
        originalMap.put(2025, 10);

        assertEquals(1, employee.getCarryForwardLeaves().size());
        assertEquals(5, employee.getCarryForwardLeavesForYear(2024));
    }

    @Test
    void isActiveShouldReturnFalseWhenActiveIsNull() {
        Employee employee = Employee.builder()
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(TEST_DATE_OF_JOINING)
                .active(null)
                .build();

        assertFalse(employee.isActive());
    }

    @Test
    void validateShouldThrowExceptionForNullName() {
        Employee employee = Employee.builder()
                .name(null)
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(TEST_DATE_OF_JOINING)
                .build();

        assertThrows(IllegalArgumentException.class, employee::validate);
    }

    @Test
    void validateShouldThrowExceptionForEmptyName() {
        Employee employee = Employee.builder()
                .name("  ")
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(TEST_DATE_OF_JOINING)
                .build();

        assertThrows(IllegalArgumentException.class, employee::validate);
    }

    @Test
    void validateShouldThrowExceptionForNullDateOfJoining() {
        Employee employee = Employee.builder()
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(null)
                .build();

        assertThrows(IllegalArgumentException.class, employee::validate);
    }

    @Test
    void validateShouldThrowExceptionForFutureDateOfJoining() {
        Employee employee = Employee.builder()
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(LocalDate.now().plusDays(1))
                .build();

        assertThrows(IllegalArgumentException.class, employee::validate);
    }

    @Test
    void validateShouldThrowExceptionForNegativeCarryForwardLeaves() {
        Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(2024, -5);

        Employee employee = Employee.builder()
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(TEST_DATE_OF_JOINING)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        assertThrows(IllegalArgumentException.class, employee::validate);
    }

    @Test
    void validateShouldThrowExceptionForNullYearInCarryForwardLeaves() {
        Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
        carryForwardLeaves.put(null, 5);

        Employee employee = Employee.builder()
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .dateOfJoining(TEST_DATE_OF_JOINING)
                .carryForwardLeaves(carryForwardLeaves)
                .build();

        assertThrows(IllegalArgumentException.class, employee::validate);
    }
}
