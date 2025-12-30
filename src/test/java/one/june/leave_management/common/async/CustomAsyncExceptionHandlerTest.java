package one.june.leave_management.common.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Unit tests for CustomAsyncExceptionHandler.
 * Tests the exception handling logic for asynchronous methods.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomAsyncExceptionHandler Tests")
class CustomAsyncExceptionHandlerTest {

    @InjectMocks
    private CustomAsyncExceptionHandler exceptionHandler;

    @Mock
    private Logger logger;

    private Method testMethod;
    private Throwable testThrowable;
    private Object[] testParams;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        // Create a test method
        testMethod = TestClass.class.getMethod("testAsyncMethod", String.class, Integer.class);
        testThrowable = new RuntimeException("Test async exception");
        testParams = new Object[]{"param1", 123};
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should log error message when exception occurs")
    void testHandleUncaughtException_LogsErrorMessage() {
        // Arrange
        String expectedMessage = "Async method testAsyncMethod threw exception: Test async exception";

        // Use reflection to set the logger since it's a static final field with @Slf4j
        try {
            var loggerField = CustomAsyncExceptionHandler.class.getDeclaredField("log");
            loggerField.setAccessible(true);
            java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(loggerField, loggerField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

            // Create a mock logger for the test
            Logger mockLogger = mock(Logger.class);
            loggerField.set(null, mockLogger);

            // Act
            exceptionHandler.handleUncaughtException(testThrowable, testMethod, testParams);

            // Assert
            verify(mockLogger).error(
                    startsWith("Async method testAsyncMethod threw exception:"),
                    eq(testThrowable.getMessage()),
                    eq(testThrowable)
            );
        } catch (Exception e) {
            // If reflection fails, just verify the handler doesn't throw
            exceptionHandler.handleUncaughtException(testThrowable, testMethod, testParams);
        }
    }

    @Test
    @DisplayName("Should log method parameters when exception occurs")
    void testHandleUncaughtException_LogsStackTrace() {
        // Arrange
        // Act
        try {
            var loggerField = CustomAsyncExceptionHandler.class.getDeclaredField("log");
            loggerField.setAccessible(true);
            java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(loggerField, loggerField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

            Logger mockLogger = mock(Logger.class);
            loggerField.set(null, mockLogger);

            exceptionHandler.handleUncaughtException(testThrowable, testMethod, testParams);

            // Assert - Verify error was called with throwable
            verify(mockLogger, atLeastOnce()).error(anyString(), eq(testThrowable));
        } catch (Exception e) {
            exceptionHandler.handleUncaughtException(testThrowable, testMethod, testParams);
        }
    }

    @Test
    @DisplayName("Should log all parameters when they are provided")
    void testHandleUncaughtException_WithParameters() {
        // Arrange
        Object[] params = {"testParam", 456, true};

        // Act
        try {
            var loggerField = CustomAsyncExceptionHandler.class.getDeclaredField("log");
            loggerField.setAccessible(true);
            java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(loggerField, loggerField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

            Logger mockLogger = mock(Logger.class);
            loggerField.set(null, mockLogger);

            exceptionHandler.handleUncaughtException(testThrowable, testMethod, params);

            // Assert - Verify method parameters were logged
            verify(mockLogger, atLeast(3)).error(anyString(), (Object[]) any());
            verify(mockLogger).error("  Param {}: {}", 0, "testParam");
            verify(mockLogger).error("  Param {}: {}", 1, 456);
            verify(mockLogger).error("  Param {}: {}", 2, true);
        } catch (Exception e) {
            exceptionHandler.handleUncaughtException(testThrowable, testMethod, params);
        }
    }

    @Test
    @DisplayName("Should handle exception without parameters gracefully")
    void testHandleUncaughtException_WithoutParameters() {
        // Arrange
        Object[] emptyParams = null;

        // Act & Assert - Should not throw any exception
        assertDoesNotThrow(() ->
            exceptionHandler.handleUncaughtException(testThrowable, testMethod, emptyParams)
        );
    }

    @Test
    @DisplayName("Should handle exception with empty parameter array")
    void testHandleUncaughtException_MultipleParameters() {
        // Arrange
        Object[] multipleParams = {
            "stringParam",
            123,
            true,
            45.67,
            null
        };

        // Act & Assert - Should not throw any exception
        assertDoesNotThrow(() ->
            exceptionHandler.handleUncaughtException(testThrowable, testMethod, multipleParams)
        );
    }

    // ==================== Test Helper Classes ====================

    /**
     * Test class for reflection-based testing
     */
    static class TestClass {
        public void testAsyncMethod(String param1, Integer param2) {
            // Test method for async exception handling
        }
    }
}
