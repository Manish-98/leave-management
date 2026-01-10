package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SlackBlockActionRequest
 * Tests deserialization and structure of block action payloads from Slack
 */
@DisplayName("SlackBlockActionRequest Unit Tests")
class SlackBlockActionRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("Deserialization Tests")
    class DeserializationTests {

        @Test
        @DisplayName("Should deserialize complete block action request")
        void shouldDeserializeCompleteBlockActionRequest() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"team\":{\"id\":\"T123\",\"domain\":\"example\"},"
                    + "\"user\":{\"id\":\"U123\",\"username\":\"testuser\",\"name\":\"Test User\",\"team_id\":\"T123\"},"
                    + "\"api_app_id\":\"A123\","
                    + "\"token\":\"verification_token\","
                    + "\"trigger_id\":\"trigger123\","
                    + "\"channel\":{\"id\":\"C123\",\"name\":\"test-channel\"},"
                    + "\"response_url\":\"https://hooks.slack.com/actions/T123/C123/secret\","
                    + "\"actions\":["
                    + "  {\"action_id\":\"confirm_button\",\"block_id\":\"block1\",\"type\":\"button\",\"value\":\"confirm\"}"
                    + "]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request).isNotNull();
            assertThat(request.getType()).isEqualTo("block_actions");
            assertThat(request.getTeam().getId()).isEqualTo("T123");
            assertThat(request.getTeam().getDomain()).isEqualTo("example");
            assertThat(request.getUser().getId()).isEqualTo("U123");
            assertThat(request.getUser().getUsername()).isEqualTo("testuser");
            assertThat(request.getUser().getName()).isEqualTo("Test User");
            assertThat(request.getApiAppId()).isEqualTo("A123");
            assertThat(request.getToken()).isEqualTo("verification_token");
            assertThat(request.getTriggerId()).isEqualTo("trigger123");
            assertThat(request.getChannel().getId()).isEqualTo("C123");
            assertThat(request.getChannel().getName()).isEqualTo("test-channel");
            assertThat(request.getResponseUrl()).isEqualTo("https://hooks.slack.com/actions/T123/C123/secret");
            assertThat(request.getActions()).hasSize(1);
        }

        @Test
        @DisplayName("Should deserialize minimal block action request")
        void shouldDeserializeMinimalBlockActionRequest() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":["
                    + "  {\"action_id\":\"action1\",\"type\":\"button\"}"
                    + "]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request).isNotNull();
            assertThat(request.getType()).isEqualTo("block_actions");
            assertThat(request.getUser().getId()).isEqualTo("U123");
            assertThat(request.getActions()).hasSize(1);
        }

        @Test
        @DisplayName("Should deserialize container information")
        void shouldDeserializeContainerInformation() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"container\":{"
                    + "  \"type\":\"view\","
                    + "  \"view_id\":\"V123\","
                    + "  \"view_hash\":\"hash456\""
                    + "},"
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":[{\"action_id\":\"action1\",\"type\":\"button\"}]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request.getContainer()).isNotNull();
            assertThat(request.getContainer().getType()).isEqualTo("view");
            assertThat(request.getContainer().getViewId()).isEqualTo("V123");
            assertThat(request.getContainer().getViewHash()).isEqualTo("hash456");
        }

        @Test
        @DisplayName("Should deserialize message-based container")
        void shouldDeserializeMessageBasedContainer() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"container\":{"
                    + "  \"type\":\"message\","
                    + "  \"message_ts\":\"1234567890.123456\","
                    + "  \"channel_id\":\"C123\","
                    + "  \"thread_ts\":\"1234567890.123456\","
                    + "  \"is_ephemeral\":false"
                    + "},"
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":[{\"action_id\":\"action1\",\"type\":\"button\"}]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request.getContainer()).isNotNull();
            assertThat(request.getContainer().getType()).isEqualTo("message");
            assertThat(request.getContainer().getMessageTs()).isEqualTo("1234567890.123456");
            assertThat(request.getContainer().getChannelId()).isEqualTo("C123");
            assertThat(request.getContainer().getThreadTs()).isEqualTo("1234567890.123456");
            assertThat(request.getContainer().getIsEphemeral()).isFalse();
        }

        @Test
        @DisplayName("Should deserialize view object with state")
        void shouldDeserializeViewObjectWithState() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"view\":{"
                    + "  \"id\":\"V123\","
                    + "  \"team_id\":\"T123\","
                    + "  \"type\":\"modal\","
                    + "  \"callback_id\":\"leave_modal\","
                    + "  \"private_metadata\":\"{\\\"userId\\\":\\\"U123\\\"}\","
                    + "  \"external_id\":\"ext123\","
                    + "  \"hash\":\"hash789\""
                    + "},"
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":[{\"action_id\":\"action1\",\"type\":\"button\"}]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request.getView()).isNotNull();
            assertThat(request.getView().getId()).isEqualTo("V123");
            assertThat(request.getView().getTeamId()).isEqualTo("T123");
            assertThat(request.getView().getType()).isEqualTo("modal");
            assertThat(request.getView().getCallbackId()).isEqualTo("leave_modal");
            assertThat(request.getView().getPrivateMetadata()).isEqualTo("{\"userId\":\"U123\"}");
            assertThat(request.getView().getExternalId()).isEqualTo("ext123");
            assertThat(request.getView().getHash()).isEqualTo("hash789");
        }

        @Test
        @DisplayName("Should deserialize view with title")
        void shouldDeserializeViewWithTitle() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"view\":{"
                    + "  \"id\":\"V123\","
                    + "  \"title\":{"
                    + "    \"type\":\"plain_text\","
                    + "    \"text\":\"Leave Request\","
                    + "    \"emoji\":true"
                    + "  }"
                    + "},"
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":[{\"action_id\":\"action1\",\"type\":\"button\"}]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request.getView()).isNotNull();
            assertThat(request.getView().getTitle()).isNotNull();
            assertThat(request.getView().getTitle().getType()).isEqualTo("plain_text");
            assertThat(request.getView().getTitle().getText()).isEqualTo("Leave Request");
            assertThat(request.getView().getTitle().getEmoji()).isTrue();
        }

        @Test
        @DisplayName("Should deserialize state object")
        void shouldDeserializeStateObject() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"state\":{"
                    + "  \"values\":{"
                    + "    \"block_id\":{"
                    + "      \"action_id\":{"
                    + "        \"type\":\"plain_text_input\","
                    + "        \"value\":\"test value\""
                    + "      }"
                    + "    }"
                    + "  }"
                    + "},"
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":[{\"action_id\":\"action1\",\"type\":\"button\"}]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request.getState()).isNotNull();
        }

        @Test
        @DisplayName("Should deserialize message object")
        void shouldDeserializeMessageObject() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"message\":{"
                    + "  \"type\":\"message\","
                    + "  \"subtype\":\"bot_message\","
                    + "  \"ts\":\"1234567890.123456\","
                    + "  \"bot_id\":\"B123\","
                    + "  \"text\":\"Test message\""
                    + "},"
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":[{\"action_id\":\"action1\",\"type\":\"button\"}]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request.getMessage()).isNotNull();
            assertThat(request.getMessage().getType()).isEqualTo("message");
            assertThat(request.getMessage().getSubtype()).isEqualTo("bot_message");
            assertThat(request.getMessage().getTs()).isEqualTo("1234567890.123456");
            assertThat(request.getMessage().getBotId()).isEqualTo("B123");
            assertThat(request.getMessage().getText()).isEqualTo("Test message");
        }

        @Test
        @DisplayName("Should deserialize enterprise information")
        void shouldDeserializeEnterpriseInformation() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"enterprise\":{"
                    + "  \"id\":\"E123\","
                    + "  \"name\":\"Enterprise Inc\""
                    + "},"
                    + "\"is_enterprise_install\":true,"
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":[{\"action_id\":\"action1\",\"type\":\"button\"}]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request.getEnterprise()).isNotNull();
            assertThat(request.getEnterprise().getId()).isEqualTo("E123");
            assertThat(request.getEnterprise().getDomain()).isEqualTo("Enterprise Inc");
            assertThat(request.getIsEnterpriseInstall()).isTrue();
        }

        @Test
        @DisplayName("Should deserialize multiple actions")
        void shouldDeserializeMultipleActions() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":["
                    + "  {\"action_id\":\"action1\",\"block_id\":\"block1\",\"type\":\"button\",\"value\":\"value1\"},"
                    + "  {\"action_id\":\"action2\",\"block_id\":\"block2\",\"type\":\"button\",\"value\":\"value2\"},"
                    + "  {\"action_id\":\"action3\",\"block_id\":\"block3\",\"type\":\"button\",\"value\":\"value3\"}"
                    + "]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request.getActions()).hasSize(3);
        }

        @Test
        @DisplayName("Should ignore unknown properties")
        void shouldIgnoreUnknownProperties() throws JsonProcessingException {
            // Given
            String json = "{"
                    + "\"type\":\"block_actions\","
                    + "\"unknown_field\":\"unknown_value\","
                    + "\"another_unknown\":123,"
                    + "\"user\":{\"id\":\"U123\"},"
                    + "\"actions\":[{\"action_id\":\"action1\",\"type\":\"button\"}]"
                    + "}";

            // When
            SlackBlockActionRequest request = objectMapper.readValue(json, SlackBlockActionRequest.class);

            // Then
            assertThat(request).isNotNull();
            assertThat(request.getType()).isEqualTo("block_actions");
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build complete request using builder")
        void shouldBuildCompleteRequestUsingBuilder() {
            // Given
            SlackAction action = SlackAction.builder()
                    .actionId("confirm_button")
                    .blockId("block1")
                    .type("button")
                    .value("confirm")
                    .build();

            SlackBlockActionRequest.SlackTeam team = SlackBlockActionRequest.SlackTeam.builder()
                    .id("T123")
                    .domain("example")
                    .build();

            SlackBlockActionRequest.SlackUser user = SlackBlockActionRequest.SlackUser.builder()
                    .id("U123")
                    .username("testuser")
                    .name("Test User")
                    .teamId("T123")
                    .build();

            SlackBlockActionRequest.SlackChannel channel = SlackBlockActionRequest.SlackChannel.builder()
                    .id("C123")
                    .name("test-channel")
                    .build();

            // When
            SlackBlockActionRequest request = SlackBlockActionRequest.builder()
                    .type("block_actions")
                    .team(team)
                    .user(user)
                    .channel(channel)
                    .triggerId("trigger123")
                    .responseUrl("https://hooks.slack.com/actions/T123/C123/secret")
                    .actions(java.util.List.of(action))
                    .build();

            // Then
            assertThat(request.getType()).isEqualTo("block_actions");
            assertThat(request.getTeam().getId()).isEqualTo("T123");
            assertThat(request.getUser().getId()).isEqualTo("U123");
            assertThat(request.getChannel().getId()).isEqualTo("C123");
            assertThat(request.getTriggerId()).isEqualTo("trigger123");
            assertThat(request.getResponseUrl()).isEqualTo("https://hooks.slack.com/actions/T123/C123/secret");
            assertThat(request.getActions()).hasSize(1);
        }

        @Test
        @DisplayName("Should build container using builder")
        void shouldBuildContainerUsingBuilder() {
            // Given
            SlackBlockActionRequest.SlackContainer container = SlackBlockActionRequest.SlackContainer.builder()
                    .type("view")
                    .viewId("V123")
                    .viewHash("hash456")
                    .build();

            // When
            SlackBlockActionRequest request = SlackBlockActionRequest.builder()
                    .type("block_actions")
                    .container(container)
                    .user(SlackBlockActionRequest.SlackUser.builder().id("U123").build())
                    .actions(java.util.List.of())
                    .build();

            // Then
            assertThat(request.getContainer().getType()).isEqualTo("view");
            assertThat(request.getContainer().getViewId()).isEqualTo("V123");
            assertThat(request.getContainer().getViewHash()).isEqualTo("hash456");
        }

        @Test
        @DisplayName("Should build view using builder")
        void shouldBuildViewUsingBuilder() {
            // Given
            SlackBlockActionRequest.SlackView.SlackTextObject title = SlackBlockActionRequest.SlackView.SlackTextObject.builder()
                    .type("plain_text")
                    .text("Leave Request")
                    .emoji(true)
                    .build();

            SlackBlockActionRequest.SlackView view = SlackBlockActionRequest.SlackView.builder()
                    .id("V123")
                    .teamId("T123")
                    .type("modal")
                    .callbackId("leave_modal")
                    .privateMetadata("{\"userId\":\"U123\"}")
                    .title(title)
                    .externalId("ext123")
                    .hash("hash789")
                    .build();

            // When
            SlackBlockActionRequest request = SlackBlockActionRequest.builder()
                    .type("block_actions")
                    .view(view)
                    .user(SlackBlockActionRequest.SlackUser.builder().id("U123").build())
                    .actions(java.util.List.of())
                    .build();

            // Then
            assertThat(request.getView().getId()).isEqualTo("V123");
            assertThat(request.getView().getTeamId()).isEqualTo("T123");
            assertThat(request.getView().getType()).isEqualTo("modal");
            assertThat(request.getView().getCallbackId()).isEqualTo("leave_modal");
            assertThat(request.getView().getPrivateMetadata()).isEqualTo("{\"userId\":\"U123\"}");
            assertThat(request.getView().getTitle().getText()).isEqualTo("Leave Request");
            assertThat(request.getView().getTitle().getEmoji()).isTrue();
            assertThat(request.getView().getExternalId()).isEqualTo("ext123");
            assertThat(request.getView().getHash()).isEqualTo("hash789");
        }
    }

    @Nested
    @DisplayName("Nested Classes Tests")
    class NestedClassesTests {

        @Test
        @DisplayName("Should build SlackTeam with all fields")
        void shouldBuildSlackTeamWithAllFields() {
            // When
            SlackBlockActionRequest.SlackTeam team = SlackBlockActionRequest.SlackTeam.builder()
                    .id("T123")
                    .domain("example")
                    .build();

            // Then
            assertThat(team.getId()).isEqualTo("T123");
            assertThat(team.getDomain()).isEqualTo("example");
        }

        @Test
        @DisplayName("Should build SlackEnterprise with all fields")
        void shouldBuildSlackEnterpriseWithAllFields() {
            // When
            SlackBlockActionRequest.SlackEnterprise enterprise = SlackBlockActionRequest.SlackEnterprise.builder()
                    .id("E123")
                    .domain("Enterprise Inc")
                    .build();

            // Then
            assertThat(enterprise.getId()).isEqualTo("E123");
            assertThat(enterprise.getDomain()).isEqualTo("Enterprise Inc");
        }

        @Test
        @DisplayName("Should build SlackChannel with all fields")
        void shouldBuildSlackChannelWithAllFields() {
            // When
            SlackBlockActionRequest.SlackChannel channel = SlackBlockActionRequest.SlackChannel.builder()
                    .id("C123")
                    .name("test-channel")
                    .build();

            // Then
            assertThat(channel.getId()).isEqualTo("C123");
            assertThat(channel.getName()).isEqualTo("test-channel");
        }

        @Test
        @DisplayName("Should build SlackUser with all fields")
        void shouldBuildSlackUserWithAllFields() {
            // When
            SlackBlockActionRequest.SlackUser user = SlackBlockActionRequest.SlackUser.builder()
                    .id("U123")
                    .username("testuser")
                    .name("Test User")
                    .teamId("T123")
                    .build();

            // Then
            assertThat(user.getId()).isEqualTo("U123");
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getName()).isEqualTo("Test User");
            assertThat(user.getTeamId()).isEqualTo("T123");
        }

        @Test
        @DisplayName("Should build SlackContainer with view fields")
        void shouldBuildSlackContainerWithViewFields() {
            // When
            SlackBlockActionRequest.SlackContainer container = SlackBlockActionRequest.SlackContainer.builder()
                    .type("view")
                    .viewId("V123")
                    .viewHash("hash456")
                    .build();

            // Then
            assertThat(container.getType()).isEqualTo("view");
            assertThat(container.getViewId()).isEqualTo("V123");
            assertThat(container.getViewHash()).isEqualTo("hash456");
        }

        @Test
        @DisplayName("Should build SlackContainer with message fields")
        void shouldBuildSlackContainerWithMessageFields() {
            // When
            SlackBlockActionRequest.SlackContainer container = SlackBlockActionRequest.SlackContainer.builder()
                    .type("message")
                    .messageTs("1234567890.123456")
                    .channelId("C123")
                    .threadTs("1234567890.123456")
                    .isEphemeral(false)
                    .build();

            // Then
            assertThat(container.getType()).isEqualTo("message");
            assertThat(container.getMessageTs()).isEqualTo("1234567890.123456");
            assertThat(container.getChannelId()).isEqualTo("C123");
            assertThat(container.getThreadTs()).isEqualTo("1234567890.123456");
            assertThat(container.getIsEphemeral()).isFalse();
        }

        @Test
        @DisplayName("Should build SlackMessage with all fields")
        void shouldBuildSlackMessageWithAllFields() {
            // When
            SlackBlockActionRequest.SlackMessage message = SlackBlockActionRequest.SlackMessage.builder()
                    .type("message")
                    .subtype("bot_message")
                    .ts("1234567890.123456")
                    .botId("B123")
                    .text("Test message")
                    .blocks(java.util.List.of())
                    .build();

            // Then
            assertThat(message.getType()).isEqualTo("message");
            assertThat(message.getSubtype()).isEqualTo("bot_message");
            assertThat(message.getTs()).isEqualTo("1234567890.123456");
            assertThat(message.getBotId()).isEqualTo("B123");
            assertThat(message.getText()).isEqualTo("Test message");
            assertThat(message.getBlocks()).isNotNull();
        }
    }
}
