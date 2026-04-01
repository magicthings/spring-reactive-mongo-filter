package de.magicthings.reactivemongofilter.model;

public record FilterCriteria(String field, FilterOperator operator, String value) {
}