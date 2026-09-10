# OWASP Top 10:2025 Audit — oxalate-backend + oxalate-frontend

Date: 2026-02-19 · Scope: backend, frontend, configuration, CI · Method: static review of the complete authorization surface, plus targeted dynamic probes
through MockMvc against a containerized PostgreSQL

Playbook: [`../../OWASP-2025-Top10.md`](../../OWASP-2025-Top10.md)

## Executive summary

The portal was in reasonable shape against the classic injection categories — every native query uses named parameters, and the rich-text surfaces already
sanitise through DOMPurify — but authorization was enforced inconsistently, and the weakest points were exactly where the task asked us to look: the endpoints
that should require `ORGANIZER` or `ADMIN`.

The three findings that matter:

1. **`MembershipController` had no authorization annotations at all** (F-01). Any authenticated member could create an active membership for themselves, or read
   and modify anyone else's, defeating the paid-membership gate on dive events.
2. **Five further endpoints were authenticated-but-unauthorized** (F-02), including the full dive-data CSV export and aggregate club statistics.
3. **A live reCAPTCHA secret key and a production-shaped JWT signing key sat in plaintext inside the source tree** (F-03), in `local.yaml`. That file is
   gitignored, so the values never reached git history — but they were the effective local defaults, and nothing prevented the same values being carried into a
   deployment. Startup validation now makes that impossible, and the reCAPTCHA key should still be rotated.

21 findings total: 2 critical, 6 high, 9 medium, 4 low. All are fixed. 112 new backend tests in
`io.oxalate.backend.security` and 16 new frontend tests pin every fix.

## Coverage matrix

| Category                                   | Checked | Findings (C/H/M/L) | Status              |
|--------------------------------------------|---------|--------------------|---------------------|
| A01 Broken Access Control                  | yes     | 2/2/1/0            | fixed               |
| A02 Security Misconfiguration              | yes     | 0/2/3/0            | fixed               |
| A03 Software Supply Chain Failures         | yes     | 0/0/1/1            | 1 fixed, 1 deferred |
| A04 Cryptographic Failures                 | yes     | 0/2/1/1            | fixed               |
| A05 Injection                              | yes     | 0/0/1/1            | fixed               |
| A06 Insecure Design                        | yes     | 0/0/0/1            | fixed               |
| A07 Authentication Failures                | yes     | 0/0/1/0            | fixed               |
| A08 Software or Data Integrity Failures    | yes     | 0/0/0/0            | no findings         |
| A09 Security Logging and Alerting Failures | yes     | 0/0/1/1            | fixed               |
| A10 Mishandling of Exceptional Conditions  | yes     | 0/0/3/1            | fixed               |

## Findings

### [CRITICAL] F-01 — `MembershipController` performed no authorization · A01:2025 · CWE-862

- **Location:** `service/src/main/java/io/oxalate/backend/controller/MembershipController.java`
- **Description:** Not one of the five endpoints carried `@PreAuthorize`, and none checked ownership. Because
  `WebSecurityConfig` only requires that the request be authenticated, every logged-in member could reach all of them.
- **Impact:** Any `ROLE_USER` could `POST /api/memberships` an `ACTIVE` membership for their own account, bypassing the club's paid-membership requirement for
  dive events; `PUT` one for anybody else; and enumerate all members' membership records, which are personal data. This is privilege escalation plus a
  horizontal IDOR against every member of the club.
- **Reproduction:** authenticate as a plain member, then
  `POST /api/memberships` with `{"id":0,"userId":<self>,"status":"ACTIVE","type":"PERIODICAL", ...}` → `200 OK`.
- **Fix applied:** `hasRole('ADMIN')` on `getAllActiveMemberships`, `getMembership`, `createMembership` and
  `updateMembership`. `getMembershipsForUser` is now `hasAnyRole('USER','ORGANIZER','ADMIN')` **plus** an imperative self-or-admin check that throws
  `OxalateUnauthorizedException(WARN, ..., FORBIDDEN)`, mirroring the
  `UserController` pattern. Note that `ORGANIZER` is deliberately *not* privileged here — organising dives does not imply a right to read other members' payment
  status. The new
  `AppAuditMessages.MEMBERSHIP_GET_FOR_USER_UNAUTHORIZED` records the refusal in the audit trail.
