package one.june.leave_management.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import one.june.leave_management.common.model.DateRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DateRangeValidator}
 * <p>
 * Tests the validator logic directly without using the Jakarta validation framework.
 * Each test covers a unique execution path or scenario.
 * <p>
 * Note: Integration tests in {@link one.june.leave_management.common.model.DateRangeTest}
 * verify the framework integration, while these tests verify the validator logic itself.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DateRangeValidator Unit Tests")
class DateRangeValidatorTest {

    private DateRangeValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new DateRangeValidator();
    }

    @Test
    @DisplayName("Should return true when endDate is after startDate")
    void shouldReturnTrueWhenEndDateIsAfterStartDate() {
        // Given
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 5))
                .build();

        // When
        boolean result = validator.isValid(dateRange, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when start and end dates are equal")
    void shouldReturnTrueWhenStartAndEndDatesAreEqual() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        DateRange dateRange = DateRange.builder()
                .startDate(date)
                .endDate(date)
                .build();

        // When
        boolean result = validator.isValid(dateRange, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when endDate is before startDate")
    void shouldReturnFalseWhenEndDateIsBeforeStartDate() {
        // Given
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 5))
                .endDate(LocalDate.of(2024, 1, 1))
                .build();

        // When
        boolean result = validator.isValid(dateRange, context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when DateRange object is null (delegates to @NotNull)")
    void shouldReturnTrueWhenDateRangeObjectIsNull() {
        // Given
        DateRange dateRange = null;

        // When
        boolean result = validator.isValid(dateRange, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when startDate is null (delegates to @NotNull)")
    void shouldReturnTrueWhenStartDateIsNull() {
        // Given
        DateRange dateRange = DateRange.builder()
                .startDate(null)
                .endDate(LocalDate.of(2024, 1, 5))
                .build();

        // When
        boolean result = validator.isValid(dateRange, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when endDate is null (delegates to @NotNull)")
    void shouldReturnTrueWhenEndDateIsNull() {
        // Given
        DateRange dateRange = DateRange.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(null)
                .build();

        // When
        boolean result = validator.isValid(dateRange, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should be instantiable without Jakarta validation framework")
    void shouldBeInstantiableWithoutJakartaValidationFramework() {
        // Given & When
        DateRangeValidator directValidator = new DateRangeValidator();

        // Then
        assertThat(directValidator).isNotNull();
    }
}
