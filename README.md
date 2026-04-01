# Spring Reactive Mongo Filter

Reusable filtering, sorting, and pagination for Spring WebFlux microservices with reactive MongoDB.

No existing library covers **WebFlux + Reactive MongoDB + REST query parameter filtering** together. This library fills that gap with a clean, annotation-driven API.

## Features

- **Filtering** via query parameters: `filter[field][operator]=value`
- **Sorting** with multi-field support: `sort=field,asc|desc`
- **Pagination** with configurable max page size
- **Field whitelisting** per entity via `EntityFilterSpec`
- **`@Filtered` annotation** for zero-boilerplate controller integration
- **Spring Boot auto-configuration** — no component scan required
- **Injection-safe** — field whitelist, operator whitelist, type conversion, regex escaping

## Requirements

- Java 21+
- Spring Boot 4.x
- Spring WebFlux
- Spring Data MongoDB Reactive

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>de.magicthings</groupId>
    <artifactId>spring-reactive-mongo-filter</artifactId>
    <version>latest</version>
</dependency>
```

The library auto-configures all beans when `ReactiveMongoTemplate` is on the classpath. No `@ComponentScan` or `@Import` needed.

## Quick Start

### 1. Define a filter spec for your entity

Create a `@Component` that implements `EntityFilterSpec<T>` to declare which fields are filterable and sortable:

```java
@Component
public class ProductFilterSpec implements EntityFilterSpec<Product> {

    private static final Map<String, FilterableField> FILTERABLE_FIELDS = Map.of(
        "name",      FilterableField.string("name"),
        "category",  FilterableField.string("category"),
        "price",     FilterableField.integer("price"),
        "active",    FilterableField.bool("active"),
        "created",   FilterableField.dateTime("created")
    );

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
        "name", "name",
        "price", "price",
        "created", "created"
    );

    @Override
    public Map<String, FilterableField> getFilterableFields() {
        return FILTERABLE_FIELDS;
    }

    @Override
    public Map<String, String> getSortableFields() {
        return SORTABLE_FIELDS;
    }
}
```

### 2. Use `@Filtered` in your controller

Annotate a `FilterRequest` parameter with `@Filtered` — Spring resolves it automatically from query parameters:

```java
@GetMapping
public Mono<ResponseEntity<Page<ProductDTO>>> getAll(
        @Filtered(ProductFilterSpec.class) FilterRequest filterRequest) {
    return productService.findAll(filterRequest)
            .map(ResponseEntity::ok);
}
```

### 3. Execute the filtered query in your service

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ReactiveFilterRepository filterRepository;
    private final ProductFilterSpec filterSpec;

    public Mono<Page<ProductDTO>> findAll(FilterRequest filterRequest) {
        return filterRepository
                .findFiltered(filterRequest, filterSpec, Product.class)
                .map(page -> /* map FilteredPage<Product> to your response DTO */);
    }
}
```

That's it. No parser setup, no criteria building, no pagination math.

## Query Parameter Reference

### Filtering

```
GET /api/products?filter[name][like]=phone
GET /api/products?filter[category][eq]=electronics
GET /api/products?filter[active][eq]=true
GET /api/products?filter[created][gte]=2025-01-01T00:00:00
GET /api/products?filter[category][in]=electronics,books,toys
```

Filters can be combined (AND logic):

```
GET /api/products?filter[active][eq]=true&filter[price][lte]=100
```

### Supported Operators

| Operator | Description              | Supported Types              |
|----------|--------------------------|------------------------------|
| `eq`     | Equals                   | String, Boolean, DateTime, Integer, Long |
| `neq`    | Not equals               | String, Integer, Long        |
| `like`   | Contains (case-insensitive) | String                    |
| `gt`     | Greater than             | DateTime, Integer, Long      |
| `lt`     | Less than                | DateTime, Integer, Long      |
| `gte`    | Greater than or equal    | DateTime, Integer, Long      |
| `lte`    | Less than or equal       | DateTime, Integer, Long      |
| `in`     | In list (comma-separated)| String, Integer, Long        |

### Sorting

```
GET /api/products?sort=price,asc
GET /api/products?sort=price,asc&sort=created,desc
```

Default direction is `asc`. Multi-sort is supported by repeating the `sort` parameter.

### Pagination

| Parameter | Default | Description          |
|-----------|---------|----------------------|
| `page`    | `0`     | Page number (0-indexed) |
| `size`    | `20`    | Page size (max 100)  |

```
GET /api/products?page=2&size=10
```

### Full Example

```
GET /api/products?filter[active][eq]=true&filter[price][lte]=50&sort=created,desc&page=0&size=10
```

## API Reference

### `EntityFilterSpec<T>`

Interface to implement per entity. Defines the whitelist of filterable/sortable fields.

| Method                  | Description                                  | Default                     |
|-------------------------|----------------------------------------------|-----------------------------|
| `getFilterableFields()` | Returns allowed filter fields and their types | *(required)*                |
| `getSortableFields()`   | Returns allowed sort fields (param -> mongo field) | *(required)*           |
| `getDefaultSort()`      | Sort applied when no `sort` param is given   | `created DESC`              |
| `getBaseCriteria()`     | Criteria added to every query (e.g. soft delete) | `deleted: false`        |

