package one.june.leave_management.domain.leave.validation;

import one.june.leave_management.domain.leave.model.LeaveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;

/**
 * Registry for leave validation strategies.
 * Auto-registers all LeaveValidationStrategyBase beans and provides lookup by leave type.
 */
@Component
public class LeaveValidationStrategyRegistry {
    private static final Logger logger = LoggerFactory.getLogger(LeaveValidationStrategyRegistry.class);

    private final EnumMap<LeaveType, LeaveValidationStrategyBase> strategies;

    /**
     * Auto-registers all strategy beans via Spring dependency injection.
     *
     * @param strategyBeans list of all strategy beans in the application context
     */
    public LeaveValidationStrategyRegistry(List<LeaveValidationStrategyBase> strategyBeans) {
        this.strategies = new EnumMap<>(LeaveType.class);

        for (LeaveValidationStrategyBase strategy : strategyBeans) {
            LeaveType type = strategy.getType();
            if (strategies.containsKey(type)) {
                logger.warn("Duplicate strategy registration for leave type: {}. Overwriting with {}", type, strategy.getClass().getSimpleName());
            }
            strategies.put(type, strategy);
            logger.debug("Registered validation strategy {} for leave type: {}", strategy.getClass().getSimpleName(), type);
        }

        logger.info("Registered {} leave validation strategies", strategies.size());
    }

    /**
     * Gets the validation strategy for the given leave type.
     *
     * @param type the leave type
     * @return the validation strategy
     * @throws IllegalArgumentException if no strategy is registered for the type
     */
    public LeaveValidationStrategyBase getStrategy(LeaveType type) {
        LeaveValidationStrategyBase strategy = strategies.get(type);
        if (strategy == null) {
            logger.error("No validation strategy registered for leave type: {}", type);
            throw new IllegalArgumentException("No validation strategy registered for: " + type);
        }
        return strategy;
    }

    /**
     * Checks if a strategy is registered for the given leave type.
     *
     * @param type the leave type
     * @return true if a strategy is registered, false otherwise
     */
    public boolean hasStrategy(LeaveType type) {
        return strategies.containsKey(type);
    }
}