- **Verification:** `OwaspAccessControlRTC` — `createMembershipAsMemberFail`, `updateMembershipAsMemberFail`,
  `createMembershipAsOrganizerFail`, `getAllActiveMembershipsAsOrganizerFail`, `getMembershipByIdAsMemberFail`,
  `getMembershipsForAnotherUserFail`, `getMembershipsForAnotherUserAsOrganizerFail`, `getMembershipsForOwnUserOk`,
  `getMembershipsForAnotherUserAsAdministratorOk`, `getAllActiveMembershipsAsAdministratorOk`.
  `OwaspEndpointAuthorizationUTC.membershipMutationsAreAdminOnlyOk`.
- **Residual risk:** any membership rows created through this hole before the fix are still in the database. The membership table should be reviewed against
  payment records.

### [CRITICAL] F-02 — Five endpoints exposed privileged data to any authenticated user · A01:2025 · CWE-862

- **Location:** `DataDownloadController.downloadDives`, `StatsController.getAggregateStats`,
  `TagController.getTagGroupsByType` / `getTagsByGroupType`,
  `EmailNotificationSubscriptionController.subscribeToEmailNotifications`
- **Description:** These methods had no `@PreAuthorize`, so the "authenticated" baseline in `WebSecurityConfig`
  was the only gate.
- **Impact:** `downloadDives` returns a CSV of the entire club's dive history — names, dates, depths, locations.
  `getAggregateStats` exposes club-wide figures intended for the organizer dashboard.
- **Fix applied:** `hasRole('ADMIN')` on the data export, `hasAnyRole('ORGANIZER','ADMIN')` on the statistics and tag administration endpoints,
  `hasAnyRole('USER','ORGANIZER','ADMIN')` on the subscription endpoint (which is legitimately member-wide, and already scopes to the caller's own
  subscription).
- **Verification:** `OwaspAccessControlRTC` — `downloadDivesAsMemberFail`, `downloadDivesAsOrganizerFail`,
  `getAggregateStatsAsMemberFail`, `getTagGroupsAsMemberFail`. Regression is prevented structurally by
  `OwaspEndpointAuthorizationUTC.everyNonPublicEndpointDeclaresPreAuthorizeOk`, which reflectively scans every `@RestController` in
  `io.oxalate.backend.controller`, resolves each mapping through its API interface, and fails if a non-public endpoint has no `@PreAuthorize`.

### [HIGH] F-03 — Real secrets in a plaintext file inside the source tree · A04:2025 · CWE-798

- **Location:** `service/src/main/resources/local.yaml`
- **Description:** the file contained a 128-hex-character JWT signing key and a real reCAPTCHA site key + secret key as literal values, with no
  environment-variable indirection.
- **Scope check:** `local.yaml` is listed in `.gitignore` and is untracked, and a search of every reachable commit confirms **the values are not in git
  history**. What *is* committed is `application.yaml` with
  `jwt-secret: override-me` and `application-test.yaml` with an obvious `YWFhYWFh...` placeholder, neither of which is a real secret. This is why the finding is
  HIGH rather than CRITICAL.
- **Impact:** the residual risk is nonetheless real. Anyone with a copy of a developer's working tree — a support tarball, a backup, a container image built
  with a naive `COPY . .`, a screen share — obtains a working reCAPTCHA secret. More importantly, the key was the *default* the application would silently start
  with, so a deployment that forgot to set `OXALATE_JWT_SECRET` would have run on a key that several people already had, allowing forged administrator session
  cookies.
