# AGENTS.md - Oxalate Backend

Spring Boot 4.1.1 / Java 25, Maven multi-module backend (`api` + `service`) for the Oxalate Portal.

## 1. Product context (read this first)

Oxalate Portal is a **membership portal for a scuba diving club/organization**. This repository is the server: it owns the data model, business rules,
authorization, and the REST API consumed by
[`oxalate-frontend`](https://github.com/Oxalate-Portal/oxalate-frontend).

The product exists to let a diving club:

- run a **member register** with self-service registration, GDPR-compliant anonymization, and diver certifications;
- publish and staff **dive events**, track who dived and how many dives each person made;
- gate participation behind **payments** and/or **memberships**;
- publish **CMS pages and blog articles** with per-role read/write visibility, in multiple languages;
- notify members by **email** and in-portal notifications;
- give admins **statistics, reports, and an immutable audit trail**.

Almost every product decision is **configurable at runtime** (see §5), because a single deployment image serves multiple organizations with different rules.
When implementing a feature, first ask: *should this be a portal configuration value rather than a hardcoded rule?*

The end-user-facing description of these flows (with screenshots, per role) lives in the frontend repository at
`documentation/user/index.md`. It is the closest thing to a product requirements document and is worth reading before changing user-visible behaviour.

### Roles

`RoleEnum` (`api/src/main/java/io/oxalate/backend/api/RoleEnum.java`): `ROLE_ANONYMOUS`, `ROLE_USER`, `ROLE_ORGANIZER`,
`ROLE_ADMIN`. Roles are additive in practice — an organizer is also a user.

| Role             | Can do                                                                                                                                                                                                                             |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ROLE_ANONYMOUS` | Read public pages, register, log in, reset password. It is not granted to accounts; it is the role the server assigns to unauthenticated callers (`AuthTools`) and the subject of anonymous page permissions.                      |
| `ROLE_USER`      | Browse/join/leave dive events and waiting lists, manage own profile, certificates, comments, email subscriptions, view own payments and dive stats.                                                                                |
| `ROLE_ORGANIZER` | Everything a user can, plus create/update/cancel dive events, set participant dive counts, manage pages they have write access to, view any user's details (payment status, certificates, next of kin) for safety/contact reasons. |
| `ROLE_ADMIN`     | Full access: member administration, roles and statuses, payments and memberships, portal configuration, page groups, moderation, statistics, audit trail.                                                                          |

### Core user lifecycle

`UserStatusEnum`: `REGISTERED` → `ACTIVE` → (`LOCKED` | `ANONYMIZED`).

1. Self-registration creates a `REGISTERED` user with `ROLE_USER` and a `REGISTRATION` token.
2. Email confirmation activates the account (`ACTIVE`). Confirmation email can be resent (limited retries).
3. Password reset and email change use `PASSWORD_RESET` / `EMAIL_CHANGE` tokens (`TokenType`).
4. Users can lock their own account; only an admin can unlock it.
5. **GDPR anonymization** (`AnonymizeService`) strips personal data and sets status `ANONYMIZED`, but keeps the row so historical event participation and
   statistics stay intact. Never hard-delete a user instead of anonymizing.
6. Repeated failed logins are throttled per IP by `LoginAttemptService` (see `documentation/index.md`).
7. Terms and conditions acceptance is tracked on the user (`approvedTerms`); admins can reset it globally to force re-acceptance.

### Dive events

`EventStatusEnum`: `DRAFTED`, `PUBLISHED`, `HELD`, `CANCELLED`. `DiveTypeEnum`: `BOAT`, `CAVE`, `COURSE`, `CURRENT`,
`OPEN_AND_CAVE`, `OPEN_WATER`, `SURFACE`. `ParticipantTypeEnum`: `ORGANIZER`, `USER`, `WAITING_LIST`.

- An event has an organizer, a start time, a duration, and safety limits (max depth, max dive duration, max participants).
- Users subscribe/unsubscribe; when the event is full they join a **waiting list** and are offered a place when one frees up. `general.waiting-list-hours`
  (default 12) controls how long that offer stays valid.
- Unless the event is surface-only, each participant is allotted one dive by default; the organizer adjusts real dive counts during or after the event. Dive
  counts feed the yearly "top divers" statistics.
- `DiveGroup` records buddy teams within an event.
- `BlockedDate` marks calendar dates on which events may not be scheduled.
- Events past their end time are closed to `HELD` automatically by `ClosingEventSchedule`.

### Payments and memberships

`PaymentTypeEnum`: `PERIODICAL`, `ONE_TIME`, `DISABLED`, `NONE`. `MembershipTypeEnum` / `PeriodicPaymentTypeEnum`:
`DISABLED`, `PERPETUAL`, `PERIODICAL`, `DURATIONAL`. `MembershipStatusEnum`: `ACTIVE`, `EXPIRED`, `CANCELLED`.

Payments and memberships are two independent gates on event participation, switched on by
`payment.event-require-payment` and `membership.event-require-membership`. Period lengths, start points and units are all configuration-driven — do not hardcode
"one year".

### CMS, blog, and commenting

`Page` → `PageVersion` (one per language) inside a `PageGroup`, with `PageRoleAccess` (per-role `readPermission` /
`writePermission`). `PageStatusEnum`: `DRAFTED`, `PUBLISHED`, `DELETED`. A reserved page group holds special pages such as the front page and the terms and
conditions. Editors may not remove read/write permission for their own role, so they cannot lock themselves out of a page.

Commenting covers page comments, event comments, and a forum: `CommentClassEnum` (`EVENT_COMMENTS`, `PAGE_COMMENTS`,
`FORUM_COMMENTS`), `CommentTypeEnum` (`TOPIC`, `USER_COMMENT`), `CommentStatusEnum` (`DRAFTED`,
`HELD_FOR_MODERATION`, `PUBLISHED`, `REJECTED`, `CANCELLED`), plus `CommentReport` with `ReportStatusEnum` (`PENDING`,
`APPROVED`, `CANCELLED`, `REJECTED`) for abuse reporting and moderation.

### Notifications

Emails are rendered from per-language templates in `service/src/main/resources/templates/` (de/en/es/fi/sv), written to an `EmailQueueEntry` (`EmailStatusEnum`:
`QUEUED`, `SENDING`, `SENT`, `FAILED`) and flushed by `EmailQueueSchedule`.
`EmailNotificationTypeEnum` (`EVENT`, `PAGE`) and `EmailNotificationDetailEnum` (`NEW`, `UPDATED`, `CANCELLED`,
`DELETED`, `WAITING_LIST_AVAILABLE`) describe what users can subscribe to. Admin bulk messages target a
`NotificationGroupEnum` audience (`ALL_REGISTERED`, `INACTIVE_DAYS`, `ACTIVE_MEMBERSHIP`, `NO_ACTIVE_MEMBERSHIP`,
`LOCKED_ACCOUNTS`, `NEVER_HAD_MEMBERSHIP`).

### Audit trail

Every notable action is recorded as an immutable `ApplicationAuditEvent` (`userId`, `AuditLevelEnum` = `INFO` / `WARN` /
`ERROR`, `traceId`, `ipAddress`, `source`, `message`). Entries contain PII and are purged automatically by
`AuditTrailCleanupSchedule`. The audit trail is admin-readable and cannot be edited or deleted from the UI.

## 2. Architecture essentials

- `api/` contains REST contracts, DTOs, and enums only; `service/` contains controllers, business logic, persistence, security, scheduling, and AOP.
- Controllers implement interfaces from `api/src/main/java/io/oxalate/backend/rest/` (example: `EventAPI` ->
  `EventController`).
- Keep controllers thin: orchestration + auth checks + response wrapping; business rules belong in services.
- Security is stateless JWT via HTTP-only cookie (`JWT_TOKEN`), not `Authorization` header (see `WebSecurityConfig`,
  `JwtCookieAuthenticationFilter`). Tokens carry and require `iss`/`aud` claims.
- CSRF tokens are disabled because the SPA cannot read the `HttpOnly` cookie to echo one. The replacement is
  `SameSite=Strict` on the cookie plus `CsrfOriginValidationFilter`, which rejects state-changing requests whose
  `Origin`/`Referer` is not an allowed origin. Do not re-enable one without the other.
- CORS is configured from `oxalate.cors.*`; `allowed-origins` is a comma-separated list and is parsed into individual origins
  (`WebSecurityConfig.getAllowedOrigins()`), which is also what the CSRF filter uses.
- Method security is enabled (`@EnableMethodSecurity`); per-endpoint authorization is expressed with `@PreAuthorize` on the controller method, not in the
  security config.
- Everything not explicitly permitted in `WebSecurityConfig` requires authentication. Currently public:
  `/api/auth/**`, `GET /api/third-party/*`, `/api/pages/**` (permission checked per call), `GET /api/files/**`,
  `GET /api/documents/**`, `GET /api/dive-plans/**`, `GET /api/configurations/frontend`, `/actuator/health`,
  `/actuator/info`, the actuator OpenAPI/Swagger paths, `/v3/api-docs/**`, and `/api/test/**` (local development only). Any other `/actuator/**` path requires
  `ROLE_ADMIN`.
- `RecaptchaFilter` protects the unauthenticated auth endpoints (login, register, lost/reset password, resend confirmation) before authentication runs. It is
  skipped entirely when `oxalate.captcha.enabled` is false.
- `SecurityPreflight` refuses to start the application when a deployed environment is configured with a committed development JWT key, a weak key,
  `SameSite=None`, an insecure cookie, or the `local` profile.

### REST API surface

| Interface (`api/src/main/java/io/oxalate/backend/rest/`) | Base path                               | Purpose                                             |
|----------------------------------------------------------|-----------------------------------------|-----------------------------------------------------|
| `AuthAPI`                                                | `/api/auth`                             | login/logout/register, password and email lifecycle |
| `UserAPI`                                                | `/api/users`                            | user profile and member management                  |
| `TokenAPI`                                               | `/api/tokens`                           | token issue/revoke                                  |
| `EventAPI`                                               | `/api/events`                           | dive events, subscribe, waiting list, dive counts   |
| `DiveGroupAPI`                                           | `/api/dive-groups`                      | buddy groups within an event                        |
| `PaymentAPI`                                             | `/api/payments`                         | payments                                            |
| `MembershipAPI`                                          | `/api/memberships`                      | memberships                                         |
| `CertificateAPI`                                         | `/api/certificates`                     | diver certificates                                  |
| `CertificateClassificationAPI`                           | `/api/certificate-classifications`      | certification catalogue                             |
| `PageAPI`                                                | `/api/pages`                            | public CMS page read + navigation                   |
| `PageManagementAPI`                                      | `/api/page-management`                  | CMS page/page-group CRUD                            |
| `CommentAPI`                                             | `/api/comments`                         | comments, forum, reports, moderation                |
| `NotificationAPI`                                        | `/api/notifications`                    | in-portal and bulk notifications                    |
| `EmailNotificationSubscriptionAPI`                       | `/api/email-notification-subscriptions` | per-user email subscriptions                        |
| `PortalConfigurationAPI`                                 | `/api/configurations`                   | runtime configuration (`/frontend` is public)       |
| `StatsAPI`                                               | `/api/stats`                            | statistics and dive reports                         |
| `AuditAPI`                                               | `/api/audits`                           | audit trail (admin)                                 |
| `FileTransferAPI`                                        | `/api/files`                            | uploads/downloads                                   |
| `DataDownloadAPI`                                        | `/api/data-download`                    | data exports                                        |
| `BlockedDateAPI`                                         | `/api/blocked-dates`                    | dates blocked for events                            |
| `ThirdPartyAPI`                                          | `/api/third-party`                      | third-party token access                            |
| `TestAPI`                                                | `/api/test`                             | local-only test data generation                     |

## 3. Audit and exception pattern (important)

- Add `@AuditSource("ControllerName")` at class level and `@Audited(startMessage, okMessage, failMessage?)` on controller methods.
- Audit texts are constants in `service/src/main/java/io/oxalate/backend/events/AppAuditMessages.java`.
- For controlled failures inside controller flow, throw typed `OxalateAuditException` subclasses (for example
  `OxalateValidationException`, `OxalateUnauthorizedException`).
- `AuditAspect` catches those and returns `ResponseEntity` with the exception metadata; unexpected exceptions rethrow after fail audit.

## 4. Data access conventions

- Prefer `JpaRepository` repositories.
- Use derived queries first; use native SQL with block text (`"""`) when needed (example: `MessageRepository`,
  `PageRepository`).
- For native joins returning non-entity columns, use projection interfaces (example: `MessageWithReadStatus`) and map in service.
- Blog article retrieval is implemented via role-filtered native queries in `PageRepository` and mapped in
  `PageService#getBlogArticles`.

## 5. Portal configuration (the main product surface)

Runtime behaviour is driven by `PortalConfiguration` rows (`groupKey`, `settingKey`, `valueType`, `defaultValue`,
`runtimeValue`, `requiredRuntime`, `description`), described by `PortalConfigEnum`
(`api/src/main/java/io/oxalate/backend/api/PortalConfigEnum.java`) and served by `PortalConfigurationService`. Defaults are seeded in Flyway migrations. Groups
and the keys they own:

| Group        | Keys                                                                                                                                                                                                                                                                                                          |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `general`    | `org-name`, `default-language`, `enabled-language`, `top-divers-list-size`, `timezone`, `blog-enabled`, `waiting-list-hours`                                                                                                                                                                                  |
| `email`      | `org-email`, `support-email`, `system-email` (all required at runtime), `email-enabled`, `email-notifications`, `email-notification-retries`                                                                                                                                                                  |
| `frontend`   | `min-event-length`, `max-event-length`, `max-dive-length`, `min-participants`, `max-participants`, `max-depth`, `types-of-event`, `max-certificates`                                                                                                                                                          |
| `payment`    | `event-require-payment`, `payment-enabled`, `single-payment-enabled`, `periodical-payment-method-type`, `periodical-payment-method-unit`, `payment-period-length`, `payment-period-start`, `payment-period-start-point`, `one-time-expiration-type`, `one-time-expiration-unit`, `one-time-expiration-length` |
| `membership` | `membership-type`, `membership-period-unit`, `membership-period-length`, `membership-period-start`, `membership-period-start-point`, `event-require-membership`                                                                                                                                               |
| `commenting` | `commenting-enabled`, `commenting-allow-editing`, `commenting-enabled-features`, `comments-report-trigger-level`, `comments-require-review`                                                                                                                                                                   |
| `files`      | `dive-files-supported`, `documents-supported`                                                                                                                                                                                                                                                                 |

The `frontend` group is exposed unauthenticated through `GET /api/configurations/frontend` and is what the UI uses for form limits; all other groups require an
authenticated session.

## 6. Files and uploads

Upload root comes from `oxalate.upload.directory`. Subdirectories are defined in `UploadDirectoryConstants`: `avatars`,
`certificates` (`certificates/{userId}/`), `dive-files` (`dive-files/{eventId}/{diveGroupId}/`), `documents`,
`page-files` (`page-files/{pageId}/{language}/`). Entities derive from `AbstractFile` in `model/filetransfer/`;
`UploadStatusEnum` is `UPLOADED`, `PUBLISHED`, `DELETED`.

## 7. Scheduled jobs

| Class                       | Interval     | Does                                         |
|-----------------------------|--------------|----------------------------------------------|
| `WaitingListScheduler`      | every 15 min | cleans up waiting lists for past events      |
| `EmailQueueSchedule`        | every 15 min | flushes the queued email table               |
| `ClosingEventSchedule`      | every 30 min | marks finished events as `HELD`              |
| `AuditTrailCleanupSchedule` | every 24 h   | purges expired audit entries (PII retention) |

## 8. API and DTO conventions

- OpenAPI annotations belong on API interfaces, not controller implementations.
- REST paths are plural + kebab-case; see `documentation/CONVENTIONS.md` for URI, HTTP method, status code, and response construction rules.
  `documentation/CODESTYLE.md` covers naming casing for Java, SQL, JSON, URLs, and YAML.
- Return the least descriptive error possible to the client (never distinguish "user not found" from "wrong password"), but log the detail server-side.
- JSON field naming is explicit with `@JsonProperty`; paging DTO example: `PagedRequest`/`PagedResponse`.
- Enums that cross the wire carry explicit JSON values (example: `DiveTypeEnum.OPEN_WATER` -> `"open-water"`). Changing one is a breaking API change — update
  the frontend enum in the same change.
- Nullability annotations come from JSpecify (`org.jspecify.annotations.NonNull` / `.Nullable`). The
  `org.springframework.lang.*` equivalents are deprecated as of Spring Framework 7 and must not be reintroduced.

## 9. Database and migrations

- PostgreSQL + Flyway; schema is migration-driven (`ddl-auto: validate`), so an entity change without a migration will fail startup.
- Migrations are in `service/src/main/resources/db/migration/` as `V{N}__snake_case_description.sql`. There are currently 41 of them; always add the next unused
  number and never edit an applied migration.
- Portal configuration defaults are seeded by migrations, so a new configuration key needs a migration too.

## 10. Build, run, and debug workflows

```bash
./mvnw clean test
./mvnw clean verify   # also runs maven-enforcer; CI runs `./mvnw clean verify package`
./db_local_setup.sh
./mvnw clean test spring-boot:run -Dspring-boot.run.jvmArguments='-Dspring.profiles.active=local -Dlogging.level.io.oxalate=debug -Doxalate.first-time=false -Doxalate.upload.directory=/var/tmp/oxalate'
./create_local_users.sh
```

Swagger UI runs at <http://localhost:8081/actuator/swagger-ui/index.html>. `test-data/test-large.sql` provides a large dataset; local credentials for both
datasets are documented in `README.md`.

### Multi-module test workflow

- If running a single class in `service`, include dependent module build (`-am`) because `service` depends on `api`.

```bash
./mvnw -pl service -am -Dtest=PageControllerRTC test
./mvnw -pl service -am -Dtest=PaymentServiceITC test
./mvnw -pl api -Dtest=RestContractTC test
```

## 11. Test structure used in this repo

- `*UTC`: unit tests with Mockito (21 classes).
- `*ITC`: integration tests with Spring Boot + Testcontainers; extend `AbstractIntegrationTest` (`postgres:18-alpine`)
  (5 classes).
- `*RTC`: REST tests with MockMvc + Spring Security against containerized DB (6 classes).
- Test method naming pattern: `methodScenarioOk/Fail` (camelCase).
- Every new REST endpoint must add or update a contract test. `api/src/test/java/io/oxalate/backend/api/RestContractTC.java`
  is the baseline contract gate; `ApiValueContractUTC` guards enum/constant wire values. Contract tests must verify the mapping, response type, OpenAPI
  metadata, and security declaration/public route classification.
- `Owasp*` classes in `service/src/test/java/io/oxalate/backend/security/` are the security regression suite; see section 12.

## 12. Security requirements (OWASP Top 10:2025)

`../OWASP-2025-Top10.md` (the workspace root, alongside this repository) is the full audit playbook.
`security/OWASP-2025-audit-report.md` records what the last audit found and fixed. **Every change must keep the rules below true**; they encode real
vulnerabilities that were found in this codebase, not hypotheticals.

### Non-negotiable rules

1. **Every endpoint declares `@PreAuthorize`.** The only exceptions are the public paths listed in section 2.
   `MembershipController` once shipped with no authorization at all, which let any logged-in member grant themselves an active membership and bypass the
   paid-membership gate on events.
   `OwaspEndpointAuthorizationUTC` fails the build if a new endpoint has no rule.
2. **`hasAnyRole('USER', 'ORGANIZER', 'ADMIN')` is not an authorization rule.** Those are all the roles that exist, so it means "any authenticated user". Use it
   only for genuinely member-wide endpoints, and never to guard an administrative action.
3. **A role check is not an ownership check.** Any endpoint that takes a `userId`, or another identifier that belongs to a user, must additionally verify
   ownership in the method body. `UserController.getUserDetails`,
   `UserController.updateUser` and `MembershipController.getMembershipsForUser` are the reference pattern:

   ```java
   @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
   public ResponseEntity<...> getSomethingForUser(long userId) {
       if (!AuthTools.currentUserHasRole(ROLE_ADMIN) && !AuthTools.isUserIdCurrentUser(userId)) {
           throw new OxalateUnauthorizedException(WARN, SOME_UNAUTHORIZED_MESSAGE + userId, HttpStatus.FORBIDDEN);
       }
       ...
   }
   ```

   Decide deliberately whether `ORGANIZER` should be able to read other members' data; it usually should not.
4. **Never commit a secret.** Signing keys, captcha secrets and passwords come from the environment.
   `local.yaml` may only contain values that are clearly labelled development-only *and* listed in
   `SecurityPreflight.KNOWN_DEVELOPMENT_JWT_SECRETS` so that they cannot be used in a deployed environment. If a secret is ever committed, rotating it is part
   of the fix; deleting the line is not enough because it stays in git history.
5. **Never trust a request header for identity or throttling.** Use `HttpTools.getRemoteIp`, which reads
   `request.getRemoteAddr()`. Reading `X-Forwarded-For` directly lets an attacker reset the login lockout on every attempt and forge the `ipAddress` in the
   audit trail. Proxy handling belongs to
   `server.forward-headers-strategy: native`.
6. **Validate anything that reaches Spring Data or the filesystem.** Sort columns must be checked against an allow-list (`AuditController.SORTABLE_COLUMNS`),
   page sizes must be capped, and file names must go through
   `FileTools.sanitizeFileName`. An unknown sort property is a 500, and a 500 that is cheap to trigger is a denial-of-service primitive.
7. **Check for null before dereferencing a lookup result.** A missing record must be a handled 4xx, never a
   `NullPointerException`.
8. **Never put backend detail in a response.** `GlobalExceptionHandler` returns an RFC 9457 `ProblemDetail` with a generic message and a correlation id; the
   stack trace goes to the log under the same id. Do not add handlers for `AccessDeniedException` or `AuthenticationException` there, or Spring Security can no
   longer distinguish 401 from 403.
9. **Never log secrets, tokens or personal data.** Audit messages describe the action, not the payload. Lombok's
   `@ToString` covers every field, so any new secret or personal-data field on an entity needs
   `@ToString.Exclude`, and any new bidirectional association needs it on one side or `toString()` recurses into a `StackOverflowError`. `User.password`,
   `User.phoneNumber`, `User.nextOfKin` and `Membership.user` are excluded for exactly these reasons.
10. **Hash passwords through the configured encoder.** Use the `PasswordEncoder` bean, or
    `WebSecurityConfig.BCRYPT_STRENGTH` when a bean is not reachable. Never `new BCryptPasswordEncoder()` — the no-argument constructor silently uses cost 10.

### Required tests

Security work is not done until it has a regression test in
`service/src/test/java/io/oxalate/backend/security/`:

| Test class                           | Guards                                                                      |
|--------------------------------------|-----------------------------------------------------------------------------|
| `OwaspEndpointAuthorizationUTC`      | every endpoint declares `@PreAuthorize`; admin surfaces require `ADMIN`     |
| `OwaspAccessControlRTC`              | role and ownership enforcement end to end, including IDOR and forged tokens |
| `OwaspJwtUtilsUTC`                   | tampered, unsigned, expired, wrong `iss`/`aud` tokens are rejected          |
| `OwaspSecurityPreflightUTC`          | insecure configuration prevents startup                                     |
| `OwaspCsrfOriginValidationFilterUTC` | cross-origin state-changing requests are rejected                           |
| `OwaspSecurityHeadersRTC`            | security headers, actuator exposure, bcrypt work factor                     |
| `OwaspHttpToolsUTC`                  | the client IP cannot be spoofed by a header                                 |
| `OwaspRecaptchaFilterUTC`            | all abusable unauthenticated POST endpoints are captcha protected           |
| `OwaspFileToolsUTC`                  | uploaded file names cannot escape the upload directory                      |
| `OwaspErrorHandlingRTC`              | malformed input yields a 4xx, and errors leak no implementation detail      |
| `OwaspEntityToStringUTC`             | entity `toString()` leaks no secret or personal data and does not recurse   |

When you add an endpoint that requires `ORGANIZER` or `ADMIN`, add a case to `OwaspAccessControlRTC` proving a plain `ROLE_USER` is rejected. When the endpoint
is user-scoped, add a case proving one member cannot read another member's data.

Run the suite with:

```bash
./mvnw -pl service -am -Dtest='Owasp*' -Dsurefire.failIfNoSpecifiedTests=false test
```

## 13. Starting points for common agent tasks

**Add or change a REST endpoint**

1. Add the method to the API interface in `api/.../rest/` with OpenAPI annotations.
2. Implement it in the corresponding controller with `@PreAuthorize` and `@Audited`, adding messages to
   `AppAuditMessages`.
3. Put business logic in the service; keep the controller thin.
4. Update `RestContractTC` (mapping, response type, OpenAPI metadata, security/public classification).
5. Add `*UTC` coverage for the service logic and `*RTC` coverage for authorization and response shape.
6. Mirror the change in `oxalate-frontend` (`src/services/`, `src/models/`) if the frontend consumes it.

**Add a domain field or entity**

1. Add the Flyway migration first (`V{next}__description.sql`), then the entity — `ddl-auto: validate` will catch drift.
2. Add/extend the DTO in `api/` and map it in the service, never expose the entity directly.
3. If the field is a date/time, remember the frontend converts by field name (see its `DATE_FIELD_PATTERNS`); prefer the existing naming (`startTime`,
   `endTime`, `createdAt`, ...) or coordinate the frontend change.

**Add a portal configuration value**

1. Add the key to `PortalConfigEnum` under the right group.
2. Seed its default row in a new Flyway migration.
3. Read it through `PortalConfigurationService`; never hardcode the behaviour it controls.
4. If the frontend needs it unauthenticated, put it in the `frontend` group; otherwise the UI reads it from the authenticated portal configuration.
5. Add the translated label/tooltip keys in the frontend locale files.

**Add an email notification**

1. Add the template for **every** language in `service/src/main/resources/templates/`.
2. Extend `EmailNotificationTypeEnum`/`EmailNotificationDetailEnum` and the subscription handling as needed.
3. Enqueue via the email queue rather than sending inline, so retries and the scheduler apply.

**Definition of done**

- `./mvnw clean verify` passes (this also runs the enforcer check for duplicate dependency versions).
- New/changed endpoints are covered by contract tests and by `*UTC`/`*RTC` tests.
- Every new endpoint declares `@PreAuthorize`, user-scoped endpoints verify ownership in the method body, and
  `ORGANIZER`/`ADMIN` endpoints have a denial case in `OwaspAccessControlRTC` (see section 12).
- No secret, token or personal data is committed, logged, or returned in an error response.
- Schema changes have a migration; configuration changes have a seeded default.
- Audit annotations and messages exist for user-visible actions.
- Personal data is anonymized, never deleted, and never logged.
- Deprecations are migrated rather than suppressed; if a full migration does not fit the change, state the remaining cleanup explicitly in the task/PR
  description.

## 14. Key files to learn first

- `service/src/main/java/io/oxalate/backend/aspect/AuditAspect.java`
- `service/src/main/java/io/oxalate/backend/events/AppEventPublisher.java`
- `service/src/main/java/io/oxalate/backend/security/WebSecurityConfig.java`
- `service/src/main/java/io/oxalate/backend/controller/UserController.java` (the role + ownership authorization pattern)
- `service/src/main/java/io/oxalate/backend/controller/EventController.java`
- `service/src/main/java/io/oxalate/backend/service/PageService.java`
- `service/src/main/java/io/oxalate/backend/repository/PageRepository.java`
- `service/src/main/java/io/oxalate/backend/service/PortalConfigurationService.java`
- `api/src/main/java/io/oxalate/backend/api/PortalConfigEnum.java`
- `api/src/test/java/io/oxalate/backend/api/RestContractTC.java`
- `service/src/test/java/io/oxalate/backend/AbstractIntegrationTest.java`
