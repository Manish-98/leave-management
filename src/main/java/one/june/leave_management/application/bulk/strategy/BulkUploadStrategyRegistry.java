package one.june.leave_management.application.bulk.strategy;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for bulk upload strategies.
 * Auto-registers all BulkUploadStrategy beans and provides lookup by type.
 */
@Component
@Slf4j
public class BulkUploadStrategyRegistry {

    private final Map<BulkUploadType, BulkUploadStrategy> strategies;

    /**
     * Constructor that auto-registers all strategy beans.
     * Spring automatically injects all beans implementing BulkUploadStrategy.
     *
     * @param strategyBeans List of all strategy beans in the application context
     */
    public BulkUploadStrategyRegistry(List<BulkUploadStrategy> strategyBeans) {
        this.strategies = new EnumMap<>(BulkUploadType.class);

        for (BulkUploadStrategy strategy : strategyBeans) {
            BulkUploadType type = strategy.getType();
            strategies.put(type, strategy);
            log.info("Registered bulk upload strategy for type: {}", type);
        }

        log.info("Bulk upload strategy registry initialized with {} strategies", strategies.size());
    }

    /**
     * Get the strategy for a given bulk upload type.
     *
     * @param type The bulk upload type
     * @return The strategy for that type
     * @throws IllegalArgumentException if no strategy is registered for the type
     */
    public BulkUploadStrategy getStrategy(BulkUploadType type) {
        BulkUploadStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No bulk upload strategy registered for type: " + type);
        }
        return strategy;
    }

    /**
     * Check if a strategy is registered for the given type.
     *
     * @param type The bulk upload type
     * @return true if a strategy is registered, false otherwise
     */
    public boolean hasStrategy(BulkUploadType type) {
        return strategies.containsKey(type);
    }
}
