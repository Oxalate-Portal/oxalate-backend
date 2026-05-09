# AGENTS.md — Oxalate Backend

Spring Boot 4.0.6 / Java 25 multi-module Maven project. Backend for a dive-club portal managing events, users, payments, memberships, and CMS pages.

## Module Structure

| Module     | Role                                                                                                                                                                |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `api/`     | Shared contracts: REST interface definitions (`io.oxalate.backend.rest`), request/response DTOs (`io.oxalate.backend.api.{request,response}`), enums, and constants |
| `service/` | All runtime code: controllers, services, repositories, JPA models, security, AOP, scheduling                                                                        |

Controllers in `service/` **implement** interfaces from `api/`. Never put business logic in the `api` module.

## Key Packages (`service/src/main/java/io/oxalate/backend/`)

| Package       | Purpose                                                                                           |
|---------------|---------------------------------------------------------------------------------------------------|
| `controller/` | Thin REST layer — delegates immediately to services                                               |
| `service/`    | All business logic                                                                                |
| `repository/` | Spring Data JPA repositories                                                                      |
| `model/`      | JPA entities                                                                                      |
| `security/`   | JWT-cookie auth (`JwtCookieAuthenticationFilter`, `WebSecurityConfig`)                            |
| `aspect/`     | `AuditAspect` — intercepts `@Audited` controller methods                                          |
| `audit/`      | `@Audited` annotation + `AuditContext`                                                            |
| `events/`     | `AppAuditMessages` string constants + `AppEventPublisher`                                         |
| `exception/`  | Custom exceptions (`OxalateAuditException`, `OxalateNotFoundException`, etc.)                     |
| `tools/`      | `AuthTools` — `getCurrentUserId()`, `currentUserHasAnyRole()`, `currentUserHasNotAcceptedTerms()` |

## Controller Pattern

Every controller implements an `*API` interface from the `api` module:

```java
// api module: io.oxalate.backend.rest.EventAPI
@Tag(name = "EventAPI")
public interface EventAPI {
    String BASE_PATH = API + "/events";

    @GetMapping(path = BASE_PATH)
    ResponseEntity<List<EventResponse>> getFutureEvents();
}

// service module: EventController
@RequiredArgsConstructor
@RestController
@Slf4j
public class EventController implements EventAPI {
    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_GET_FUTURE_START, okMessage = EVENTS_GET_FUTURE_OK, failMessage = EVENTS_GET_FUTURE_FAIL)
    public ResponseEntity<List<EventResponse>> getFutureEvents() { ...}
}
```

- Swagger/OpenAPI annotations belong **only** on the `*API` interface, not the controller.
- All audit message strings are constants in `AppAuditMessages` — add new ones there first.
- Throw typed subclasses of `OxalateAuditException` for mid-method audit events; `AuditAspect` catches them and returns the correct HTTP response automatically.

## Response Pattern

```java
// Success
return ResponseEntity.status(HttpStatus.OK).

body(result);

// Error — return null body and log details server-side; do NOT expose internals
log.

error("Failed to ... ID {}: {}",id, e.getMessage(),e);
        return ResponseEntity.

status(HttpStatus.NOT_FOUND).

body(null);
```

## Authentication

JWT is stored in an HTTP-only cookie (not `Authorization` header). Roles: `ROLE_USER`, `ROLE_ORGANIZER`, `ROLE_ADMIN`. Use `@PreAuthorize("hasAnyRole(...)")` on
controller methods.

## Naming Conventions

| Context           | Convention                                                                 |
|-------------------|----------------------------------------------------------------------------|
| Java classes      | PascalCase                                                                 |
| Java variables    | camelCase                                                                  |
| REST URL paths    | kebab-case, plural resource (`/api/events`, `/api/users/recover-password`) |
| JSON fields       | snake_case                                                                 |
| DB tables/columns | snake_case                                                                 |
| YAML keys         | kebab-case                                                                 |

## Database & Migrations

PostgreSQL with Flyway. Migration scripts: `service/src/main/resources/db/migration/V{N}__description.sql`. Always add a new `V{N+1}__...sql` file; never modify
existing migrations. JPA DDL is set to `validate` — schema comes from Flyway only.

## Build & Run

```shell
# Run all tests
./mvnw clean test

# Run with dependency version enforcement
./mvnw clean verify

# Start local PostgreSQL (Docker) — wipes previous container/volume
./db_local_setup.sh

# Run the service locally (requires local.yaml from templates/local.yaml.template)
./mvnw clean test spring-boot:run \
  -Dspring-boot.run.jvmArguments='-Dspring.profiles.active=local \
  -Dlogging.level.io.oxalate=debug -Doxalate.first-time=false \
  -Doxalate.upload.directory=/var/tmp/oxalate'

# Seed local DB with test users/events
./create_local_users.sh

# Swagger UI (local)
open http://localhost:8081/actuator/swagger-ui/index.html
```

Profiles: `local`, `local-web`, `prod`. Test profile is `test` (set via `@ActiveProfiles("test")`). Copy `templates/local.yaml.template` →
`service/src/main/resources/local.yaml` and fill in values before running locally.

## Testing Conventions

Test class name suffixes encode the type:

| Suffix | Type                       | Framework                                       |
|--------|----------------------------|-------------------------------------------------|
| `UTC`  | Unit Test Class            | Mockito (`@ExtendWith(MockitoExtension.class)`) |
| `ITC`  | Integration Test Class     | Testcontainers + `@SpringBootTest`              |
| `RTC`  | REST/Controller Test Class | MockMvc-style full-stack                        |

Integration tests extend `AbstractIntegrationTest`, which spins up a `postgres:18-alpine` Testcontainers container and injects datasource properties
dynamically. No manual DB setup needed for `./mvnw test`.

## Key Files to Reference

- `service/src/main/java/io/oxalate/backend/aspect/AuditAspect.java` — audit lifecycle
- `service/src/main/java/io/oxalate/backend/events/AppAuditMessages.java` — all audit message constants
- `service/src/main/java/io/oxalate/backend/tools/AuthTools.java` — auth helper utilities
- `service/src/main/java/io/oxalate/backend/security/WebSecurityConfig.java` — security config
- `service/src/main/java/io/oxalate/backend/controller/EventController.java` — canonical controller example
- `api/src/main/java/io/oxalate/backend/rest/EventAPI.java` — canonical API interface example
- `service/src/test/java/io/oxalate/backend/AbstractIntegrationTest.java` — integration test base
- `service/src/main/resources/application.yaml` — all configurable properties with profiles

