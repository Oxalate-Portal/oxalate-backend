# Conventions

## REST

### URI path form and case

The main resource group (for event, user, etc.) should be in plural form. For example:

```
    /api/users
```

The URI path should be in lower case, and use hyphens to separate words (ie. kebab-case). For example:

```
    /api/users/recover-password
```

### HTTP methods

| Method | Description |
|--------|-------------|
| GET    | Read        |
| POST¹  | Create      |
| PUT    | Update      |
| DELETE | Delete      |
| PATCH² | Update      |

¹ POST is also used when logging in, as it is not a RESTful operation.

² PATCH is not currently used in this project. It may be taken into use in the future for specific use cases.

### HTTP status codes

In order to minimize data leaking to the client, the server should always return the least descriptive status code possible. So instead of distinguishing
between "user not found" and "wrong password", the server should always return "invalid credentials". A simple rule is to pick one error code for all error
cases, and then only use HttpStatus.OK for the successful case. All error cases should however be logged in detail (but avoid personal information if possible)
on the server side for investigations.

### Response creation

We should stick to returning the response in the following manner:

```java
    return ResponseEntity.status(HttpStatus.OK).body(someResponseObject);
```

And in the case of an error:

```java
    log.error("Failed to zyx user ID {}: {}", userId, e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
```

### List endpoints and paging

Endpoints that return a table of rows page on the server. The rules below are the contract shared with `oxalate-frontend`.

**Request.** A paged endpoint is a `POST` whose API interface method takes `io.oxalate.backend.api.request.PagedRequest` as its JSON `@RequestBody`.
The body uses the snake_case naming of the rest of the API:

| Field            | Type           | Default          | Notes                                                                                                                                                                   |
|------------------|----------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `page`           | int, 0-based   | `0`              | negative values are treated as 0                                                                                                                                        |
| `size`           | int            | `25`             | values below 1 fall back to the default, values above `PagingTools.MAX_PAGE_SIZE` (200) are capped                                                                      |
| `sort_by`        | string         | endpoint default | a snake_case response field name, validated against the endpoint's allow-list; unknown values fall back to the default and are logged, never a 500                      |
| `direction`      | `ASC` / `DESC` | endpoint default | `org.springframework.data.domain.Sort.Direction`                                                                                                                        |
| `search`         | string         | none             | matched as a literal substring (`LIKE %term%`, wildcards escaped) against the endpoint's searchable string columns, OR-ed                                               |
| `case_sensitive` | boolean        | `false`          |                                                                                                                                                                         |
| `filter_column`  | string         | none             | a snake_case response field name; restricts `search` to that text column, or, for an enum column, matches `search` exactly against the enum constant name or wire value |

Endpoint specific filters are additional `@RequestParam`s next to the body, also named in snake_case (for example `event_id` on dive files,
`creator_id` on documents, `language` on blog articles).

**Response.** Every paged endpoint returns `ResponseEntity<PagedResponse<T>>` with this stable JSON shape:

```json
{ "content": [ ... ], "page": 0, "size": 25, "total_elements": 1234, "total_pages": 50, "first": true, "last": false, "empty": false }
```

Build it with `PagedResponse.fromPage(springPage, Entity::toResponse)`, or `PagedResponse.of(...)` when the rows do not come from a Spring Data page.
Endpoints that are switched off by configuration still answer with this shape (`PagingTools.emptyPage(pagedRequest)`), not with a plain empty list.

**Implementation.** The controller stays thin and hands the `PagedRequest` to the service. The service owns

- a `private static final Map<String, String> SORTABLE_COLUMNS` mapping the snake_case client sort names to entity property paths
  (`"username" -> "user.lastName"`, `"created_at" -> "createdAt"`) and a default sort column and direction, turned into a `Pageable` by
  `PagingTools.toPageable`;
- the searchable entity string properties, turned into a `Specification` by `PagingTools.searchSpecification`;
- the mandatory filters as `Specification`s (past events: `startTime < now`; active memberships: status `ACTIVE` and `endDate >= today or null`),
  combined with the optional search through `PagingTools.allOf`, which skips `null`s.

The repository extends `JpaRepository<E, Long>` and `JpaSpecificationExecutor<E>` and the query is `repository.findAll(specification, pageable)`.
Per-row enrichment (URLs, user names, participants) happens in the mapper passed to `fromPage`, so it is bounded by the page size.

**Tests.** A paged endpoint has a `*UTC` that verifies the `Pageable` handed to the repository (allow-listed sort mapped, unknown sort falls back) and a
`*RTC` that inserts enough rows for two pages and asserts the JSON shape, the sort direction, the search filter and that a forbidden `sort_by` yields 200
with the default order. The RTCs extend `PagedRestTestSupport`.

**Paged endpoints.** `GET /api/users`, `GET /api/events/past`, `GET /api/memberships`, `GET /api/tokens`, `GET /api/files/avatars`,
`GET /api/files/certificates`, `GET /api/files/dive-files`, `GET /api/files/documents`, `GET /api/files/page-files`, `GET /api/audits`,
`GET /api/audits/{userId}` and `GET /api/pages/blogs`. Small, bounded lists (configuration, tags, certificate classifications, page groups), exports,
reports and per-user aggregates deliberately stay unpaged.
