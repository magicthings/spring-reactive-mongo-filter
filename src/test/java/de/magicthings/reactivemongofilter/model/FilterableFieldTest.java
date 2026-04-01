package de.magicthings.reactivemongofilter.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FilterableFieldTest {

    @Test
    void stringFactoryShouldUseNameAsDocumentField() {
        var field = FilterableField.string("status");

        assertThat(field.paramName()).isEqualTo("status");
        assertThat(field.documentField()).isEqualTo("status");
        assertThat(field.fieldType()).isEqualTo(String.class);
        assertThat(field.allowedOperators()).containsExactlyInAnyOrder(
                FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.LIKE, FilterOperator.IN);
    }

    @Test
    void stringFactoryWithMappedDocumentField() {
        var field = FilterableField.string("name", "displayName");

        assertThat(field.paramName()).isEqualTo("name");
        assertThat(field.documentField()).isEqualTo("displayName");
    }

    @Test
    void boolFactoryShouldOnlyAllowEq() {
        var field = FilterableField.bool("active");

        assertThat(field.fieldType()).isEqualTo(Boolean.class);
        assertThat(field.allowedOperators()).containsExactly(FilterOperator.EQ);
    }

    @Test
    void boolFactoryWithMappedDocumentField() {
        var field = FilterableField.bool("active", "isActive");

        assertThat(field.paramName()).isEqualTo("active");
        assertThat(field.documentField()).isEqualTo("isActive");
    }

    @Test
    void dateTimeFactoryShouldAllowComparisonOperators() {
        var field = FilterableField.dateTime("created");

        assertThat(field.fieldType()).isEqualTo(LocalDateTime.class);
        assertThat(field.allowedOperators()).containsExactlyInAnyOrder(
                FilterOperator.EQ, FilterOperator.GT, FilterOperator.LT,
                FilterOperator.GTE, FilterOperator.LTE);
    }

    @Test
    void dateTimeFactoryWithMappedDocumentField() {
        var field = FilterableField.dateTime("created", "createdAt");

        assertThat(field.documentField()).isEqualTo("createdAt");
    }

    @Test
    void integerFactoryShouldAllowNumericOperators() {
        var field = FilterableField.integer("price");

        assertThat(field.fieldType()).isEqualTo(Integer.class);
        assertThat(field.allowedOperators()).containsExactlyInAnyOrder(
                FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.GT,
                FilterOperator.LT, FilterOperator.GTE, FilterOperator.LTE, FilterOperator.IN);
    }

    @Test
    void longTypeFactoryShouldAllowNumericOperators() {
        var field = FilterableField.longType("count");

        assertThat(field.fieldType()).isEqualTo(Long.class);
        assertThat(field.allowedOperators()).containsExactlyInAnyOrder(
                FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.GT,
                FilterOperator.LT, FilterOperator.GTE, FilterOperator.LTE, FilterOperator.IN);
    }
}