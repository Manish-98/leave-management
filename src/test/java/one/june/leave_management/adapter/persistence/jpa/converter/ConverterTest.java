package one.june.leave_management.adapter.persistence.jpa.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JPA converters.
 * Tests JSON serialization/deserialization for complex map types.
 */
@DisplayName("JPA Converter Tests")
class ConverterTest {

    private CarryForwardLeavesConverter carryForwardLeavesConverter;
    private MetadataConverter metadataConverter;

    @BeforeEach
    void setUp() {
        carryForwardLeavesConverter = new CarryForwardLeavesConverter();
        metadataConverter = new MetadataConverter();
    }

    // ==================== CarryForwardLeavesConverter Tests ====================

    @Nested
    @DisplayName("CarryForwardLeavesConverter Tests")
    class CarryForwardLeavesConverterTests {

        @Test
        @DisplayName("Should convert valid map to JSON string")
        void shouldConvertValidMapToJson() {
            // Given
            Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
            carryForwardLeaves.put(2023, 5);
            carryForwardLeaves.put(2024, 3);

            // When
            String json = carryForwardLeavesConverter.convertToDatabaseColumn(carryForwardLeaves);

            // Then
            assertThat(json).isNotNull();
            assertThat(json).contains("2023");
            assertThat(json).contains("2024");
            assertThat(json).contains("5");
            assertThat(json).contains("3");
        }

        @Test
        @DisplayName("Should convert null map to empty JSON object")
        void shouldConvertNullMapToEmptyJson() {
            // When
            String json = carryForwardLeavesConverter.convertToDatabaseColumn(null);

            // Then
            assertThat(json).isEqualTo("{}");
        }

        @Test
        @DisplayName("Should convert empty map to empty JSON object")
        void shouldConvertEmptyMapToEmptyJson() {
            // Given
            Map<Integer, Integer> emptyMap = new HashMap<>();

            // When
            String json = carryForwardLeavesConverter.convertToDatabaseColumn(emptyMap);

            // Then
            assertThat(json).isEqualTo("{}");
        }

        @Test
        @DisplayName("Should parse valid JSON string to map")
        void shouldParseValidJsonToMap() {
            // Given
            String json = "{\"2023\":5,\"2024\":3}";

            // When
            Map<Integer, Integer> map = carryForwardLeavesConverter.convertToEntityAttribute(json);

            // Then
            assertThat(map).hasSize(2);
            assertThat(map.get(2023)).isEqualTo(5);
            assertThat(map.get(2024)).isEqualTo(3);
        }

