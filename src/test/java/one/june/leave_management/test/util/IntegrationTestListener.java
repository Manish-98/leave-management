package one.june.leave_management.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Test execution listener for integration tests.
 *
 * <p>Performs automatic database cleanup after each test method.
 *
 * <p>For non-transactional tests (transactional=false), performs full SQL cleanup
 * of all tables after each test method.
 *
 * <p>For transactional tests (transactional=true, the default), Spring's @Transactional
 * rolls back test data, but @BeforeTransaction data is committed separately and persists.
 * Therefore, we clean up employee and optional_holidays tables (which are typically
 * set up in @BeforeTransaction) after each test.
 *
 * <p>Cleanup is performed in the correct order to respect foreign key constraints:
 * <ol>
 *   <li>leave_source_ref (has foreign key to leave)</li>
 *   <li>leave (has no dependent tables)</li>
 *   <li>optional_holidays (independent table, used in @BeforeTransaction)</li>
 *   <li>employee (independent table, used in @BeforeTransaction)</li>
 *   <li>audit_log (independent table)</li>
 * </ol>
 */
@Slf4j
public class IntegrationTestListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestMethod(TestContext testContext) {
        // Check if the test class has @IntegrationTest annotation
        IntegrationTest integrationTest = testContext.getTestClass()
                .getAnnotation(IntegrationTest.class);

        if (integrationTest == null) {
            log.debug("No @IntegrationTest annotation found, skipping cleanup");
            return; // Not an @IntegrationTest, skip cleanup
        }

        log.debug("Test class: {}, transactional: {}", testContext.getTestClass().getName(), integrationTest.transactional());

        // Always perform cleanup
        // - For non-transactional tests: clean all tables
        // - For transactional tests: clean employee and optional_holidays (@BeforeTransaction data persists)
        log.debug("Performing SQL cleanup");
        cleanupDatabase(testContext, integrationTest.transactional());
    }

    /**
     * Performs SQL-based cleanup of database tables.
     *
     * <p>Executes DELETE statements to clean data while preserving
     * the schema (tables, constraints, sequences).
     *
     * @param testContext the test context
     * @param isTransactional whether the test is transactional
     */
    private void cleanupDatabase(TestContext testContext, boolean isTransactional) {
        try {
            DataSource dataSource = testContext.getApplicationContext().getBean(DataSource.class);
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {

                // Disable foreign key constraint checks (H2 syntax)
                statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

                if (!isTransactional) {
                    // Non-transactional: Clean all tables (test data is committed)
                    statement.execute("DELETE FROM leave_source_ref");
                    log.debug("Deleted all data from leave_source_ref table");

                    statement.execute("DELETE FROM leave");
                    log.debug("Deleted all data from leave table");
                } else {
                    // Transactional: Clean leave_source_ref (needed for @BeforeTransaction cleanup)
                    // Tests roll back leave data, but we need to clean up for next test's @BeforeTransaction
                    statement.execute("DELETE FROM leave_source_ref");
                    log.debug("Deleted all data from leave_source_ref table (transactional test)");

                    statement.execute("DELETE FROM leave");
                    log.debug("Deleted all data from leave table (transactional test)");
                }

                // Always clean employee and optional_holidays (@BeforeTransaction data persists)
                statement.execute("DELETE FROM optional_holidays");
                log.debug("Deleted all data from optional_holidays table");

                statement.execute("DELETE FROM employee");
                log.debug("Deleted all data from employee table");

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
