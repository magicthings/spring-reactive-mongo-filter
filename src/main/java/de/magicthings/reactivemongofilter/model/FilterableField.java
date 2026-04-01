package de.magicthings.reactivemongofilter.model;

import java.time.LocalDateTime;
import java.util.Set;

public record FilterableField(
        String paramName,
        String documentField,
        Class<?> fieldType,
        Set<FilterOperator> allowedOperators
) {

    public static FilterableField string(String name) {
        return new FilterableField(name, name, String.class,
                Set.of(FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.LIKE, FilterOperator.IN));
    }

    public static FilterableField string(String paramName, String documentField) {
        return new FilterableField(paramName, documentField, String.class,
                Set.of(FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.LIKE, FilterOperator.IN));
    }

    public static FilterableField bool(String name) {
        return new FilterableField(name, name, Boolean.class,
                Set.of(FilterOperator.EQ));
    }

    public static FilterableField bool(String paramName, String documentField) {
        return new FilterableField(paramName, documentField, Boolean.class,
                Set.of(FilterOperator.EQ));
    }

    public static FilterableField dateTime(String name) {
        return new FilterableField(name, name, LocalDateTime.class,
                Set.of(FilterOperator.EQ, FilterOperator.GT, FilterOperator.LT, FilterOperator.GTE, FilterOperator.LTE));
    }

    public static FilterableField dateTime(String paramName, String documentField) {
        return new FilterableField(paramName, documentField, LocalDateTime.class,
                Set.of(FilterOperator.EQ, FilterOperator.GT, FilterOperator.LT, FilterOperator.GTE, FilterOperator.LTE));
    }

    public static FilterableField integer(String name) {
        return new FilterableField(name, name, Integer.class,
                Set.of(FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.GT, FilterOperator.LT, FilterOperator.GTE, FilterOperator.LTE, FilterOperator.IN));
    }

    public static FilterableField longType(String name) {
        return new FilterableField(name, name, Long.class,
                Set.of(FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.GT, FilterOperator.LT, FilterOperator.GTE, FilterOperator.LTE, FilterOperator.IN));
    }
}