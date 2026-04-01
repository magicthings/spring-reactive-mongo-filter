package de.magicthings.reactivemongofilter.parser;

import de.magicthings.reactivemongofilter.model.FilterCriteria;
import de.magicthings.reactivemongofilter.model.FilterOperator;
import de.magicthings.reactivemongofilter.model.FilterableField;
import de.magicthings.reactivemongofilter.model.SortCriteria;
import de.magicthings.reactivemongofilter.spec.EntityFilterSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterRequestParserTest {

    private final FilterRequestParser parser = new FilterRequestParser();
    private final TestFilterSpec spec = new TestFilterSpec();

    @Nested
    class FilterParsing {

        @Test
        void shouldParseValidFilter() {
            var params = params("filter[name][eq]", "phone");

            var result = parser.parse(params, spec);

            assertThat(result.filters()).hasSize(1);
            FilterCriteria filter = result.filters().getFirst();
            assertThat(filter.field()).isEqualTo("name");
            assertThat(filter.operator()).isEqualTo(FilterOperator.EQ);
            assertThat(filter.value()).isEqualTo("phone");
        }

        @Test
        void shouldParseMultipleFilters() {
            var params = new LinkedMultiValueMap<String, String>();
            params.add("filter[name][like]", "phone");
            params.add("filter[active][eq]", "true");

            var result = parser.parse(params, spec);

            assertThat(result.filters()).hasSize(2);
        }

        @Test
        void shouldIgnoreBlankFilterValues() {
            var params = params("filter[name][eq]", "   ");

            var result = parser.parse(params, spec);

            assertThat(result.filters()).isEmpty();
        }

        @Test
        void shouldIgnoreNonFilterParams() {
            var params = params("search", "something");

            var result = parser.parse(params, spec);

            assertThat(result.filters()).isEmpty();
        }

        @Test
        void shouldRejectUnknownField() {
            var params = params("filter[unknown][eq]", "value");

            assertThatThrownBy(() -> parser.parse(params, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown filter field: 'unknown'");
        }

        @Test
        void shouldRejectDisallowedOperator() {
            var params = params("filter[active][like]", "true");

            assertThatThrownBy(() -> parser.parse(params, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Operator 'like' is not allowed for field 'active'");
        }

        @Test
        void shouldRejectUnknownOperator() {
            var params = params("filter[name][between]", "a");

            assertThatThrownBy(() -> parser.parse(params, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown filter operator");
        }
    }

    @Nested
    class SortParsing {

        @Test
        void shouldParseSortWithDirection() {
            var params = params("sort", "name,desc");

            var result = parser.parse(params, spec);

            assertThat(result.sorts()).hasSize(1);
            SortCriteria sort = result.sorts().getFirst();
            assertThat(sort.field()).isEqualTo("name");
            assertThat(sort.direction()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        void shouldDefaultToAscDirection() {
            var params = params("sort", "name");

            var result = parser.parse(params, spec);

            assertThat(result.sorts().getFirst().direction()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        void shouldParseExplicitAsc() {
            var params = params("sort", "name,asc");

            var result = parser.parse(params, spec);

            assertThat(result.sorts().getFirst().direction()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        void shouldParseMultipleSorts() {
            var params = new LinkedMultiValueMap<String, String>();
            params.add("sort", "name,asc");
            params.add("sort", "price,desc");

            var result = parser.parse(params, spec);

            assertThat(result.sorts()).hasSize(2);
        }

        @Test
        void shouldRejectUnknownSortField() {
            var params = params("sort", "unknown,asc");

            assertThatThrownBy(() -> parser.parse(params, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown sort field: 'unknown'");
        }

        @Test
        void shouldRejectInvalidSortDirection() {
            var params = params("sort", "name,upward");

            assertThatThrownBy(() -> parser.parse(params, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid sort direction: 'upward'");
        }

        @Test
        void shouldReturnEmptySortsWhenNoSortParam() {
            var params = new LinkedMultiValueMap<String, String>();

            var result = parser.parse(params, spec);

            assertThat(result.sorts()).isEmpty();
        }
    }

    @Nested
    class Pagination {

        @Test
        void shouldUseDefaultPageAndSize() {
            var params = new LinkedMultiValueMap<String, String>();

            var result = parser.parse(params, spec);

            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(20);
        }

        @Test
        void shouldParseCustomPageAndSize() {
            var params = new LinkedMultiValueMap<String, String>();
            params.add("page", "2");
            params.add("size", "50");

            var result = parser.parse(params, spec);

            assertThat(result.page()).isEqualTo(2);
            assertThat(result.size()).isEqualTo(50);
        }

        @Test
        void shouldCapSizeAtMax100() {
            var params = params("size", "200");

            var result = parser.parse(params, spec);

            assertThat(result.size()).isEqualTo(100);
        }

        @Test
        void shouldRejectNonNumericPage() {
            var params = params("page", "abc");

            assertThatThrownBy(() -> parser.parse(params, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for 'page'");
        }

        @Test
        void shouldRejectNonNumericSize() {
            var params = params("size", "xyz");

            assertThatThrownBy(() -> parser.parse(params, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for 'size'");
        }
    }

    private MultiValueMap<String, String> params(String key, String value) {
        var map = new LinkedMultiValueMap<String, String>();
        map.add(key, value);
        return map;
    }

    static class TestFilterSpec implements EntityFilterSpec<Object> {

        @Override
        public Map<String, FilterableField> getFilterableFields() {
            return Map.of(
                    "name", FilterableField.string("name"),
                    "active", FilterableField.bool("active"),
                    "price", FilterableField.integer("price"),
                    "created", FilterableField.dateTime("created")
            );
        }

        @Override
        public Map<String, String> getSortableFields() {
            return Map.of(
                    "name", "name",
                    "price", "price",
                    "created", "createdAt"
            );
        }
    }
}