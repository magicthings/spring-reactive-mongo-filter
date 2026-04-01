package de.magicthings.reactivemongofilter.model;

public enum FilterOperator {

    EQ,
    NEQ,
    LIKE,
    GT,
    LT,
    GTE,
    LTE,
    IN;

    public static FilterOperator fromString(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown filter operator: '" + value
                    + "'. Supported operators: eq, neq, like, gt, lt, gte, lte, in");
        }
    }
}