### `FilterableField`

Defines a single filterable field with its type and allowed operators.

```java
FilterableField.string("name")                     // eq, neq, like, in
FilterableField.string("name", "displayName")       // param name differs from mongo field
FilterableField.bool("active")                      // eq only
FilterableField.dateTime("created")                 // eq, gt, lt, gte, lte
FilterableField.integer("quantity")                  // eq, neq, gt, lt, gte, lte, in
FilterableField.longType("viewCount")               // eq, neq, gt, lt, gte, lte, in
```

### `FilteredPage<T>`

Returned by `ReactiveFilterRepository.findFiltered()`. A simple record containing the query results:

```java
public record FilteredPage<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
)
```

Helper methods: `isFirst()`, `isLast()`, `hasNext()`, `hasPrevious()`.

### `ReactiveFilterRepository`

The main query execution component. Injected automatically via auto-configuration.

```java
public <T> Mono<FilteredPage<T>> findFiltered(
    FilterRequest filterRequest,
    EntityFilterSpec<T> spec,
    Class<T> entityClass,
    Criteria... additional    // optional extra criteria
)
```

The `additional` varargs allows programmatic constraints on top of the user's filters. Example: restricting results to a subset of IDs (e.g. for multi-tenancy):

```java
filterRepository.findFiltered(
    filterRequest, productFilterSpec, Product.class,
    Criteria.where("tenantId").is(currentTenantId)
);
```

### `@Filtered`

Annotation for controller method parameters. Automatically resolves a `FilterRequest` from query parameters using the specified `EntityFilterSpec`:

```java
@GetMapping
public Mono<ResponseEntity<...>> list(
        @Filtered(MyEntityFilterSpec.class) FilterRequest filterRequest) {
    // filterRequest is parsed and validated
}
```

## Advanced Usage

### Custom MongoDB field names

When the query parameter name differs from the MongoDB document field:

```java
FilterableField.string("name", "displayName")     // ?filter[name][eq]=... queries "displayName"
FilterableField.dateTime("modified", "lastModified")
```

### Disabling soft-delete filter

By default, `deleted: false` is added to every query. Override `getBaseCriteria()` to change or disable this:

```java
@Override
public List<Criteria> getBaseCriteria() {
    return List.of(); // no base criteria
}
```

Or use a different field:

```java
@Override
public List<Criteria> getBaseCriteria() {
    return List.of(Criteria.where("active").is(true));
}
```

### Overriding default beans

All beans are registered with `@ConditionalOnMissingBean`. To customize behavior, define your own bean:

```java
@Bean
public FilterRequestParser filterRequestParser() {
    return new CustomFilterRequestParser();
}
```

## Error Handling

Invalid filter requests throw `IllegalArgumentException` with descriptive messages:

| Error                        | Example Message                                                          |
|------------------------------|--------------------------------------------------------------------------|
| Unknown filter field         | `Unknown filter field: 'secret'. Allowed fields: [name, category]`       |
| Invalid operator for field   | `Operator 'like' is not allowed for field 'active'. Allowed: [EQ]`       |
| Invalid operator name        | `Unknown filter operator: 'contains'. Supported: eq, neq, like, ...`     |
| Invalid boolean value        | `Invalid boolean value: 'banana'. Use 'true' or 'false'`                 |
| Invalid datetime value       | `Invalid value '2025-13-01' for type LocalDateTime`                      |
| Unknown sort field           | `Unknown sort field: 'secret'. Allowed fields: [name, created]`          |
| Invalid sort direction       | `Invalid sort direction: 'up'. Use 'asc' or 'desc'`                      |

These are caught by Spring's exception handling and typically mapped to **400 Bad Request**.

## Security

The library is designed to be injection-safe:

- **Field whitelist** — Only fields declared in `EntityFilterSpec` can be filtered or sorted. Prevents access to sensitive fields like `password` or internal metadata.
- **Operator whitelist** — Each field defines which operators are allowed. Boolean fields only accept `eq`, preventing nonsensical queries.
- **Type conversion** — Values are converted to typed Java objects before being passed to MongoDB. No raw strings in queries for non-string types.
- **Regex escaping** — The `like` operator uses `Pattern.quote()` to escape user input, preventing regex injection.
- **No dot notation** — Field name regex `\w+` does not allow dots, preventing access to nested fields like `credentials.hash`.
- **Parameterized queries** — Spring Data's Criteria API uses BSON encoding, not string concatenation. MongoDB operator injection (`$gt`, `$where`) is not possible.

## Package Structure

```
reactivemongofilter
├── annotation/     @Filtered
├── config/         ReactiveMongoFilterAutoConfiguration
├── model/          FilterCriteria, FilterOperator, FilterRequest,
│                   FilterableField, FilteredPage, SortCriteria
├── parser/         FilterRequestParser, FilterCriteriaBuilder
├── repository/     ReactiveFilterRepository
├── resolver/       FilteredArgumentResolver
└── spec/           EntityFilterSpec
```

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.

## License

This project is licensed under the [MIT License](LICENSE).