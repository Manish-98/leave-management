package one.june.leave_management.adapter.inbound.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI documentation.
 * Configures the Swagger UI and API documentation for the Leave Management application.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${springdoc.api-docs.path:/api-docs}")
    private String apiDocsPath;

    /**
     * Configures the main OpenAPI information.
     * This includes API title, version, description, license, and contact information.
     *
     * @return OpenAPI configuration with metadata
     */
    @Bean
    public OpenAPI leaveManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leave Management API")
                        .version("1.0.0")
                        .description(
                                "REST API for managing leave requests in the Leave Management System. " +
                                "This API provides endpoints to create, query, and manage leave requests " +
                                "with support for different leave types, durations, and statuses."
                        )
                        .contact(new Contact()
                                .name("Leave Management Team")
                                .email("support@leave-management.com")
                                .url("https://leave-management.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("https://api.leave-management.com")
                                .description("Production server")
                ));
    }

    /**
     * Configures the API group for Leave Management endpoints.
     * This groups all REST API endpoints under the "Leave Management" tag in Swagger UI.
     * Excludes Slack integration endpoints from the main API documentation.
     *
     * @return GroupedOpenApi configuration for leave management endpoints
     */
    @Bean
    public GroupedOpenApi leaveManagementApi() {
        return GroupedOpenApi.builder()
                .group("leave-management")
                .pathsToMatch("/api/**")
                .pathsToExclude("/integrations/**")
                .packagesToScan("one.june.leave_management.adapter.inbound.web")
                .build();
    }
}
