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
 * Configuration properties for leave management
 * These properties are loaded from application.properties with prefix "leave"
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "leave")
public class LeaveProperties {

    /**
     * Maximum number of optional holidays allowed per user per year
     */
    @Builder.Default
    private int maxOptionalHolidaysPerYear = 2;
}