        @Test
        @DisplayName("Should convert null JSON to empty map")
        void shouldConvertNullJsonToEmptyMap() {
            // When
            Map<Integer, Integer> map = carryForwardLeavesConverter.convertToEntityAttribute(null);

            // Then
            assertThat(map).isNotNull();
            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("Should convert empty JSON string to empty map")
        void shouldConvertEmptyJsonToEmptyMap() {
            // When
            Map<Integer, Integer> map = carryForwardLeavesConverter.convertToEntityAttribute("{}");

            // Then
            assertThat(map).isNotNull();
            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("Should handle JSON with whitespace")
        void shouldHandleJsonWithWhitespace() {
            // Given
            String json = " { \"2023\" : 5 } ";

            // When
            Map<Integer, Integer> map = carryForwardLeavesConverter.convertToEntityAttribute(json);

            // Then
            assertThat(map).hasSize(1);
            assertThat(map.get(2023)).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw exception for malformed JSON")
        void shouldThrowExceptionForMalformedJson() {
            // Given
            String malformedJson = "{2023:5}"; // Missing quotes

            // When & Then
            assertThatThrownBy(() -> carryForwardLeavesConverter.convertToEntityAttribute(malformedJson))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to convert JSON");
        }

        @Test
        @DisplayName("Should handle single entry in map")
        void shouldHandleSingleEntryInMap() {
            // Given
            Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
            carryForwardLeaves.put(2024, 10);

            // When
            String json = carryForwardLeavesConverter.convertToDatabaseColumn(carryForwardLeaves);
            Map<Integer, Integer> result = carryForwardLeavesConverter.convertToEntityAttribute(json);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(2024)).isEqualTo(10);
        }

        @Test
        @DisplayName("Should handle zero value in map")
        void shouldHandleZeroValueInMap() {
            // Given
            Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
            carryForwardLeaves.put(2024, 0);

            // When
            String json = carryForwardLeavesConverter.convertToDatabaseColumn(carryForwardLeaves);
            Map<Integer, Integer> result = carryForwardLeavesConverter.convertToEntityAttribute(json);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(2024)).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle multiple years with varying values")
        void shouldHandleMultipleYearsWithVaryingValues() {
            // Given
            Map<Integer, Integer> carryForwardLeaves = new HashMap<>();
            carryForwardLeaves.put(2022, 5);
            carryForwardLeaves.put(2023, 3);
            carryForwardLeaves.put(2024, 7);
            carryForwardLeaves.put(2025, 2);

            // When
            String json = carryForwardLeavesConverter.convertToDatabaseColumn(carryForwardLeaves);
            Map<Integer, Integer> result = carryForwardLeavesConverter.convertToEntityAttribute(json);

            // Then
            assertThat(result).hasSize(4);
            assertThat(result.get(2022)).isEqualTo(5);
            assertThat(result.get(2023)).isEqualTo(3);
            assertThat(result.get(2024)).isEqualTo(7);
            assertThat(result.get(2025)).isEqualTo(2);
        }
    }

    // ==================== MetadataConverter Tests ====================

    @Nested
    @DisplayName("MetadataConverter Tests")
    class MetadataConverterTests {

        @Test
        @DisplayName("Should convert valid metadata map to JSON string")
        void shouldConvertValidMetadataToJson() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userId", "U12345");
            metadata.put("source", "slack");

            // When
            String json = metadataConverter.convertToDatabaseColumn(metadata);

            // Then
            assertThat(json).isNotNull();
            assertThat(json).contains("userId");
            assertThat(json).contains("U12345");
            assertThat(json).contains("source");
            assertThat(json).contains("slack");
        }

        @Test
        @DisplayName("Should convert null metadata to empty JSON object")
        void shouldConvertNullMetadataToEmptyJson() {
            // When
            String json = metadataConverter.convertToDatabaseColumn(null);

            // Then
            assertThat(json).isEqualTo("{}");
        }

        @Test
        @DisplayName("Should convert empty metadata to empty JSON object")
        void shouldConvertEmptyMetadataToEmptyJson() {
            // Given
            Map<String, String> emptyMap = new HashMap<>();

            // When
            String json = metadataConverter.convertToDatabaseColumn(emptyMap);

            // Then
            assertThat(json).isEqualTo("{}");
        }

        @Test
        @DisplayName("Should parse valid JSON metadata to map")
        void shouldParseValidJsonMetadataToMap() {
            // Given
            String json = "{\"userId\":\"U12345\",\"source\":\"slack\"}";

            // When
            Map<String, String> map = metadataConverter.convertToEntityAttribute(json);

            // Then
            assertThat(map).hasSize(2);
            assertThat(map.get("userId")).isEqualTo("U12345");
            assertThat(map.get("source")).isEqualTo("slack");
        }

        @Test
        @DisplayName("Should convert null JSON metadata to empty map")
        void shouldConvertNullJsonMetadataToEmptyMap() {
            // When
            Map<String, String> map = metadataConverter.convertToEntityAttribute(null);

            // Then
            assertThat(map).isNotNull();
            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("Should convert empty JSON metadata to empty map")
        void shouldConvertEmptyJsonMetadataToEmptyMap() {
            // When
            Map<String, String> map = metadataConverter.convertToEntityAttribute("{}");

            // Then
            assertThat(map).isNotNull();
            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception for malformed JSON metadata")
        void shouldThrowExceptionForMalformedJsonMetadata() {
            // Given
            String malformedJson = "{userId:U12345}"; // Missing quotes

            // When & Then
            assertThatThrownBy(() -> metadataConverter.convertToEntityAttribute(malformedJson))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Could not convert JSON");
        }

        @Test
        @DisplayName("Should handle special characters in metadata values")
        void shouldHandleSpecialCharactersInMetadata() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("message", "Hello, \"World\"!");
            metadata.put("data", "{\"key\":\"value\"}");

            // When
            String json = metadataConverter.convertToDatabaseColumn(metadata);
            Map<String, String> result = metadataConverter.convertToEntityAttribute(json);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get("message")).isEqualTo("Hello, \"World\"!");
            assertThat(result.get("data")).isEqualTo("{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("Should handle empty string values in metadata")
        void shouldHandleEmptyStringValuesInMetadata() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userId", "U12345");
            metadata.put("comment", "");

            // When
            String json = metadataConverter.convertToDatabaseColumn(metadata);
            Map<String, String> result = metadataConverter.convertToEntityAttribute(json);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get("userId")).isEqualTo("U12345");
            assertThat(result.get("comment")).isEqualTo("");
        }
    }
}
