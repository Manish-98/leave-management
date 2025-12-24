package one.june.leave_management.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Slack integration
 * These properties are loaded from application.properties with prefix "slack"
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "slack")
public class SlackProperties {

    /**
     * Slack signing secret for verifying request signatures
     * This is used to verify that requests actually come from Slack
     */
    private String signingSecret;

    /**
     * Slack bot token for API calls
     * This is used to make API calls to Slack (e.g., opening modals)
     */
    private String botToken;

    /**
     * Whether Slack integration is enabled
     * Allows disabling Slack integration without removing code
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Slack API base URL
     * Can be overridden for testing or custom Slack installations
     */
    @Builder.Default
    private String apiBaseUrl = "https://slack.com/api";

    /**
     * Slack views.open API endpoint path
     * Can be overridden for testing or different API versions
     */
    @Builder.Default
    private String viewsOpenEndpoint = "/views.open";
}
