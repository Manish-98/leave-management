package one.june.leave_management.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark controller methods that should be audited.
 * When applied to a method, all requests and responses will be logged
 * to the audit_log table via the AuditAspect.
 *
 * <p>Usage example:
 * <pre>
 * &#64;PostMapping("/ingest")
 * &#64;Auditable("Leave ingestion endpoint")
 * public ResponseEntity&lt;LeaveDto&gt; ingestLeave(@RequestBody LeaveIngestionRequest request) {
 *     // method implementation
 * }
 * </pre>
 *
 * <p>The audit aspect will capture:
 * <ul>
 *   <li>Full request body</li>
 *   <li>Full response body</li>
 *   <li>HTTP method and endpoint</li>
 *   <li>Execution time</li>
 *   <li>Error messages if applicable</li>
 *   <li>Request correlation ID</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Optional description of what is being audited.
     * This is stored in the audit log for documentation purposes.
     *
     * @return description of the audited operation
     */
    String value() default "";
}
