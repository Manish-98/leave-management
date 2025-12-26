package one.june.leave_management.adapter.inbound.slack.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SlackMetadataUtil}
 * <p>
 * Tests JSON encoding/decoding of Slack modal metadata with all edge cases
 */
@DisplayName("SlackMetadataUtil Unit Tests")
class SlackMetadataUtilTest {

    @Nested
    @DisplayName("encodeMetadata() Tests")
    class EncodeMetadataTests {

        @Test
        @DisplayName("Should successfully encode valid metadata object with all fields")
        void shouldEncodeValidMetadataWithAllFields() {
            // Given
            SlackMetadataUtil.SlackModalMetadata metadata = new SlackMetadataUtil.SlackModalMetadata(
                    "U12345",
                    "C67890",
                    "general",
                    "1234567890.123456"
            );

            // When
            String encoded = SlackMetadataUtil.encodeMetadata(metadata);

            // Then
            assertThat(encoded).isNotNull();
            assertThat(encoded).contains("\"userId\":\"U12345\"");
            assertThat(encoded).contains("\"channelId\":\"C67890\"");
            assertThat(encoded).contains("\"channelName\":\"general\"");
            assertThat(encoded).contains("\"threadTs\":\"1234567890.123456\"");
        }

        @Test
        @DisplayName("Should successfully encode metadata with null optional fields")
        void shouldEncodeMetadataWithNullOptionalFields() {
            // Given
            SlackMetadataUtil.SlackModalMetadata metadata = new SlackMetadataUtil.SlackModalMetadata(
                    "U12345",
                    "C67890",
                    "general",
                    null
            );

            // When
            String encoded = SlackMetadataUtil.encodeMetadata(metadata);

            // Then
            assertThat(encoded).isNotNull();
            assertThat(encoded).contains("\"userId\":\"U12345\"");
            assertThat(encoded).contains("\"channelId\":\"C67890\"");
            assertThat(encoded).contains("\"channelName\":\"general\"");
            assertThat(encoded).contains("\"threadTs\":null");
        }

        @Test
        @DisplayName("Should successfully encode metadata with empty strings")
        void shouldEncodeMetadataWithEmptyStrings() {
            // Given
            SlackMetadataUtil.SlackModalMetadata metadata = new SlackMetadataUtil.SlackModalMetadata(
                    "",
                    "",
                    "",
                    ""
            );

            // When
            String encoded = SlackMetadataUtil.encodeMetadata(metadata);

            // Then
            assertThat(encoded).isNotNull();
            assertThat(encoded).contains("\"userId\":\"\"");
            assertThat(encoded).contains("\"channelId\":\"\"");
            assertThat(encoded).contains("\"channelName\":\"\"");
            assertThat(encoded).contains("\"threadTs\":\"\"");
        }

