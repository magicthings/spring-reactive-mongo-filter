package de.magicthings.reactivemongofilter.spec;

import de.magicthings.reactivemongofilter.model.FilterableField;
import de.magicthings.reactivemongofilter.model.SortCriteria;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Map;

public interface EntityFilterSpec<T> {

    Map<String, FilterableField> getFilterableFields();

    Map<String, String> getSortableFields();

    default List<SortCriteria> getDefaultSort() {
        return List.of(new SortCriteria("created", Sort.Direction.DESC));
    }

    /**
     * Base criteria applied to every query (e.g. soft-delete filter).
     * Override to customize or return empty list to disable.
     */
    default List<Criteria> getBaseCriteria() {
        return List.of(Criteria.where("deleted").is(false));
    }
}