- **Fix applied:** the values are replaced with `${OXALATE_JWT_SECRET:...}` / `${OXALATE_CAPTCHA_*:...}`
  placeholders that are clearly labelled development-only, and captcha now defaults to disabled locally. The new
  `SecurityPreflight` component refuses to complete startup outside the `local`/`test` environments if the configured key is one of the known development keys,
  is not valid base64, decodes to fewer than 64 bytes, or if the captcha secret is a known development value. The committed `override-me` default is rejected by
  the same check, which closes the "forgot to set the environment variable" path for good.
- **Verification:** `OwaspSecurityPreflightUTC` — 12 cases driven through `MockEnvironment`.
- **Residual risk / follow-up:** **rotate the reCAPTCHA key pair in the Google admin console**, since that key is a real one and has been distributed to every
  developer who cloned and ran the project. The JWT key needs no rotation for exposure reasons, but F-07 invalidates all sessions anyway.

### [HIGH] F-04 — Client IP taken from attacker-controlled headers · A07:2025 · CWE-290

- **Location:** `service/src/main/java/io/oxalate/backend/tools/HttpTools.java`
- **Description:** `getRemoteIp` preferred `X-Forwarded-For`, then `X-Real-IP`, `Forwarded` and `X-Client-IP`, falling back to `getRemoteAddr()`. None of these
  headers is trustworthy: any client can set them.
- **Impact:** two concrete attacks. (1) `LoginAttemptService` keys its lockout on this value, so an attacker rotating `X-Forwarded-For` on every request never
  triggers the lockout — the brute-force control was bypassable by adding one header. (2) The `ipAddress` column of the audit trail could be forged, letting an
  attacker attribute their actions to an arbitrary address.
- **Fix applied:** `getRemoteIp` now returns `request.getRemoteAddr()` only, null-safe to `"unknown"`. Proxy handling is the container's job and is already
  configured through `server.forward-headers-strategy: native`, which populates `getRemoteAddr()` from the proxy's headers *only* when the request arrives from
  a trusted proxy. `AuthenticationFailureListener` was simplified accordingly.
- **Verification:** `OwaspHttpToolsUTC` — five cases asserting each header is ignored and that the lockout key stays stable while the headers rotate.

### [HIGH] F-05 — No CSRF defence of any kind · A01:2025 · CWE-352

- **Location:** `WebSecurityConfig`, `local.yaml`
- **Description:** CSRF tokens were disabled, and the session cookie was issued with `SameSite=None` in the local configuration. Nothing prevented a third-party
  site from issuing an authenticated state-changing request with the user's cookie attached.
- **Impact:** a member visiting a malicious page could be made to subscribe to, unsubscribe from, or modify dive events; an administrator could be made to
  change portal configuration or user roles.
- **Fix applied:** token-based CSRF is genuinely not available to this architecture — the JWT lives in an
  `HttpOnly` cookie the SPA cannot read, so it cannot echo a token. The OWASP-sanctioned alternative is applied instead, in two layers: `SameSite=Strict` on the
  cookie (enforced at startup by `SecurityPreflight`, which rejects `None`), and the new `CsrfOriginValidationFilter`, which rejects `POST`/`PUT`/`PATCH`/
  `DELETE` whose
  `Origin` — or `Referer`, reduced to `scheme://host[:port]` — is not in the configured allowed-origin set. Requests with neither header are passed through, so
  non-browser API clients still work.
- **Verification:** `OwaspCsrfOriginValidationFilterUTC` (11 cases) and
  `OwaspSecurityHeadersRTC` — `stateChangingRequestFromForeignOriginFail`,
  `stateChangingRequestFromForeignRefererFail`, `stateChangingRequestFromAllowedOriginReachesAuthorizationOk`.

### [HIGH] F-06 — Whole actuator surface was public · A02:2025 · CWE-497

- **Location:** `WebSecurityConfig`
- **Description:** `/actuator/**` was in the `permitAll` list.
- **Impact:** depending on the exposure configuration, `/actuator/env` leaks the resolved configuration including database credentials and the JWT key, and
  `/actuator/heapdump` leaks live session tokens.
