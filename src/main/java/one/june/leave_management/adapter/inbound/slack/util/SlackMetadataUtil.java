package one.june.leave_management.adapter.inbound.slack.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * Utility class for encoding and decoding Slack modal private_metadata
 * <p>
 * Slack's private_metadata field is a string field with limited length.
 * This utility provides methods to serialize/deserialize metadata objects
 * and handle encoding for safe transport.
 * <p>
 * Metadata structure:
 * - userId: Slack user ID who initiated the request
 * - channelId: Channel ID where the request was initiated
 * - channelName: Channel name where the request was initiated
 * - threadTs: Thread timestamp for posting threaded replies
 */
@Slf4j
public class SlackMetadataUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Metadata object stored in Slack private_metadata
     */
    public static class SlackModalMetadata {
        private String userId;
        private String channelId;
        private String channelName;
        private String threadTs;

        public SlackModalMetadata() {
        }

        public SlackModalMetadata(String userId, String channelId, String channelName, String threadTs) {
            this.userId = userId;
            this.channelId = channelId;
            this.channelName = channelName;
            this.threadTs = threadTs;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getChannelId() {
            return channelId;
        }

        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public String getThreadTs() {
            return threadTs;
        }

        public void setThreadTs(String threadTs) {
            this.threadTs = threadTs;
        }
    }

    /**
     * Encodes metadata object to JSON string for private_metadata field
     *
     * @param metadata The metadata object to encode
     * @return JSON string representation
     * @throws IllegalArgumentException if encoding fails
     */
    public static String encodeMetadata(SlackModalMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to encode Slack modal metadata", e);
            throw new IllegalArgumentException("Failed to encode metadata", e);
        }
    }

    /**
     * Decodes JSON string from private_metadata field to metadata object
     *
     * @param metadataJson The JSON string from private_metadata
     * @return Decoded metadata object
     * @throws IllegalArgumentException if decoding fails
     */
    public static SlackModalMetadata decodeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty()) {
            throw new IllegalArgumentException("Metadata JSON cannot be null or empty");
        }

        try {
            return objectMapper.readValue(metadataJson, SlackModalMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to decode Slack modal metadata: {}", metadataJson, e);
            throw new IllegalArgumentException("Failed to decode metadata", e);
        }
    }

    /**
     * Creates metadata object from individual components
     *
     * @param userId     Slack user ID
     * @param channelId  Channel ID
     * @param channelName Channel name
     * @param threadTs   Thread timestamp
     * @return Encoded JSON string
     */
    public static String createMetadata(String userId, String channelId, String channelName, String threadTs) {
        SlackModalMetadata metadata = new SlackModalMetadata(userId, channelId, channelName, threadTs);
        return encodeMetadata(metadata);
    }

    /**
     * Extracts user ID from metadata JSON
     *
     * @param metadataJson The JSON string from private_metadata
     * @return User ID
     */
    public static String extractUserId(String metadataJson) {
        return decodeMetadata(metadataJson).getUserId();
    }

    /**
     * Extracts channel ID from metadata JSON
     *
     * @param metadataJson The JSON string from private_metadata
     * @return Channel ID
     */
    public static String extractChannelId(String metadataJson) {
        return decodeMetadata(metadataJson).getChannelId();
    }

    /**
     * Extracts thread timestamp from metadata JSON
     *
     * @param metadataJson The JSON string from private_metadata
     * @return Thread timestamp
     */
    public static String extractThreadTs(String metadataJson) {
        return decodeMetadata(metadataJson).getThreadTs();
    }

    /**
     * Extracts channel name from metadata JSON
     *
     * @param metadataJson The JSON string from private_metadata
     * @return Channel name
     */
    public static String extractChannelName(String metadataJson) {
        return decodeMetadata(metadataJson).getChannelName();
    }
}
