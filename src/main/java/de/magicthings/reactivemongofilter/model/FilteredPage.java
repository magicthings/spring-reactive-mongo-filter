package de.magicthings.reactivemongofilter.model;

import java.util.List;

public record FilteredPage<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public boolean isFirst() {
        return pageNumber == 0;
    }

    public boolean isLast() {
        return (pageNumber + 1) >= totalPages;
    }

    public boolean hasNext() {
        return !isLast();
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }
}