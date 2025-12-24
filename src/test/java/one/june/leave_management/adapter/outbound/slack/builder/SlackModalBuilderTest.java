package one.june.leave_management.adapter.outbound.slack.builder;

import one.june.leave_management.adapter.outbound.slack.dto.SlackModalView;
import one.june.leave_management.adapter.outbound.slack.dto.blocks.SlackInputBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SlackModalBuilder
 */
class SlackModalBuilderTest {

    @Test
    void testBasicModalCreation() {
        SlackModalView modal = SlackModalBuilder.create("Test Modal", "test_callback")
                .build();

        assertThat(modal.getType()).isEqualTo("modal");
        assertThat(modal.getTitle().getText()).isEqualTo("Test Modal");
        assertThat(modal.getCallbackId()).isEqualTo("test_callback");
        assertThat(modal.getSubmit().getText()).isEqualTo("Submit");
        assertThat(modal.getClose().getText()).isEqualTo("Cancel");
    }

    @Test
    void testModalWithBlocks() {
        SlackInputBlock block = SlackBlockBuilder.dateInput(
                "date_block",
                "date_action",
                "Date",
                "Select...",
                false
        );

        SlackModalView modal = SlackModalBuilder.create("Test Modal", "test_callback")
                .withBlock(block)
                .build();

        assertThat(modal.getBlocks()).hasSize(1);
        assertThat(modal.getBlocks().get(0)).isInstanceOf(SlackInputBlock.class);
    }

    @Test
    void testModalWithMultipleBlocks() {
        SlackInputBlock block1 = SlackBlockBuilder.dateInput("b1", "a1", "D1", "S1", false);
        SlackInputBlock block2 = SlackBlockBuilder.dateInput("b2", "a2", "D2", "S2", false);

        SlackModalView modal = SlackModalBuilder.create("Test", "cb")
                .withBlocks(List.of(block1, block2))
                .build();

        assertThat(modal.getBlocks()).hasSize(2);
    }

    @Test
    void testModalWithPrivateMetadata() {
        SlackModalView modal = SlackModalBuilder.create("Test", "cb")
                .withPrivateMetadata("user123")
                .build();

        assertThat(modal.getPrivateMetadata()).isEqualTo("user123");
    }

    @Test
    void testCustomSubmitAndCloseText() {
        SlackModalView modal = SlackModalBuilder.create("Test", "cb")
                .withSubmitText("Send Request")
                .withCloseText("Abort")
                .build();

        assertThat(modal.getSubmit().getText()).isEqualTo("Send Request");
        assertThat(modal.getClose().getText()).isEqualTo("Abort");
    }

    @Test
    void testClearOnClose() {
        SlackModalView modal = SlackModalBuilder.create("Test", "cb")
                .withClearOnClose(false)
                .build();

        assertThat(modal.getClearOnClose()).isFalse();
    }

    @Test
    void testMethodChaining() {
        SlackInputBlock block1 = SlackBlockBuilder.dateInput("b1", "a1", "D1", "S1", false);
        SlackInputBlock block2 = SlackBlockBuilder.dateInput("b2", "a2", "D2", "S2", false);

        SlackModalView modal = SlackModalBuilder.create("Test", "cb")
                .withBlock(block1)
                .withBlock(block2)
                .withSubmitText("Submit")
                .withCloseText("Cancel")
                .withPrivateMetadata("metadata")
                .withClearOnClose(true)
                .build();

        assertThat(modal.getBlocks()).hasSize(2);
        assertThat(modal.getPrivateMetadata()).isEqualTo("metadata");
        assertThat(modal.getClearOnClose()).isTrue();
    }

    @Test
    void testExternalIdIsGenerated() {
        SlackModalView modal1 = SlackModalBuilder.create("Test", "cb")
                .build();

        // Small delay to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        SlackModalView modal2 = SlackModalBuilder.create("Test", "cb")
                .build();

        assertThat(modal1.getExternalId()).isNotNull();
        assertThat(modal2.getExternalId()).isNotNull();
        assertThat(modal1.getExternalId()).isNotEqualTo(modal2.getExternalId());
    }

    @Test
    void testDefaultEmojiEnabled() {
        SlackModalView modal = SlackModalBuilder.create("Test", "cb")
                .build();

        assertThat(modal.getTitle().getEmoji()).isTrue();
        assertThat(modal.getSubmit().getEmoji()).isTrue();
        assertThat(modal.getClose().getEmoji()).isTrue();
    }
}
