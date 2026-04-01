package de.magicthings.reactivemongofilter.model;

import java.util.List;

public record FilterRequest(
        List<FilterCriteria> filters,
        List<SortCriteria> sorts,
        int page,
        int size
) {

    public FilterRequest {
        filters = List.copyOf(filters);
        sorts = List.copyOf(sorts);
        if (page < 0) throw new IllegalArgumentException("Page must not be negative");
        if (size < 1) throw new IllegalArgumentException("Size must be at least 1");
    }
}