- **Fix applied:** only `/actuator/health`, `/actuator/info` and the OpenAPI/Swagger paths are public; every other actuator path requires `ROLE_ADMIN`.
- **Verification:** `OwaspSecurityHeadersRTC` — `actuatorEnvironmentEndpointIsNotPublicFail`,
  `actuatorHeapDumpIsNotPublicFail`.

### [HIGH] F-07 — JWT validation accepted tokens it should have rejected · A04:2025 · CWE-347

- **Location:** `service/src/main/java/io/oxalate/backend/security/jwt/JwtUtils.java`
- **Description:** three separate problems. The class held a dead `Keys.secretKeyFor(HS512)` field generating an unused random key, which invited confusion
  about which key actually signs. The validation `catch` block caught
  `java.security.SignatureException` while jjwt throws `io.jsonwebtoken.security.SignatureException`, so a signature failure propagated as an unhandled
  exception rather than a clean rejection. And no `iss`/`aud` claims were set or required, so a token minted by any other system sharing the key would be
  accepted.
- **Impact:** token confusion, and an error path that did not fail closed.
- **Fix applied:** the class was rewritten against the jjwt 0.13 API. Tokens now carry `iss` and `aud`, and the parser is built with `verifyWith(SecretKey)`,
  `requireIssuer`, `requireAudience` and `clockSkewSeconds(30)`. Validation catches the correct exception types plus a catch-all so that it always fails closed.
- **Verification:** `OwaspJwtUtilsUTC` — 11 cases: valid, tampered signature, tampered payload, foreign key,
  `alg: none`, wrong issuer, wrong audience, missing claims, expired, garbage.
- **Residual risk / follow-up:** **requiring `iss`/`aud` invalidates every session issued before the deploy.**
  All users will be logged out once. Schedule accordingly.

### [HIGH] F-08 — reCAPTCHA protected only two endpoints, and demanded a token when disabled · A07:2025 · CWE-307

- **Location:** `service/src/main/java/io/oxalate/backend/security/RecaptchaFilter.java`
- **Description:** `PROTECTED_PATHS` covered login and registration only, leaving `lost-password`,
  `reset-password` and `registrations/resend-confirmation` unprotected — all unauthenticated endpoints that send email and are therefore abusable as a
  mail-bombing or user-enumeration primitive. Separately, the filter demanded the `X-Captcha-Token` header even when `oxalate.captcha.enabled` was false.
- **Fix applied:** the path set now covers all five endpoints, and `requiresCaptcha()` returns false whenever captcha is disabled. (The second half is not
  cosmetic: without it, extending the path set would have broken password reset in every environment that runs without captcha.)
- **Verification:** `OwaspRecaptchaFilterUTC` — `protectedPathsCoverEveryAbusableAuthEndpointOk`,
  `protectedPathsAreAllUnderTheAuthApiOk`.

### [MEDIUM] F-09 — CORS allowed-origins list was never split · A02:2025 · CWE-942

- **Location:** `WebSecurityConfig`
- **Description:** `Collections.singletonList(allowedOrigins)` passed the entire comma-joined property value to Spring as a *single* origin, so a multi-origin
  configuration matched nothing.
- **Impact:** on its own this fails closed, but it is a latent trap: the obvious "fix" when a legitimate origin is rejected is to widen the pattern to `*`. It
  also mattered immediately, because the new CSRF filter shares this list.
- **Fix applied:** a package-private `getAllowedOrigins()` splits the property on commas, trims, and drops empty entries; both the CORS configuration and
  `CsrfOriginValidationFilter` consume it.
- **Verification:** `OwaspCsrfOriginValidationFilterUTC` exercises multi-origin sets.

### [MEDIUM] F-10 — No security response headers · A02:2025 · CWE-693

- **Location:** `WebSecurityConfig` (backend), `vite.config.ts` (frontend)
- **Description:** the API sent no `Content-Security-Policy`, `X-Frame-Options`, `Strict-Transport-Security`,
  `Referrer-Policy` or `Permissions-Policy`, and the SPA had no CSP at all.
