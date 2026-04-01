package de.magicthings.reactivemongofilter.repository;

import de.magicthings.reactivemongofilter.model.FilterRequest;
import de.magicthings.reactivemongofilter.model.FilterableField;
import de.magicthings.reactivemongofilter.model.FilteredPage;
import de.magicthings.reactivemongofilter.parser.FilterCriteriaBuilder;
import de.magicthings.reactivemongofilter.spec.EntityFilterSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveFilterRepositoryTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    private ReactiveFilterRepository repository;
    private final FilterCriteriaBuilder criteriaBuilder = new FilterCriteriaBuilder();
    private final TestSpec spec = new TestSpec();

    @BeforeEach
    void setUp() {
        repository = new ReactiveFilterRepository(mongoTemplate, criteriaBuilder);
    }

    @Test
    void shouldReturnFilteredPageWithResults() {
        var request = new FilterRequest(List.of(), List.of(), 0, 10);

        when(mongoTemplate.find(any(Query.class), eq(TestEntity.class)))
                .thenReturn(Flux.just(new TestEntity("a"), new TestEntity("b")));
        when(mongoTemplate.count(any(Query.class), eq(TestEntity.class)))
                .thenReturn(Mono.just(25L));

        StepVerifier.create(repository.findFiltered(request, spec, TestEntity.class))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.pageNumber()).isZero();
                    assertThat(page.pageSize()).isEqualTo(10);
                    assertThat(page.totalElements()).isEqualTo(25);
                    assertThat(page.totalPages()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyPage() {
        var request = new FilterRequest(List.of(), List.of(), 0, 10);

        when(mongoTemplate.find(any(Query.class), eq(TestEntity.class)))
                .thenReturn(Flux.empty());
        when(mongoTemplate.count(any(Query.class), eq(TestEntity.class)))
                .thenReturn(Mono.just(0L));

        StepVerifier.create(repository.findFiltered(request, spec, TestEntity.class))
                .assertNext(page -> {
                    assertThat(page.content()).isEmpty();
                    assertThat(page.totalElements()).isZero();
                    assertThat(page.totalPages()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void shouldCalculateTotalPagesCorrectly() {
        var request = new FilterRequest(List.of(), List.of(), 0, 7);

        when(mongoTemplate.find(any(Query.class), eq(TestEntity.class)))
                .thenReturn(Flux.just(new TestEntity("a")));
        when(mongoTemplate.count(any(Query.class), eq(TestEntity.class)))
                .thenReturn(Mono.just(20L));

        StepVerifier.create(repository.findFiltered(request, spec, TestEntity.class))
                .assertNext(page -> assertThat(page.totalPages()).isEqualTo(3)) // ceil(20/7) = 3
                .verifyComplete();
    }

    record TestEntity(String name) {}

    static class TestSpec implements EntityFilterSpec<TestEntity> {
        @Override
        public Map<String, FilterableField> getFilterableFields() {
            return Map.of("name", FilterableField.string("name"));
        }

        @Override
        public Map<String, String> getSortableFields() {
            return Map.of("name", "name");
        }
    }
}