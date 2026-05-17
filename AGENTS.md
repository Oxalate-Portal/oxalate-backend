# AGENTS.md - Oxalate Backend

Spring Boot 4.0.6 / Java 25, Maven multi-module backend (`api` + `service`) for events, users, payments, memberships, notifications, pages, and audit trail.

## Architecture essentials

- `api/` contains REST contracts + DTOs only; `service/` contains controllers, business logic, persistence, security, AOP.
- Controllers implement interfaces from `api/src/main/java/io/oxalate/backend/rest/` (example: `EventAPI` -> `EventController`).
- Keep controllers thin: orchestration + auth checks + response wrapping; business rules belong in services.
- Security is stateless JWT via HTTP-only cookie (`JWT_TOKEN`), not `Authorization` header (see `WebSecurityConfig`, `JwtCookieAuthenticationFilter`).
- Audit is AOP-based: methods annotated with `@Audited` are wrapped by `AuditAspect` and stored asynchronously to `application_audit_event`.

## Audit and exception pattern (important)

- Add `@AuditSource("ControllerName")` at class level and `@Audited(startMessage, okMessage, failMessage?)` on controller methods.
- Audit texts are constants in `service/src/main/java/io/oxalate/backend/events/AppAuditMessages.java`.
- For controlled failures inside controller flow, throw typed `OxalateAuditException` subclasses (for example `OxalateValidationException`,
  `OxalateUnauthorizedException`).
- `AuditAspect` catches those and returns `ResponseEntity` with the exception metadata; unexpected exceptions rethrow after fail audit.

## Data access conventions

- Prefer `JpaRepository` repositories.
- Use derived queries first; use native SQL with block text (`"""`) when needed (example: `MessageRepository`, `PageRepository`).
- For native joins returning non-entity columns, use projection interfaces (example: `MessageWithReadStatus`) and map in service.
- Blog article retrieval is implemented via role-filtered native queries in `PageRepository` and mapped in `PageService#getBlogArticles`.

## API and DTO conventions

- OpenAPI annotations belong on API interfaces, not controller implementations.
- REST paths are plural + kebab-case (see `documentation/CONVENTIONS.md`).
- JSON field naming is explicit with `@JsonProperty`; paging DTO example: `PagedRequest`/`PagedResponse`.

## Build, run, and debug workflows

```bash
./mvnw clean test
./mvnw clean verify
./db_local_setup.sh
./mvnw clean test spring-boot:run -Dspring-boot.run.jvmArguments='-Dspring.profiles.active=local -Dlogging.level.io.oxalate=debug -Doxalate.first-time=false -Doxalate.upload.directory=/var/tmp/oxalate'
./create_local_users.sh
```

## Multi-module test workflow

- If running a single class in `service`, include dependent module build (`-am`) because `service` depends on `api`.

```bash
./mvnw -pl service -am -Dtest=PageControllerRTC test
./mvnw -pl service -am -Dtest=PaymentServiceITC test
```

## Test structure used in this repo

- `*UTC`: unit tests with Mockito.
- `*ITC`: integration tests with Spring Boot + Testcontainers; extend `AbstractIntegrationTest` (`postgres:18-alpine`).
- `*RTC`: REST tests with MockMvc + Spring Security against containerized DB.
- Test method naming pattern: `methodScenarioOk/Fail` (camelCase).

## Database and migrations

- PostgreSQL + Flyway; schema is migration-driven (`ddl-auto: validate`).
- Migrations are in `service/src/main/resources/db/migration/` as `V{N}__description.sql`.

## Key files to learn first

- `service/src/main/java/io/oxalate/backend/aspect/AuditAspect.java`
- `service/src/main/java/io/oxalate/backend/events/AppEventPublisher.java`
- `service/src/main/java/io/oxalate/backend/security/WebSecurityConfig.java`
- `service/src/main/java/io/oxalate/backend/controller/EventController.java`
- `service/src/main/java/io/oxalate/backend/service/PageService.java`
- `service/src/main/java/io/oxalate/backend/repository/PageRepository.java`
- `service/src/test/java/io/oxalate/backend/AbstractIntegrationTest.java`