- **Fix applied:** the backend now sends a lockdown CSP appropriate for a JSON API
  (`default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`), `X-Frame-Options: DENY`, HSTS for one year with subdomains,
  `Referrer-Policy: no-referrer`, and a `Permissions-Policy` disabling camera, microphone, geolocation and payment. The frontend build injects a
  `<meta http-equiv="Content-Security-Policy">`
  allowing `'self'` plus the Google reCAPTCHA origins. The meta tag is injected on `build` only, because the Vite dev server needs inline scripts for hot module
  replacement.
- **Verification:** `OwaspSecurityHeadersRTC` — 14 cases including HSTS over a `.secure(true)` request.
- **Residual risk:** a `<meta>` CSP cannot express `frame-ancestors`. The same policy should also be set as a response header in the serving layer (nginx/CDN),
  where it can.

### [MEDIUM] F-11 — bcrypt cost factor 10 · A04:2025 · CWE-916

- **Location:** `WebSecurityConfig`
- **Fix applied:** `new BCryptPasswordEncoder(12)`. Existing `$2a$10$` hashes keep verifying, since the cost is encoded in the hash; they are upgraded naturally
  as users change their passwords.
- **Verification:** `OwaspSecurityHeadersRTC` — `passwordEncoderUsesConfiguredBcryptStrengthOk` asserts the `$2a$12$`
  prefix, `passwordEncoderStillVerifiesLegacyHashesOk` asserts backwards compatibility.

### [MEDIUM] F-12 — Unvalidated sort column and page size on the audit endpoint · A05:2025 · CWE-20

- **Location:** `service/src/main/java/io/oxalate/backend/controller/AuditController.java`
- **Description:** the `sorting` request parameter was split and handed to `Sort.by` unvalidated, and the page size was unbounded. An unknown property produced
  a `PropertyReferenceException` → HTTP 500. A `sorting` value of
  `","` produced an `ArrayIndexOutOfBoundsException`, because Java's `String.split` strips trailing empty tokens and returns an empty array for `","`.
- **Impact:** trivially triggered 500s on an admin endpoint, and an unbounded page size lets a single request pull the entire audit table — which is full of
  personal data — into memory.
- **Fix applied:** a `SORTABLE_COLUMNS` allow-list with `createdAt` as the default, a `MAX_PAGE_SIZE` of 200, and a `parseSorting` that handles null, blank,
  `","`, `",,,,"`, a missing direction and an unknown column.
- **Verification:** `OwaspErrorHandlingRTC` — parameterized over the malformed inputs above; all must return below 500.

### [MEDIUM] F-13 — Stack traces and framework detail returned to clients · A10:2025 · CWE-209

- **Location:** new `service/src/main/java/io/oxalate/backend/exception/GlobalExceptionHandler.java`
- **Description:** anything escaping `AuditAspect` reached the default error handler, which returns the exception message.
- **Fix applied:** an `@RestControllerAdvice` at lowest precedence returning an RFC 9457 `ProblemDetail` with a generic message and a `correlationId`; the full
  stack trace is logged server-side under the same id.
  `AccessDeniedException` and `AuthenticationException` are **deliberately re-thrown** — handling them here would swallow `AuthorizationDeniedException` and
  collapse the 401/403 distinction that Spring Security's
  `ExceptionTranslationFilter` produces.
- **Verification:** `OwaspErrorHandlingRTC` — 14 cases asserting no response body contains `io.oxalate`,
  `org.springframework`, `nullpointerexception` or similar.

### [MEDIUM] F-14 — Null dereference on missing certificate · A10:2025 · CWE-476

- **Location:** `service/src/main/java/io/oxalate/backend/controller/CertificateController.java`
- **Description:** `getCertificate` and `deleteCertificate` dereferenced the lookup result before the null check.
- **Fix applied:** the null checks were moved ahead of the dereference so a missing record is a handled 4xx.
- **Verification:** `OwaspErrorHandlingRTC.getUnknownCertificateReturnsClientErrorOk`.

### [MEDIUM] F-15 — Session state never cleared when the backend rejected the session · A07:2025 · CWE-613

