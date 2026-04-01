package de.magicthings.reactivemongofilter.parser;

import de.magicthings.reactivemongofilter.model.FilterCriteria;
import de.magicthings.reactivemongofilter.model.FilterOperator;
import de.magicthings.reactivemongofilter.model.FilterRequest;
import de.magicthings.reactivemongofilter.model.FilterableField;
import de.magicthings.reactivemongofilter.model.SortCriteria;
import de.magicthings.reactivemongofilter.spec.EntityFilterSpec;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterCriteriaBuilderTest {

    private final FilterCriteriaBuilder builder = new FilterCriteriaBuilder();

    @Test
    void buildQueryShouldApplyBaseCriteria() {
        var request = emptyRequest();

        Query query = builder.buildQuery(request, spec());

        String queryString = query.toString();
        assertThat(queryString).contains("deleted");
    }

    @Test
    void buildQueryShouldApplyEqFilter() {
        var request = requestWithFilter("name", FilterOperator.EQ, "phone");

        Query query = builder.buildQuery(request, spec());

        String queryString = query.toString();
        assertThat(queryString).contains("name");
    }

    @Test
    void buildQueryShouldApplyLikeFilterWithRegex() {
        var request = requestWithFilter("name", FilterOperator.LIKE, "pho.ne");

        Query query = builder.buildQuery(request, spec());

        String queryString = query.toString();
        // Pattern.quote wraps in \Q...\E, toString double-escapes backslashes
        assertThat(queryString).contains("\\\\Qpho.ne\\\\E");
    }

    @Test
    void buildQueryShouldApplyInFilter() {
        var request = requestWithFilter("name", FilterOperator.IN, "a,b,c");

        Query query = builder.buildQuery(request, spec());

        String queryString = query.toString();
        assertThat(queryString).contains("name");
    }

    @Test
    void buildQueryShouldApplyPagination() {
        var request = new FilterRequest(List.of(), List.of(), 2, 15);

        Query query = builder.buildQuery(request, spec());

        assertThat(query.getSkip()).isEqualTo(30);
        assertThat(query.getLimit()).isEqualTo(15);
    }

    @Test
    void buildQueryShouldApplyExplicitSort() {
        var sorts = List.of(new SortCriteria("name", Sort.Direction.DESC));
        var request = new FilterRequest(List.of(), sorts, 0, 10);

        Query query = builder.buildQuery(request, spec());

        assertThat(query.getSortObject().toJson()).contains("name");
    }

    @Test
    void buildQueryShouldApplyDefaultSortWhenNoneProvided() {
        var request = emptyRequest();

        Query query = builder.buildQuery(request, spec());

        assertThat(query.getSortObject().toJson()).contains("createdAt");
    }

    @Test
    void buildQueryShouldApplyAdditionalCriteria() {
        var request = emptyRequest();
        Criteria tenantCriteria = Criteria.where("tenantId").is("tenant-1");

        Query query = builder.buildQuery(request, spec(), tenantCriteria);

        assertThat(query.toString()).contains("tenantId");
    }

    @Test
    void buildCountQueryShouldNotIncludePagination() {
        var request = new FilterRequest(List.of(), List.of(), 5, 25);

        Query countQuery = builder.buildCountQuery(request, spec());

        assertThat(countQuery.getSkip()).isZero();
        assertThat(countQuery.getLimit()).isZero();
    }

    @Test
    void shouldConvertBooleanValues() {
        var request = requestWithFilter("active", FilterOperator.EQ, "true");

        Query query = builder.buildQuery(request, specWithBool());

        assertThat(query.toString()).contains("active");
    }

    @Test
    void shouldRejectInvalidBooleanValue() {
        var request = requestWithFilter("active", FilterOperator.EQ, "maybe");

        assertThatThrownBy(() -> builder.buildQuery(request, specWithBool()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid boolean value");
    }

    @Test
    void shouldConvertIntegerValues() {
        var request = requestWithFilter("price", FilterOperator.GT, "100");

        Query query = builder.buildQuery(request, specWithInteger());

        assertThat(query.toString()).contains("price");
    }

    @Test
    void shouldRejectInvalidIntegerValue() {
        var request = requestWithFilter("price", FilterOperator.EQ, "notanumber");

        assertThatThrownBy(() -> builder.buildQuery(request, specWithInteger()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldConvertDateTimeValues() {
        var request = requestWithFilter("created", FilterOperator.GTE, "2024-01-01T00:00:00");

        Query query = builder.buildQuery(request, specWithDateTime());

        assertThat(query.toString()).contains("created");
    }

    @Test
    void shouldConvertLongValues() {
        var request = requestWithFilter("count", FilterOperator.EQ, "9999999999");

        Query query = builder.buildQuery(request, specWithLong());

        assertThat(query.toString()).contains("count");
    }

    // --- helpers ---

    private FilterRequest emptyRequest() {
        return new FilterRequest(List.of(), List.of(), 0, 20);
    }

    private FilterRequest requestWithFilter(String field, FilterOperator op, String value) {
        return new FilterRequest(List.of(new FilterCriteria(field, op, value)), List.of(), 0, 20);
    }

    private EntityFilterSpec<Object> spec() {
        return new EntityFilterSpec<>() {
            @Override
            public Map<String, FilterableField> getFilterableFields() {
                return Map.of("name", FilterableField.string("name"));
            }

            @Override
            public Map<String, String> getSortableFields() {
                return Map.of("name", "name", "created", "createdAt");
            }
        };
    }

    private EntityFilterSpec<Object> specWithBool() {
        return new EntityFilterSpec<>() {
            @Override
            public Map<String, FilterableField> getFilterableFields() {
                return Map.of("active", FilterableField.bool("active"));
            }

            @Override
            public Map<String, String> getSortableFields() {
                return Map.of();
            }
        };
    }

    private EntityFilterSpec<Object> specWithInteger() {
        return new EntityFilterSpec<>() {
            @Override
            public Map<String, FilterableField> getFilterableFields() {
                return Map.of("price", FilterableField.integer("price"));
            }

            @Override
            public Map<String, String> getSortableFields() {
                return Map.of();
            }
        };
    }

    private EntityFilterSpec<Object> specWithDateTime() {
        return new EntityFilterSpec<>() {
            @Override
            public Map<String, FilterableField> getFilterableFields() {
                return Map.of("created", FilterableField.dateTime("created"));
            }

            @Override
            public Map<String, String> getSortableFields() {
                return Map.of();
            }
        };
    }

    private EntityFilterSpec<Object> specWithLong() {
        return new EntityFilterSpec<>() {
            @Override
            public Map<String, FilterableField> getFilterableFields() {
                return Map.of("count", FilterableField.longType("count"));
            }

            @Override
            public Map<String, String> getSortableFields() {
                return Map.of();
            }
        };
    }
}