package de.magicthings.reactivemongofilter.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilteredPageTest {

    @Test
    void firstPageShouldBeFirst() {
        var page = new FilteredPage<>(List.of("a", "b"), 0, 10, 25, 3);

        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isTrue();
        assertThat(page.isLast()).isFalse();
    }

    @Test
    void lastPageShouldBeLast() {
        var page = new FilteredPage<>(List.of("e"), 2, 10, 25, 3);

        assertThat(page.isLast()).isTrue();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.isFirst()).isFalse();
    }

    @Test
    void middlePageShouldHavePreviousAndNext() {
        var page = new FilteredPage<>(List.of("c", "d"), 1, 10, 25, 3);

        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    void singlePageShouldBeFirstAndLast() {
        var page = new FilteredPage<>(List.of("a"), 0, 10, 1, 1);

        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void emptyPageShouldBeFirstAndLast() {
        var page = new FilteredPage<>(List.of(), 0, 10, 0, 0);

        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
    }
}