- **Location:** `oxalate-frontend/src/services/sessionExpiryInterceptor.ts` (new),
  `configureAxiosBaseUrl.ts`
- **Description:** `AuthVerify` decided expiry purely from the `expiresAt` stored in `localStorage` and the local clock. If the backend revoked or invalidated
  the session, the UI kept presenting a signed-in application while every request failed.
- **Fix applied:** an axios response interceptor, registered on every API instance. A 401 always clears the stored session and redirects to `/login`. A 403 does
  so only when the stored session has already expired — 403 is also the ordinary "you lack this permission" answer and must not sign a user out mid-session.
- **Verification:** `src/__tests__/security.sessionExpiryInterceptor.test.ts` (8 cases).

### [MEDIUM] F-16 — Anonymous requests caused `ClassCastException` in `AuthTools` · A10:2025 · CWE-476

- **Location:** `service/src/main/java/io/oxalate/backend/tools/AuthTools.java`
- **Description:** the helpers cast the security principal to `UserDetailsImpl` unconditionally. For an anonymous request the principal is the string
  `"anonymousUser"`, so the cast threw.
- **Impact:** an authorization helper that throws instead of returning `false` is a fail-open pattern waiting to happen, and it produced 500s on paths reachable
  without authentication.
- **Fix applied:** a single private `getCurrentUserDetails()` using `instanceof` pattern matching; every helper now fails closed.
- **Verification:** covered indirectly by `OwaspAccessControlRTC` anonymous cases and `OwaspErrorHandlingRTC`.

### [LOW] F-17 — Unsanitised deployment-supplied HTML in the footer · A05:2025 · CWE-79

- **Location:** `oxalate-frontend/src/components/main/OxalateFooter.tsx`
- **Description:** the footer rendered `runtimeConfig.copyrightFooter` and `runtimeConfig.poweredByOxalate`
  through `dangerouslySetInnerHTML` without sanitising. These come from a mounted `runtime-config.js`, so this is operator-controlled rather than
  user-controlled input — hence LOW — but it was the only unsanitised sink left in the application, and it is on every page.
- **Fix applied:** wrapped in `DOMPurify.sanitize(..., {ADD_ATTR: ["target", "rel"]})`, matching what
  `BlogCard.tsx` and `Page.tsx` already do.
- **Verification:** `src/__tests__/security.OxalateFooter.test.tsx` — script tags, inline handlers and
  `javascript:` URLs are stripped while legitimate links survive.

### [LOW] F-18 — A render failure produced a blank page · A10:2025 · CWE-755

- **Location:** `oxalate-frontend/src/components/main/ErrorBoundary.tsx` (new), `src/index.tsx`
- **Fix applied:** an error boundary wrapping the application tree, rendering a neutral message and a reload button. The underlying error goes to the console
  only, never to the screen.
- **Verification:** `src/__tests__/security.ErrorBoundary.test.tsx`.

### [MEDIUM] F-19 — Entity `toString()` leaked the password hash and personal data · A09:2025 · CWE-532

- **Location:** `service/src/main/java/io/oxalate/backend/model/User.java`
- **Description:** Lombok's `@ToString` covers every field, so `User.toString()` rendered the bcrypt password hash, the phone number and the next-of-kin
  contact. `AuthService` interpolates a `User` into a log statement during login.
- **Impact:** password hashes and member personal data written into log files, which have a different retention, access-control and backup story than the
  database — and which the audit-trail purge job does not touch. This directly contradicts the project's own rule that personal data is never logged.
- **Fix applied:** `@ToString.Exclude` on `password`, `phoneNumber` and `nextOfKin`.
- **Verification:** `OwaspEntityToStringUTC` — `userToStringOmitsPasswordOk`, `userToStringOmitsContactDetailsOk`.

### [MEDIUM] F-20 — Infinite recursion between `User` and `Membership` `toString()` · A10:2025 · CWE-674

