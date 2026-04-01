package de.magicthings.reactivemongofilter.model;

import org.springframework.data.domain.Sort;

public record SortCriteria(String field, Sort.Direction direction) {
}