package one.june.leave_management.adapter.outbound.slack.dto.composition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SlackText
 */
class SlackTextTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testPlainTextCreation() {
        SlackText text = SlackText.plainText("Hello World");

        assertThat(text.getType()).isEqualTo("plain_text");
        assertThat(text.getText()).isEqualTo("Hello World");
        assertThat(text.getEmoji()).isNull();
    }

    @Test
    void testPlainTextWithEmojiCreation() {
        SlackText text = SlackText.plainText("Hello World", true);

        assertThat(text.getType()).isEqualTo("plain_text");
        assertThat(text.getText()).isEqualTo("Hello World");
        assertThat(text.getEmoji()).isTrue();
    }

    @Test
    void testMarkdownCreation() {
        SlackText text = SlackText.markdown("*Hello* World");

        assertThat(text.getType()).isEqualTo("mrkdwn");
        assertThat(text.getText()).isEqualTo("*Hello* World");
        assertThat(text.getEmoji()).isNull();
    }

    @Test
    void testPlainTextSerialization() throws JsonProcessingException {
        SlackText text = SlackText.plainText("Hello", true);

        String json = objectMapper.writeValueAsString(text);

        assertThat(json).contains("\"type\":\"plain_text\"");
        assertThat(json).contains("\"text\":\"Hello\"");
        assertThat(json).contains("\"emoji\":true");
    }

    @Test
    void testPlainTextWithoutEmojiSerialization() throws JsonProcessingException {
        SlackText text = SlackText.plainText("Hello");

        String json = objectMapper.writeValueAsString(text);

        // Emoji should not be in JSON when null (NON_NULL annotation)
        assertThat(json).doesNotContain("emoji");
        assertThat(json).contains("\"type\":\"plain_text\"");
        assertThat(json).contains("\"text\":\"Hello\"");
    }

    @Test
    void testFieldOrder() throws JsonProcessingException {
        SlackText text = SlackText.plainText("Hello", true);

        String json = objectMapper.writeValueAsString(text);

        // Verify field order: type, text, emoji (as per @JsonPropertyOrder)
        int typeIndex = json.indexOf("\"type\"");
        int textIndex = json.indexOf("\"text\"");
        int emojiIndex = json.indexOf("\"emoji\"");

        assertThat(typeIndex).isLessThan(textIndex);
        assertThat(textIndex).isLessThan(emojiIndex);
    }
}
