package one.june.leave_management.adapter.inbound.web.csv;

import lombok.extern.slf4j.Slf4j;
import one.june.leave_management.domain.leave.model.BulkUploadType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry for CSV parser strategies.
 * <p>
 * This class maintains a map of all available CSV parser strategies indexed by their
 * bulk upload type. It provides a simple way to retrieve the appropriate parser strategy
 * for a given bulk upload type.
 * <p>
 * Strategies are auto-registered through Spring dependency injection.
 */
@Component
@Slf4j
public class CsvParserStrategyRegistry {

    private final Map<BulkUploadType, CsvParserStrategy<?>> strategyMap;

    /**
     * Constructor that auto-registers all CSV parser strategy beans.
     *
     * @param strategies List of all available CSV parser strategies (auto-injected by Spring)
     */
    public CsvParserStrategyRegistry(List<CsvParserStrategy<?>> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        CsvParserStrategy::getType,
                        Function.identity()
                ));

        log.info("Registered {} CSV parser strategies: {}",
                strategyMap.size(),
                strategyMap.keySet());
    }

    /**
     * Get the CSV parser strategy for the given bulk upload type.
     *
     * @param type The bulk upload type
     * @return The corresponding CSV parser strategy
     * @throws IllegalArgumentException if no strategy found for the type
     */
    @SuppressWarnings("unchecked")
    public <T> CsvParserStrategy<T> getStrategy(BulkUploadType type) {
        CsvParserStrategy<?> strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No CSV parser strategy found for type: " + type);
        }
        return (CsvParserStrategy<T>) strategy;
    }

    /**
     * Check if a strategy exists for the given bulk upload type.
     *
     * @param type The bulk upload type
     * @return true if a strategy exists, false otherwise
     */
    public boolean hasStrategy(BulkUploadType type) {
        return strategyMap.containsKey(type);
    }
}