        @Test
        @DisplayName("Should serialize null metadata as JSON null string")
        void shouldSerializeNullMetadataAsJsonNull() {
            // When & Then - Jackson serializes null as "null" string
            String result = SlackMetadataUtil.encodeMetadata(null);
            assertThat(result).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("decodeMetadata() Tests")
    class DecodeMetadataTests {

        @Test
        @DisplayName("Should successfully decode valid JSON with all fields")
        void shouldDecodeValidJsonWithAllFields() {
            // Given
            String json = """
                    {
                        "userId": "U12345",
                        "channelId": "C67890",
                        "channelName": "general",
                        "threadTs": "1234567890.123456"
                    }
                    """;

            // When
            SlackMetadataUtil.SlackModalMetadata metadata = SlackMetadataUtil.decodeMetadata(json);

            // Then
            assertThat(metadata).isNotNull();
            assertThat(metadata.getUserId()).isEqualTo("U12345");
            assertThat(metadata.getChannelId()).isEqualTo("C67890");
            assertThat(metadata.getChannelName()).isEqualTo("general");
            assertThat(metadata.getThreadTs()).isEqualTo("1234567890.123456");
        }

        @Test
        @DisplayName("Should successfully decode JSON with null optional fields")
        void shouldDecodeJsonWithNullOptionalFields() {
            // Given
            String json = """
                    {
                        "userId": "U12345",
                        "channelId": "C67890",
                        "channelName": "general",
                        "threadTs": null
                    }
                    """;

            // When
            SlackMetadataUtil.SlackModalMetadata metadata = SlackMetadataUtil.decodeMetadata(json);

            // Then
            assertThat(metadata).isNotNull();
            assertThat(metadata.getUserId()).isEqualTo("U12345");
            assertThat(metadata.getChannelId()).isEqualTo("C67890");
            assertThat(metadata.getChannelName()).isEqualTo("general");
            assertThat(metadata.getThreadTs()).isNull();
        }

        @Test
        @DisplayName("Should successfully decode JSON with empty strings")
        void shouldDecodeJsonWithEmptyStrings() {
            // Given
            String json = """
                    {
                        "userId": "",
                        "channelId": "",
                        "channelName": "",
                        "threadTs": ""
                    }
                    """;

            // When
            SlackMetadataUtil.SlackModalMetadata metadata = SlackMetadataUtil.decodeMetadata(json);

            // Then
            assertThat(metadata).isNotNull();
            assertThat(metadata.getUserId()).isEmpty();
            assertThat(metadata.getChannelId()).isEmpty();
            assertThat(metadata.getChannelName()).isEmpty();
            assertThat(metadata.getThreadTs()).isEmpty();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when input is null")
        void shouldThrowExceptionWhenDecodingNullInput() {
            // When
            assertThatThrownBy(() -> SlackMetadataUtil.decodeMetadata(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Metadata JSON cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when input is empty string")
        void shouldThrowExceptionWhenDecodingEmptyString() {
            // When
            assertThatThrownBy(() -> SlackMetadataUtil.decodeMetadata(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Metadata JSON cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when JSON is malformed")
        void shouldThrowExceptionWhenDecodingMalformedJson() {
            // Given
            String malformedJson = "{invalid json}";

            // When
            assertThatThrownBy(() -> SlackMetadataUtil.decodeMetadata(malformedJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to decode metadata");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for unknown fields in JSON")
        void shouldThrowExceptionForUnknownFieldsInJson() {
            // Given
            String json = """
                    {
                        "userId": "U12345",
                        "channelId": "C67890",
                        "channelName": "general",
                        "threadTs": "1234567890.123456",
                        "unknownField": "someValue",
                        "anotherUnknown": 123
                    }
                    """;

            // When & Then - Jackson does not ignore unknown fields by default
            assertThatThrownBy(() -> SlackMetadataUtil.decodeMetadata(json))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to decode metadata");
        }
    }

    @Nested
    @DisplayName("createMetadata() Tests")
    class CreateMetadataTests {

        @Test
        @DisplayName("Should successfully create metadata string from all components")
        void shouldSuccessfullyCreateMetadataFromAllComponents() {
            // When
            String metadata = SlackMetadataUtil.createMetadata(
                    "U12345",
                    "C67890",
                    "general",
                    "1234567890.123456"
            );

            // Then
            assertThat(metadata).isNotNull();
            assertThat(metadata).contains("\"userId\":\"U12345\"");
            assertThat(metadata).contains("\"channelId\":\"C67890\"");
            assertThat(metadata).contains("\"channelName\":\"general\"");
            assertThat(metadata).contains("\"threadTs\":\"1234567890.123456\"");
        }

        @Test
        @DisplayName("Should successfully create metadata with null threadTs")
        void shouldSuccessfullyCreateMetadataWithNullThreadTs() {
            // When
            String metadata = SlackMetadataUtil.createMetadata(
                    "U12345",
                    "C67890",
                    "general",
                    null
            );

            // Then
            assertThat(metadata).isNotNull();
            assertThat(metadata).contains("\"userId\":\"U12345\"");
            assertThat(metadata).contains("\"channelId\":\"C67890\"");
            assertThat(metadata).contains("\"channelName\":\"general\"");
            assertThat(metadata).contains("\"threadTs\":null");
        }
    }

    @Nested
    @DisplayName("Extract Field Tests")
    class ExtractFieldTests {

        @Test
        @DisplayName("Should successfully extract userId from valid metadata")
        void shouldSuccessfullyExtractUserId() {
            // Given
            String json = """
                    {
                        "userId": "U12345",
                        "channelId": "C67890",
                        "channelName": "general",
                        "threadTs": "1234567890.123456"
                    }
                    """;

            // When
            String userId = SlackMetadataUtil.extractUserId(json);

            // Then
            assertThat(userId).isEqualTo("U12345");
        }

        @Test
        @DisplayName("Should successfully extract channelId from valid metadata")
        void shouldSuccessfullyExtractChannelId() {
            // Given
            String json = """
                    {
                        "userId": "U12345",
                        "channelId": "C67890",
                        "channelName": "general",
                        "threadTs": "1234567890.123456"
                    }
                    """;

            // When
            String channelId = SlackMetadataUtil.extractChannelId(json);

            // Then
            assertThat(channelId).isEqualTo("C67890");
        }

        @Test
        @DisplayName("Should successfully extract threadTs from valid metadata")
        void shouldSuccessfullyExtractThreadTs() {
            // Given
            String json = """
                    {
                        "userId": "U12345",
                        "channelId": "C67890",
                        "channelName": "general",
                        "threadTs": "1234567890.123456"
                    }
                    """;

            // When
            String threadTs = SlackMetadataUtil.extractThreadTs(json);

            // Then
            assertThat(threadTs).isEqualTo("1234567890.123456");
        }

        @Test
        @DisplayName("Should successfully extract channelName from valid metadata")
        void shouldSuccessfullyExtractChannelName() {
            // Given
            String json = """
                    {
                        "userId": "U12345",
                        "channelId": "C67890",
                        "channelName": "general",
                        "threadTs": "1234567890.123456"
                    }
                    """;

            // When
            String channelName = SlackMetadataUtil.extractChannelName(json);

            // Then
            assertThat(channelName).isEqualTo("general");
        }

        @Test
        @DisplayName("Should propagate IllegalArgumentException from decodeMetadata when input is invalid")
        void shouldPropagateExceptionFromDecodeMetadata() {
            // Given
            String invalidJson = "{invalid}";

            // When & Then - All extract methods should propagate the exception
            assertThatThrownBy(() -> SlackMetadataUtil.extractUserId(invalidJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to decode metadata");

            assertThatThrownBy(() -> SlackMetadataUtil.extractChannelId(invalidJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to decode metadata");

            assertThatThrownBy(() -> SlackMetadataUtil.extractThreadTs(invalidJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to decode metadata");

            assertThatThrownBy(() -> SlackMetadataUtil.extractChannelName(invalidJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to decode metadata");
        }
    }
}
