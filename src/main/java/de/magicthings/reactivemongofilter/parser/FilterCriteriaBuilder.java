package de.magicthings.reactivemongofilter.parser;

import de.magicthings.reactivemongofilter.model.FilterCriteria;
import de.magicthings.reactivemongofilter.model.FilterOperator;
import de.magicthings.reactivemongofilter.model.FilterRequest;
import de.magicthings.reactivemongofilter.model.FilterableField;
import de.magicthings.reactivemongofilter.model.SortCriteria;
import de.magicthings.reactivemongofilter.spec.EntityFilterSpec;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FilterCriteriaBuilder {

    public <T> Query buildQuery(FilterRequest filterRequest, EntityFilterSpec<T> spec, Criteria... additional) {
        Query query = new Query();

        List<Criteria> criteriaList = buildCriteriaList(filterRequest, spec, additional);
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        applySorting(query, filterRequest, spec);
        query.with(PageRequest.of(filterRequest.page(), filterRequest.size()));

        return query;
    }

    public <T> Query buildCountQuery(FilterRequest filterRequest, EntityFilterSpec<T> spec, Criteria... additional) {
        Query query = new Query();

        List<Criteria> criteriaList = buildCriteriaList(filterRequest, spec, additional);
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return query;
    }

    private <T> List<Criteria> buildCriteriaList(FilterRequest filterRequest, EntityFilterSpec<T> spec, Criteria... additional) {
        List<Criteria> criteriaList = new ArrayList<>(spec.getBaseCriteria());

        Map<String, FilterableField> filterableFields = spec.getFilterableFields();

        for (FilterCriteria fc : filterRequest.filters()) {
            FilterableField field = filterableFields.get(fc.field());
            String mongoField = field.documentField();
            Object typedValue = convertValue(fc.value(), field.fieldType(), fc.operator());
            criteriaList.add(toCriteria(mongoField, fc.operator(), typedValue));
        }

        criteriaList.addAll(Arrays.asList(additional));

        return criteriaList;
    }

    private <T> void applySorting(Query query, FilterRequest filterRequest, EntityFilterSpec<T> spec) {
        List<SortCriteria> sorts = filterRequest.sorts().isEmpty()
                ? spec.getDefaultSort()
                : filterRequest.sorts();

        if (!sorts.isEmpty()) {
            Map<String, String> sortableFields = spec.getSortableFields();
            List<Sort.Order> orders = sorts.stream()
                    .filter(sc -> sortableFields.containsKey(sc.field()))
                    .map(sc -> new Sort.Order(sc.direction(), sortableFields.get(sc.field())))
                    .toList();
            if (!orders.isEmpty()) {
                query.with(Sort.by(orders));
            }
        }
    }

    private Criteria toCriteria(String field, FilterOperator operator, Object value) {
        return switch (operator) {
            case EQ -> Criteria.where(field).is(value);
            case NEQ -> Criteria.where(field).ne(value);
            case LIKE -> Criteria.where(field).regex((String) value, "i");
            case GT -> Criteria.where(field).gt(value);
            case LT -> Criteria.where(field).lt(value);
            case GTE -> Criteria.where(field).gte(value);
            case LTE -> Criteria.where(field).lte(value);
            case IN -> Criteria.where(field).in((Collection<?>) value);
        };
    }

    private Object convertValue(String raw, Class<?> targetType, FilterOperator operator) {
        if (operator == FilterOperator.IN) {
            return Arrays.stream(raw.split(","))
                    .map(v -> convertSingle(v.trim(), targetType))
                    .toList();
        }
        if (operator == FilterOperator.LIKE) {
            return Pattern.quote(raw);
        }
        return convertSingle(raw, targetType);
    }

    private Object convertSingle(String raw, Class<?> targetType) {
        try {
            if (targetType == String.class) return raw;
            if (targetType == Boolean.class) return parseBoolean(raw);
            if (targetType == Integer.class) return Integer.parseInt(raw);
            if (targetType == Long.class) return Long.parseLong(raw);
            if (targetType == LocalDateTime.class) return LocalDateTime.parse(raw);
            throw new IllegalArgumentException("Unsupported filter value type: " + targetType.getSimpleName());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value '" + raw + "' for type " + targetType.getSimpleName(), e);
        }
    }

    private boolean parseBoolean(String raw) {
        if ("true".equalsIgnoreCase(raw)) return true;
        if ("false".equalsIgnoreCase(raw)) return false;
        throw new IllegalArgumentException("Invalid boolean value: '" + raw + "'. Use 'true' or 'false'");
    }
}