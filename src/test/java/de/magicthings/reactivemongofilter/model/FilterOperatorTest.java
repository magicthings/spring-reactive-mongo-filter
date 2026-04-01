package de.magicthings.reactivemongofilter.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterOperatorTest {

    @ParameterizedTest
    @CsvSource({
            "eq,   EQ",
            "EQ,   EQ",
            "neq,  NEQ",
            "like, LIKE",
            "gt,   GT",
            "lt,   LT",
            "gte,  GTE",
            "lte,  LTE",
            "in,   IN",
            "Gte,  GTE"
    })
    void fromStringShouldParseCaseInsensitively(String input, FilterOperator expected) {
        assertThat(FilterOperator.fromString(input)).isEqualTo(expected);
    }

    @Test
    void fromStringShouldThrowForUnknownOperator() {
        assertThatThrownBy(() -> FilterOperator.fromString("between"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown filter operator: 'between'");
    }
}