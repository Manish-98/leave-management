package one.june.leave_management.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LeaveProperties.class)
@EnableConfigurationProperties(LeaveProperties.class)
@TestPropertySource(properties = {
        "leave.max-optional-holidays-per-year=5"
})
@DisplayName("LeaveProperties - Configuration Properties Tests")
class LeavePropertiesTest {

    private LeaveProperties leaveProperties;

    @BeforeEach
    void setUp() {
        leaveProperties = new LeaveProperties();
    }

    @Test
    @DisplayName("Should use builder to create LeaveProperties with custom values")
    void shouldUseBuilderToCreateWithCustomValues() {
        // Given
        int expectedMax = 5;

        // When
        LeaveProperties properties = LeaveProperties.builder()
                .maxOptionalHolidaysPerYear(expectedMax)
                .build();

        // Then
        assertThat(properties.getMaxOptionalHolidaysPerYear()).isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("Should use default value when not specified")
    void shouldUseDefaultValueWhenNotSpecified() {
        // When
        LeaveProperties properties = LeaveProperties.builder()
                .build();

        // Then
        assertThat(properties.getMaxOptionalHolidaysPerYear()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should allow setting maxOptionalHolidaysPerYear")
    void shouldAllowSettingMaxOptionalHolidaysPerYear() {
        // Given
        LeaveProperties properties = LeaveProperties.builder()
                .build();
        int expectedMax = 10;

        // When
        properties.setMaxOptionalHolidaysPerYear(expectedMax);

        // Then
        assertThat(properties.getMaxOptionalHolidaysPerYear()).isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("Should create LeaveProperties with all args constructor")
    void shouldCreateWithAllArgsConstructor() {
        // Given
        int expectedMax = 3;

        // When
        LeaveProperties properties = new LeaveProperties(expectedMax);

        // Then
        assertThat(properties.getMaxOptionalHolidaysPerYear()).isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("Should support no-args constructor with builder pattern")
    void shouldSupportNoArgsConstructorWithBuilder() {
        // When
        LeaveProperties properties = new LeaveProperties();
        properties.setMaxOptionalHolidaysPerYear(7);

        // Then
        assertThat(properties.getMaxOptionalHolidaysPerYear()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should handle zero max optional holidays")
    void shouldHandleZeroMaxOptionalHolidays() {
        // When
        LeaveProperties properties = LeaveProperties.builder()
                .maxOptionalHolidaysPerYear(0)
                .build();

        // Then
        assertThat(properties.getMaxOptionalHolidaysPerYear()).isZero();
    }

    @Test
    @DisplayName("Should handle large max optional holidays")
    void shouldHandleLargeMaxOptionalHolidays() {
        // Given
        int expectedMax = 100;

        // When
        LeaveProperties properties = LeaveProperties.builder()
                .maxOptionalHolidaysPerYear(expectedMax)
                .build();

        // Then
        assertThat(properties.getMaxOptionalHolidaysPerYear()).isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("Should generate proper toString")
    void shouldGenerateProperToString() {
        // Given
        LeaveProperties properties = LeaveProperties.builder()
                .maxOptionalHolidaysPerYear(3)
                .build();

        // When
        String toString = properties.toString();

        // Then
        assertThat(toString)
                .contains("LeaveProperties")
                .contains("maxOptionalHolidaysPerYear=3");
    }
}
