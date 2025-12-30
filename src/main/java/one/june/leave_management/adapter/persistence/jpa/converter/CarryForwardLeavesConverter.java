package one.june.leave_management.adapter.persistence.jpa.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA AttributeConverter for converting Map<Integer, Integer> to/from JSON for JSONB storage.
 * This enables storing carry forward leaves data as a JSON object in PostgreSQL.
 */
@Slf4j
@Converter(autoApply = false)
public class CarryForwardLeavesConverter implements AttributeConverter<Map<Integer, Integer>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts Map<Integer, Integer> to JSON string for database storage.
     * Returns an empty JSON object "{}" if the map is null or empty.
     *
     * @param attribute the map to convert
     * @return JSON string representation
     */
    @Override
    public String convertToDatabaseColumn(Map<Integer, Integer> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert carry forward leaves map to JSON", e);
            throw new IllegalStateException("Failed to convert carry forward leaves map to JSON", e);
        }
    }

    /**
     * Converts JSON string from database to Map<Integer, Integer>.
     * Returns an empty map if the JSON string is null or empty.
     *
     * @param dbData the JSON string from database
     * @return Map<Integer, Integer> representation
     */
    @Override
    public Map<Integer, Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "{}".equals(dbData.trim())) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<Integer, Integer>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to carry forward leaves map. JSON: {}", dbData, e);
            throw new IllegalStateException("Failed to convert JSON to carry forward leaves map", e);
        }
    }
}
