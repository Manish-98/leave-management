package one.june.leave_management.adapter.outbound.sync;

import one.june.leave_management.application.leave.service.OutboundSyncService;
import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StubOutboundSyncService implements OutboundSyncService {
    private static final Logger logger = LoggerFactory.getLogger(StubOutboundSyncService.class);

    @Override
    public void sync(Leave leave, SourceType originatingSource) {
        logger.info("=== OUTBOUND SYNC STUB ===");
        logger.info("Syncing leave {} to external systems", leave.getId());
        logger.info("Originating source: {}", originatingSource);
        logger.info("User ID: {}", leave.getUserId());
        logger.info("Leave period: {} to {}", leave.getStartDate(), leave.getEndDate());
        logger.info("Leave type: {}", leave.getType());
        logger.info("Leave status: {}", leave.getStatus());
        logger.info("Source references count: {}", leave.getSourceRefs().size());

        // In a real implementation, this would:
        // - Sync to Slack (if originatingSource != SLACK)
        // - Sync to Google Calendar (if originatingSource != CALENDAR)
        // - Sync to Kimai (if originatingSource != KIMAI)
        // - Handle conflicts and retries
        // - Log success/failure

        logger.info("=== END SYNC STUB ===");
    }
}