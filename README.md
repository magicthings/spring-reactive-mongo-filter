# Spring Reactive Mongo Filter

[![Maven Central](https://img.shields.io/maven-central/v/de.magicthings/spring-reactive-mongo-filter)](https://central.sonatype.com/artifact/de.magicthings/spring-reactive-mongo-filter)
[![Javadoc](https://javadoc.io/badge2/de.magicthings/spring-reactive-mongo-filter/javadoc.svg)](https://javadoc.io/doc/de.magicthings/spring-reactive-mongo-filter)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.x](https://img.shields.io/badge/Spring%20Boot-4.x-green)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Annotation-driven filtering, sorting, and pagination for **Spring WebFlux** + **Reactive MongoDB**.

No existing library covers WebFlux + Reactive MongoDB + REST query parameter filtering together. This library fills that gap with a clean, annotation-driven API.

---

## Table of Contents

- [Features](#features)
- [Compatibility](#compatibility)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Query Parameters](#query-parameters)
- [API Reference](#api-reference)
- [Advanced Configuration](#advanced-configuration)
- [Error Handling](#error-handling)
- [Security](#security)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Filtering** via query parameters: `filter[field][operator]=value`
- **Sorting** with multi-field support: `sort=field,asc|desc`
- **Pagination** with configurable max page size
- **Field whitelisting** per entity via `EntityFilterSpec`
- **`@Filtered` annotation** for zero-boilerplate controller integration
- **Spring Boot auto-configuration** -- no `@ComponentScan` or `@Import` required
- **Injection-safe** -- field whitelist, operator whitelist, type conversion, regex escaping

## Compatibility

| Library Version | Java | Spring Boot | Spring Data MongoDB Reactive |
|-----------------|------|-------------|------------------------------|
| 1.0.x           | 21+  | 4.x         | 5.x                         |

## Getting Started

### Maven

```xml
<dependency>
    <groupId>de.magicthings</groupId>
    <artifactId>spring-reactive-mongo-filter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("de.magicthings:spring-reactive-mongo-filter:1.0.0")
```

### Gradle (Groovy DSL)

```groovy
implementation 'de.magicthings:spring-reactive-mongo-filter:1.0.0'
```

All required beans are auto-configured when `ReactiveMongoTemplate` is on the classpath. No additional setup needed.

## Usage

### 1. Define a filter spec

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

### 2. Annotate your controller

Use `@Filtered` to resolve a `FilterRequest` from query parameters automatically:

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Mono<FilteredPage<Product>> getAll(
            @Filtered(ProductFilterSpec.class) FilterRequest filterRequest) {
        return productService.findAll(filterRequest);
    }
}
```

### 3. Execute the filtered query

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ReactiveFilterRepository filterRepository;
    private final ProductFilterSpec filterSpec;

    public Mono<FilteredPage<Product>> findAll(FilterRequest filterRequest) {
        return filterRepository.findFiltered(filterRequest, filterSpec, Product.class);
    }
}
```

That's it -- no parser setup, no criteria building, no pagination math.

**Example request:**

```
GET /api/products?filter[active][eq]=true&filter[price][lte]=50&sort=created,desc&page=0&size=10
```

## Query Parameters

### Filtering

```
GET /api/products?filter[name][like]=phone
GET /api/products?filter[category][eq]=electronics
GET /api/products?filter[active][eq]=true
GET /api/products?filter[created][gte]=2025-01-01T00:00:00
GET /api/products?filter[category][in]=electronics,books,toys
```

Multiple filters are combined with AND logic:

```
GET /api/products?filter[active][eq]=true&filter[price][lte]=100
```

### Operators

| Operator | Description                 | Supported Types                          |
|----------|-----------------------------|------------------------------------------|
| `eq`     | Equals                      | String, Boolean, DateTime, Integer, Long |
| `neq`    | Not equals                  | String, Integer, Long                    |
| `like`   | Contains (case-insensitive) | String                                   |
| `gt`     | Greater than                | DateTime, Integer, Long                  |
| `lt`     | Less than                   | DateTime, Integer, Long                  |
| `gte`    | Greater than or equal       | DateTime, Integer, Long                  |
| `lte`    | Less than or equal          | DateTime, Integer, Long                  |
| `in`     | In list (comma-separated)   | String, Integer, Long                    |

### Sorting

```
GET /api/products?sort=price,asc
GET /api/products?sort=price,asc&sort=created,desc
```

Default direction is `asc`. Multi-sort is supported by repeating the `sort` parameter. When no `sort` parameter is provided, the default sort from `EntityFilterSpec.getDefaultSort()` is applied (`created DESC`).

### Pagination

| Parameter | Default | Max   | Description             |
|-----------|---------|-------|-------------------------|
| `page`    | `0`     | --    | Page number (0-indexed) |
| `size`    | `20`    | `100` | Items per page          |

## API Reference

Full Javadoc is available at [javadoc.io](https://javadoc.io/doc/de.magicthings/spring-reactive-mongo-filter).

### `EntityFilterSpec<T>`

Interface to implement per entity. Defines the whitelist of filterable and sortable fields.

| Method                  | Description                                           | Default          |
|-------------------------|-------------------------------------------------------|------------------|
| `getFilterableFields()` | Returns allowed filter fields and their types         | *(required)*     |
| `getSortableFields()`   | Returns allowed sort fields (`param` -> `mongoField`) | *(required)*     |
| `getDefaultSort()`      | Sort applied when no `sort` param is given            | `created DESC`   |
| `getBaseCriteria()`     | Criteria added to every query (e.g. soft-delete)      | `deleted: false` |

### `FilterableField`

Defines a filterable field with its type and allowed operators. Factory methods accept either a single `name` (used as both query parameter and MongoDB field) or a pair `(paramName, documentField)` when they differ.

| Factory Method                           | Operators                                     |
|------------------------------------------|-----------------------------------------------|
| `FilterableField.string(name)`           | `eq`, `neq`, `like`, `in`                     |
| `FilterableField.string(param, field)`   | `eq`, `neq`, `like`, `in`                     |
| `FilterableField.bool(name)`             | `eq`                                           |
| `FilterableField.bool(param, field)`     | `eq`                                           |
| `FilterableField.dateTime(name)`         | `eq`, `gt`, `lt`, `gte`, `lte`                |
| `FilterableField.dateTime(param, field)` | `eq`, `gt`, `lt`, `gte`, `lte`                |
| `FilterableField.integer(name)`          | `eq`, `neq`, `gt`, `lt`, `gte`, `lte`, `in`   |
| `FilterableField.longType(name)`         | `eq`, `neq`, `gt`, `lt`, `gte`, `lte`, `in`   |

> **Note:** `integer` and `longType` only support the single-name variant. Use the `FilterableField` constructor directly if you need a different parameter-to-field mapping for these types.

### `FilteredPage<T>`

Record returned by `ReactiveFilterRepository.findFiltered()`:

```java
public record FilteredPage<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
)
```

| Method          | Description                      |
|-----------------|----------------------------------|
| `isFirst()`     | `true` if this is the first page |
| `isLast()`      | `true` if this is the last page  |
| `hasNext()`     | `true` if a next page exists     |
| `hasPrevious()` | `true` if a previous page exists |

### `ReactiveFilterRepository`

Main query execution component. Auto-configured as a Spring bean.

```java
public <T> Mono<FilteredPage<T>> findFiltered(
    FilterRequest filterRequest,
    EntityFilterSpec<T> spec,
    Class<T> entityClass,
    Criteria... additional
)
```

The `additional` varargs allows adding programmatic constraints on top of the user's filters (e.g. for multi-tenancy):

```java
filterRepository.findFiltered(
    filterRequest, productFilterSpec, Product.class,
    Criteria.where("tenantId").is(currentTenantId)
);
```

### `@Filtered`

Parameter annotation for WebFlux controller methods. Automatically resolves a `FilterRequest` from query parameters using the specified `EntityFilterSpec`. The referenced spec class must be a Spring-managed bean (`@Component`).

```java
@GetMapping
public Mono<FilteredPage<Product>> list(
        @Filtered(ProductFilterSpec.class) FilterRequest filterRequest) {
    // filterRequest is fully parsed and validated
}
```

## Advanced Configuration

### Custom MongoDB field names

When the query parameter name differs from the MongoDB document field:

```java
FilterableField.string("name", "displayName")       // ?filter[name][eq]=... queries "displayName"
FilterableField.dateTime("modified", "lastModified")
```

### Disabling the soft-delete filter

By default, `deleted: false` is added to every query. Override `getBaseCriteria()` to change or disable this:

```java
@Override
public List<Criteria> getBaseCriteria() {
    return List.of(); // no base criteria
}
```

### Custom default sort

```java
@Override
public List<SortCriteria> getDefaultSort() {
    return List.of(new SortCriteria("name", Sort.Direction.ASC));
}
```

### Overriding auto-configured beans

All beans are registered with `@ConditionalOnMissingBean`. Define your own to customize behavior:

```java
@Bean
public FilterRequestParser filterRequestParser() {
    return new CustomFilterRequestParser();
}
```

## Error Handling

Invalid filter requests throw `IllegalArgumentException` with descriptive messages:

| Error                      | Example Message                                                      |
|----------------------------|----------------------------------------------------------------------|
| Unknown filter field       | `Unknown filter field: 'secret'. Allowed fields: [name, category]`   |
| Invalid operator for field | `Operator 'like' is not allowed for field 'active'. Allowed: [EQ]`   |
| Unknown operator           | `Unknown filter operator: 'contains'. Supported: eq, neq, like, ...` |
| Invalid boolean value      | `Invalid boolean value: 'banana'. Use 'true' or 'false'`            |
| Invalid datetime value     | `Invalid value '2025-13-01' for type LocalDateTime`                  |
| Unknown sort field         | `Unknown sort field: 'secret'. Allowed fields: [name, created]`      |
| Invalid sort direction     | `Invalid sort direction: 'up'. Use 'asc' or 'desc'`                 |

To map these to HTTP 400 responses, add an exception handler in your application:

```java
@RestControllerAdvice
public class FilterExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, String>> handleFilterError(IllegalArgumentException ex) {
        return Mono.just(Map.of("error", ex.getMessage()));
    }
}
```

## Security

The library is designed to be injection-safe:

- **Field whitelist** -- only fields declared in `EntityFilterSpec` can be filtered or sorted
- **Operator whitelist** -- each field defines which operators are allowed
- **Type conversion** -- values are converted to typed Java objects before being passed to MongoDB
- **Regex escaping** -- the `like` operator uses `Pattern.quote()` to escape user input
- **No dot notation** -- field name regex `\w+` prevents access to nested fields like `credentials.hash`
- **Parameterized queries** -- Spring Data's Criteria API uses BSON encoding, not string concatenation

## Building from Source

```bash
git clone https://github.com/magicthings/spring-reactive-mongo-filter.git
cd spring-reactive-mongo-filter
./mvnw clean install
```

Requires Java 21+.

## Contributing

Contributions are welcome! Please open an [issue](https://github.com/magicthings/spring-reactive-mongo-filter/issues) or submit a [pull request](https://github.com/magicthings/spring-reactive-mongo-filter/pulls).

## License

This project is licensed under the [MIT License](LICENSE).