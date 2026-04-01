package de.magicthings.reactivemongofilter.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterRequestTest {

    @Test
    void shouldCreateValidFilterRequest() {
        var filters = List.of(new FilterCriteria("name", FilterOperator.EQ, "test"));
        var sorts = List.<SortCriteria>of();

        var request = new FilterRequest(filters, sorts, 0, 20);

        assertThat(request.filters()).hasSize(1);
        assertThat(request.sorts()).isEmpty();
        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    void shouldMakeListsImmutable() {
        var filters = new ArrayList<>(List.of(new FilterCriteria("name", FilterOperator.EQ, "test")));
        var sorts = new ArrayList<SortCriteria>();

        var request = new FilterRequest(filters, sorts, 0, 10);

        assertThatThrownBy(() -> request.filters().add(new FilterCriteria("x", FilterOperator.EQ, "y")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.sorts().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectNegativePage() {
        assertThatThrownBy(() -> new FilterRequest(List.of(), List.of(), -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page must not be negative");
    }

    @Test
    void shouldRejectZeroSize() {
        assertThatThrownBy(() -> new FilterRequest(List.of(), List.of(), 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Size must be at least 1");
    }
}