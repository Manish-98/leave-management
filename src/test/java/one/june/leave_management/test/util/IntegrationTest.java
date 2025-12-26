package one.june.leave_management.test.util;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for integration tests that combines common configuration
 * and provides automatic database cleanup functionality.
 *
 * <p>Features:
 * <ul>
 *   <li>Sets up Spring Boot test context with random web environment</li>
 *   <li>Activates "test" profile</li>
 *   <li>Enables transactions by default for test isolation</li>
 *   <li>Provides automatic database cleanup after each test method</li>
 * </ul>
 *
 * <p>By default (transactional=true), tests run within a transaction that is
 * rolled back after each test, providing automatic cleanup. When transactional=false,
 * the IntegrationTestListener performs SQL-based cleanup of all database tables.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * @IntegrationTest
 * class MyIntegrationTest {
 *     // Test methods - no manual cleanup needed
 * }
 * }
 * </pre>
 *
 * <p>For tests that cannot use @Transactional (e.g., testing transactional behavior):
 * <pre>
 * {@code
 * @IntegrationTest(transactional = false)
 * class MyNonTransactionalIntegrationTest {
 *     // Test methods - SQL cleanup will be performed automatically
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestExecutionListeners(listeners = IntegrationTestListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Transactional
public @interface IntegrationTest {

    /**
     * Whether the test should run within a transaction.
     *
     * <p>When true (default), each test method runs in a transaction that is
     * rolled back after the test, providing automatic cleanup.
     *
     * <p>When false, the IntegrationTestListener performs SQL-based cleanup
     * of all database tables after each test method. Use this for tests that
     * need to verify transactional behavior or commit data.
     *
     * @return true if tests should be transactional, false otherwise
     */
    boolean transactional() default true;
}
