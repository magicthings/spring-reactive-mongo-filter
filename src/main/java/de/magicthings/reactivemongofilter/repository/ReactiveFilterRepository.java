package de.magicthings.reactivemongofilter.repository;

import de.magicthings.reactivemongofilter.model.FilterRequest;
import de.magicthings.reactivemongofilter.model.FilteredPage;
import de.magicthings.reactivemongofilter.parser.FilterCriteriaBuilder;
import de.magicthings.reactivemongofilter.spec.EntityFilterSpec;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;

import java.util.List;

public class ReactiveFilterRepository {

    private final ReactiveMongoTemplate mongoTemplate;
    private final FilterCriteriaBuilder criteriaBuilder;

    public ReactiveFilterRepository(ReactiveMongoTemplate mongoTemplate, FilterCriteriaBuilder criteriaBuilder) {
        this.mongoTemplate = mongoTemplate;
        this.criteriaBuilder = criteriaBuilder;
    }

    public <T> Mono<FilteredPage<T>> findFiltered(
            FilterRequest filterRequest,
            EntityFilterSpec<T> spec,
            Class<T> entityClass,
            Criteria... additional
    ) {
        Query dataQuery = criteriaBuilder.buildQuery(filterRequest, spec, additional);
        Query countQuery = criteriaBuilder.buildCountQuery(filterRequest, spec, additional);

        Mono<List<T>> dataMono = mongoTemplate.find(dataQuery, entityClass).collectList();
        Mono<Long> countMono = mongoTemplate.count(countQuery, entityClass);

        return dataMono.zipWith(countMono)
                .map(tuple -> {
                    List<T> content = tuple.getT1();
                    long totalElements = tuple.getT2();
                    int totalPages = filterRequest.size() > 0
                            ? (int) Math.ceil((double) totalElements / filterRequest.size())
                            : 0;
                    return new FilteredPage<>(content, filterRequest.page(), filterRequest.size(), totalElements, totalPages);
                });
    }
}