package one.june.leave_management.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Test execution listener for integration tests.
 *
 * <p>Performs automatic database cleanup after each test method when the test
 * is not transactional (i.e., @IntegrationTest(transactional = false)).
 *
 * <p>For transactional tests, Spring's @Transactional automatically rolls back
 * changes, so no explicit cleanup is needed. For non-transactional tests,
 * this listener deletes all data from database tables after each test method.
 *
 * <p>Cleanup is performed in the correct order to respect foreign key constraints:
 * <ol>
 *   <li>leave_source_ref (has foreign key to leave)</li>
 *   <li>leave (has no dependent tables)</li>
 *   <li>audit_log (independent table)</li>
 * </ol>
 */
@Slf4j
public class IntegrationTestListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        // Check if the test class has @IntegrationTest annotation
        IntegrationTest integrationTest = testContext.getTestClass()
                .getAnnotation(IntegrationTest.class);

        if (integrationTest == null) {
            log.debug("No @IntegrationTest annotation found, skipping cleanup");
            return; // Not an @IntegrationTest, skip cleanup
        }

        log.debug("Test class: {}, transactional: {}", testContext.getTestClass().getName(), integrationTest.transactional());

        // Only perform SQL cleanup if transactional=false
        // If transactional=true (default), Spring's @Transactional handles rollback
        if (!integrationTest.transactional()) {
            log.debug("Performing SQL cleanup for non-transactional test");
            cleanupDatabase(testContext);
        } else {
            log.debug("Skipping cleanup for transactional test - Spring will rollback");
        }
    }

    /**
     * Performs SQL-based cleanup of all database tables.
     *
     * <p>Executes DELETE statements to clean all data while preserving
     * the schema (tables, constraints, sequences).
     *
     * @param testContext the test context
     */
    private void cleanupDatabase(TestContext testContext) {
        try {
            DataSource dataSource = testContext.getApplicationContext().getBean(DataSource.class);
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {

                // Disable foreign key constraint checks (H2 syntax)
                statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

                // Clean tables in correct order to respect foreign key dependencies
                // leave_source_ref has foreign key to leave
                statement.execute("DELETE FROM leave_source_ref");
                log.debug("Deleted all data from leave_source_ref table");

                // leave table (no dependent tables)
                statement.execute("DELETE FROM leave");
                log.debug("Deleted all data from leave table");

                // audit_log (independent table)
                statement.execute("DELETE FROM audit_log");
                log.debug("Deleted all data from audit_log table");

                // Re-enable foreign key constraint checks
                statement.execute("SET REFERENTIAL_INTEGRITY TRUE");

                log.debug("Database cleanup completed successfully");
            }
        } catch (Exception e) {
            log.error("Failed to perform database cleanup", e);
            throw new RuntimeException("Failed to clean up database after test", e);
        }
    }
}
