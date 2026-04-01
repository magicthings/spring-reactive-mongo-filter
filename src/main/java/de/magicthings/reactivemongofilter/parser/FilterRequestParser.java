package de.magicthings.reactivemongofilter.parser;

import de.magicthings.reactivemongofilter.model.FilterCriteria;
import de.magicthings.reactivemongofilter.model.FilterOperator;
import de.magicthings.reactivemongofilter.model.FilterRequest;
import de.magicthings.reactivemongofilter.model.FilterableField;
import de.magicthings.reactivemongofilter.model.SortCriteria;
import de.magicthings.reactivemongofilter.spec.EntityFilterSpec;
import org.springframework.data.domain.Sort;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterRequestParser {

    private static final Pattern FILTER_PATTERN = Pattern.compile("^filter\\[(\\w+)]\\[(\\w+)]$");
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public <T> FilterRequest parse(MultiValueMap<String, String> queryParams, EntityFilterSpec<T> spec) {
        List<FilterCriteria> filters = parseFilters(queryParams, spec);
        List<SortCriteria> sorts = parseSorts(queryParams, spec);
        int page = parseIntParam(queryParams, "page", DEFAULT_PAGE);
        int size = Math.min(parseIntParam(queryParams, "size", DEFAULT_SIZE), MAX_SIZE);

        return new FilterRequest(filters, sorts, page, size);
    }

    private <T> List<FilterCriteria> parseFilters(MultiValueMap<String, String> queryParams, EntityFilterSpec<T> spec) {
        List<FilterCriteria> filters = new ArrayList<>();
        Map<String, FilterableField> filterableFields = spec.getFilterableFields();

        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            Matcher matcher = FILTER_PATTERN.matcher(entry.getKey());
            if (!matcher.matches()) continue;

            String fieldName = matcher.group(1);
            String operatorStr = matcher.group(2);

            FilterableField field = filterableFields.get(fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Unknown filter field: '" + fieldName
                        + "'. Allowed fields: " + filterableFields.keySet());
            }

            FilterOperator operator = FilterOperator.fromString(operatorStr);

            Set<FilterOperator> allowedOps = field.allowedOperators();
            if (!allowedOps.contains(operator)) {
                throw new IllegalArgumentException("Operator '" + operatorStr
                        + "' is not allowed for field '" + fieldName
                        + "'. Allowed operators: " + allowedOps);
            }

            String value = entry.getValue().getFirst();
            if (value != null && !value.isBlank()) {
                filters.add(new FilterCriteria(fieldName, operator, value));
            }
        }

        return filters;
    }

    private <T> List<SortCriteria> parseSorts(MultiValueMap<String, String> queryParams, EntityFilterSpec<T> spec) {
        List<SortCriteria> sorts = new ArrayList<>();
        List<String> sortParams = queryParams.get("sort");
        if (sortParams == null) return sorts;

        Map<String, String> sortableFields = spec.getSortableFields();

        for (String sortParam : sortParams) {
            String[] parts = sortParam.split(",", 2);
            String fieldName = parts[0].trim();

            if (!sortableFields.containsKey(fieldName)) {
                throw new IllegalArgumentException("Unknown sort field: '" + fieldName
                        + "'. Allowed fields: " + sortableFields.keySet());
            }

            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length > 1) {
                String dir = parts[1].trim().toLowerCase();
                if ("desc".equals(dir)) {
                    direction = Sort.Direction.DESC;
                } else if (!"asc".equals(dir)) {
                    throw new IllegalArgumentException("Invalid sort direction: '" + dir
                            + "'. Use 'asc' or 'desc'");
                }
            }

            sorts.add(new SortCriteria(fieldName, direction));
        }

        return sorts;
    }

    private int parseIntParam(MultiValueMap<String, String> queryParams, String name, int defaultValue) {
        String value = queryParams.getFirst(name);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for '" + name + "': " + value);
        }
    }
}
