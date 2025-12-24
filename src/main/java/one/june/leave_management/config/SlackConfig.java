package one.june.leave_management.config;

import one.june.leave_management.adapter.inbound.slack.util.SlackRequestSignatureVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Slack integration
 * Creates necessary beans for Slack functionality
 */
@Configuration
@ConditionalOnProperty(name = "slack.enabled", havingValue = "true", matchIfMissing = true)
public class SlackConfig {

    /**
     * Creates a SlackRequestSignatureVerifier bean
     * This bean is used to verify that incoming requests actually come from Slack
     *
     * @param slackProperties The Slack configuration properties
     * @return A configured SlackRequestSignatureVerifier
     */
    @Bean
    public SlackRequestSignatureVerifier slackRequestSignatureVerifier(SlackProperties slackProperties) {
        return new SlackRequestSignatureVerifier(slackProperties.getSigningSecret());
    }

    /**
     * Creates a RestTemplate bean for making HTTP requests
     * <p>
     * RestTemplate is Spring's standard HTTP client for synchronous requests.
     * It's included in spring-web and doesn't require additional dependencies.
     *
     * @return A configured RestTemplate
     */
    @Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}