- **Location:** `service/src/main/java/io/oxalate/backend/model/Membership.java`
- **Description:** `User.membership` renders each `Membership`, which rendered its `user` back, and so on. This was not theoretical — the test suite logs
  `User login... Got: [FAILED toString()]` because the call threw a
  `StackOverflowError` that Spring's logging happened to catch.
- **Impact:** any `log.info`/`log.warn` that interpolates a `User` or a `Membership` throws `StackOverflowError`. Where that is not caught it aborts the
  request, giving a one-request denial-of-service on whichever code path logs an entity. It also hid F-19, since the failed `toString()` masked what would
  otherwise have been leaked.
- **Fix applied:** `@ToString.Exclude` on `Membership.user`, breaking the cycle on one side while keeping the useful `userId`.
- **Verification:** `OwaspEntityToStringUTC` — `membershipToStringDoesNotRecurseOk`,
  `membershipToStringOmitsUserDetailsOk`.

### [LOW] F-21 — Two code paths hashed passwords at the default cost factor · A04:2025 · CWE-916

- **Location:** `service/src/main/java/io/oxalate/backend/model/User.java` (the `SignupRequest` constructor),
  `service/src/main/java/io/oxalate/backend/service/AuthService.generatePasswordHash`
- **Description:** both instantiated `new BCryptPasswordEncoder()` directly, which is cost 10, rather than using the configured bean. So the F-11 increase to
  cost 12 would have applied to password *verification* but not to registration or password reset — every new password would still have been hashed at cost 10.
- **Fix applied:** both now use `WebSecurityConfig.BCRYPT_STRENGTH`.
- **Verification:** `OwaspEntityToStringUTC.signupHashUsesConfiguredCostFactorOk`.

## Verified safe — do not "fix" these

- **SQL injection (A05).** Every `@Query(nativeQuery = true)` in the repository layer uses named parameters. No string concatenation into SQL was found
  anywhere. Audit filtering uses Spring Data derived queries with
  `PageRequest`.
- **Path traversal (A05).** `FileTools.sanitizeFileName` already resolves uploads inside the configured upload directory. `OwaspFileToolsUTC` adds 14
  parameterized traversal cases as a regression guard.
- **Rich-text XSS (A05).** `BlogCard.tsx` and `Page.tsx` already sanitise CKEditor content with DOMPurify.
- **Login flow (A07).** `AuthService.authenticate` correctly requires `status == ACTIVE` and a non-empty role set.
- **Deserialization (A08).** No Java native deserialization, and Jackson polymorphic typing is not enabled.
- **Role naming (A01).** `hasRole('ADMIN')` correctly corresponds to `RoleEnum.ROLE_ADMIN` throughout.
- **`rel="noreferrer"`** in `UserDocumentFiles.tsx` and `DiveEventFiles.tsx` implies `noopener` in every current browser, so those links are not vulnerable to
  reverse tabnabbing.

## Accepted risks and deferred items

| Item                                                                     | Category | Why deferred                                                                                                                                                                                                                                                                                                     | Follow-up                                                                                                             |
|--------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Anonymous requests receive **403** where **401** is correct              | A01      | No `AuthenticationEntryPoint` is configured, so Spring Security answers 403 for unauthenticated requests. Fixing it is semantically right and would simplify the frontend interceptor, but it changes 34 existing `isForbidden()` assertions across three RTC classes and may break unaudited frontend handling. | Separate change: add `HttpStatusEntryPoint(UNAUTHORIZED)` and update the affected tests and the frontend interceptor. |
| Session mirrored into `localStorage`, roles client-tamperable            | A01/A07  | By design: the mirror drives UX only and the backend re-checks every request. Tampering with the stored roles changes which menu entries render, nothing more.                                                                                                                                                   | Documented in both `AGENTS.md` files.                                                                                 |
| No SCA (dependency scanning) step in CI                                  | A03      | Adding a scanner to the pipeline is a CI policy decision, not a code fix.                                                                                                                                                                                                                                        | Recommend OWASP Dependency-Check (Maven) and `yarn npm audit` (frontend) as a scheduled job.                          |
| `packageManager: yarn@4.18.0` but CI runs `corepack prepare yarn@4.13.0` | A03      | A one-line CI change, but it belongs to whoever owns the pipeline and changes build reproducibility.                                                                                                                                                                                                             | Align the two versions.                                                                                               |
| `react-csv` is unmaintained                                              | A03      | Replacing it is a functional change beyond the audit's scope.                                                                                                                                                                                                                                                    | Replace with a maintained exporter or a small local implementation.                                                   |
| Raw backend `data.message` shown in some antd toasts                     | A10      | Widespread pattern; changing every call site risks regressing user-visible behaviour.                                                                                                                                                                                                                            | Migrate to translated messages screen by screen (e.g. `AdminCertificateClassifications.tsx`).                         |
| Whether one `ORGANIZER` may modify another organizer's event             | A01      | `EventController.updateEvent` and `getEventDives` grant any `ORGANIZER`. This may well be the intended club policy — organisers cover for each other — but it was not confirmed with the product owner.                                                                                                          | Confirm the intent; if ownership should be enforced, add the check and an `OwaspAccessControlRTC` case.               |

## Recommendations not implemented (require approval)

1. **Rotate the reCAPTCHA key pair** — see F-03. Requires access to the Google reCAPTCHA admin console.
2. **Add SCA to CI** — OWASP Dependency-Check for Maven and `yarn npm audit --all` for the frontend, failing the build on HIGH.
3. **Serve the CSP as a response header** from the reverse proxy so `frame-ancestors` and `report-uri` apply.
4. **Return 401 for unauthenticated requests** (see the deferred table).
5. **Multi-factor authentication for `ROLE_ADMIN`** — the administrator role can read and export the entire membership's personal data, and is currently
   protected by a password alone.
6. **Alerting on the audit trail** — `WARN`-level authorization refusals are recorded but nothing watches them. A burst of `*_UNAUTHORIZED` events is exactly
   the signal that someone is probing the API.

## Changes at a glance

**Backend — new**

- `security/CsrfOriginValidationFilter.java`
- `security/SecurityPreflight.java`
- `exception/GlobalExceptionHandler.java`
-
`test/.../security/Owasp{AccessControlRTC, SecurityHeadersRTC, ErrorHandlingRTC, EndpointAuthorizationUTC, JwtUtilsUTC, HttpToolsUTC, CsrfOriginValidationFilterUTC, SecurityPreflightUTC, FileToolsUTC, RecaptchaFilterUTC, EntityToStringUTC}.java` —
112 tests

**Backend — modified**

`WebSecurityConfig`, `JwtUtils`, `RecaptchaFilter`, `RecaptchaService`, `AuthenticationFailureListener`,
`HttpTools`, `AuthTools`, `MembershipController`, `DataDownloadController`, `StatsController`, `TagController`,
`EmailNotificationSubscriptionController`, `AuditController`, `CertificateController`, `AppAuditMessages`,
`AuthService`, `model/User`, `model/Membership`, `application.yaml`, `application-test.yaml`, `AGENTS.md`, and the untracked `local.yaml` (gitignored, so it
will not appear in the diff — apply the same change to your own copy)

**Frontend — new**

- `components/main/ErrorBoundary.tsx`
- `services/sessionExpiryInterceptor.ts`
- `__tests__/security.{OxalateFooter, ErrorBoundary}.test.tsx`, `__tests__/security.sessionExpiryInterceptor.test.ts` — 16 tests

**Frontend — modified**

`OxalateFooter.tsx`, `components/main/index.ts`, `index.tsx`, `configureAxiosBaseUrl.ts`, `AuthAPI.ts`,
`vite.config.ts`, `.env`, `AGENTS.md`

## Verification

```bash
cd oxalate-backend  && ./mvnw clean verify          # 426 tests
cd oxalate-frontend && yarn test && yarn lint        # 565 tests, lint clean

# security suite only
cd oxalate-backend && ./mvnw -pl service -am -Dtest='Owasp*' -Dsurefire.failIfNoSpecifiedTests=false test
```
