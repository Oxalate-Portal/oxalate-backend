package io.oxalate.backend.api;

import io.oxalate.backend.api.RestEndpointInventory.Endpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Provider-neutral contract checks for the public REST API. These checks run in the API module, so a contract
 * regression is caught without a database or Docker.
 * <p>
 * The API interfaces are <b>discovered</b> from the classpath by {@link RestEndpointInventory} rather than listed
 * here. The previous version of this test held a hardcoded array of 23 interfaces, so a 24th interface was silently
 * unchecked, and it asserted {@code endpointCount >= 100} against an actual 164 endpoints, which meant the
 * completeness check could not fail.
 * <p>
 * What this test guarantees for <i>every</i> mapped endpoint:
 * <ul>
 *     <li>the mapping is unique;</li>
 *     <li>the method returns {@link ResponseEntity};</li>
 *     <li>the method declares OpenAPI {@link Operation} metadata;</li>
 *     <li>the method declares {@link ApiResponses} with at least one success and one error status;</li>
 *     <li>the method either declares {@link SecurityRequirement} or is classified as a public route.</li>
 * </ul>
 * The last three have shrink-only allow-lists for endpoints that do not comply yet.
 * <p>
 * What this test deliberately does <b>not</b> guarantee: that the declared statuses are actually produced. Declaring a
 * 404 is not the same as returning one. That is {@code RestEndpointTestCoverageUTC} in the service module, where
 * MockMvc lives.
 */
@DisplayName("REST contract: every declared endpoint is completely specified")
class RestContractTC {

    /**
     * Exact endpoint inventory. Pinned rather than a lower bound so that adding or removing an endpoint forces a
     * deliberate edit here. This is what turns "every new REST endpoint must add or update a contract test" from a
     * request in AGENTS.md into a rule the build checks.
     */
    private static final int EXPECTED_ENDPOINT_COUNT = 165;

    /**
     * Endpoints that do not yet declare their response statuses, as {@code Interface.method}.
     * <p>
     * This list may only shrink. Every entry is an endpoint whose contract is undocumented, so neither a client nor a
     * contract test can know what it is allowed to return. A new endpoint must declare {@code @ApiResponses} from the
     * start. See ../TODO-20260916.md item 4.
     */
    private static final Set<String> ENDPOINTS_WITHOUT_DECLARED_RESPONSES = Set.of();

    /**
     * Endpoints that declare a success status but no error status, so a client has no documented failure contract.
     * This list may only shrink. See ../TODO-20260916.md item 4.
     */
    private static final Set<String> ENDPOINTS_WITHOUT_DECLARED_ERROR_STATUS = Set.of();

    /**
     * Endpoints that are authenticated in {@code WebSecurityConfig} and carry a {@code @PreAuthorize} rule on the
     * controller, but do not say so in their OpenAPI metadata, so Swagger presents them as public.
     * <p>
     * A documentation defect rather than a security hole: {@code OwaspEndpointAuthorizationUTC} independently proves
     * the authorization rule exists. This list may only shrink. See ../TODO-20260916.md item 4.
     */
    private static final Set<String> ENDPOINTS_WITHOUT_SECURITY_DECLARATION = Set.of();

    @Test
    void everyApiInterfaceIsDiscoveredOk() {
        var interfaces = RestEndpointInventory.apiInterfaces();

        assertFalse(interfaces.isEmpty(),
                "Classpath scan of " + RestEndpointInventory.API_PACKAGE + " found no API interfaces at all");
        // A sanity floor only. Discovery is the real guarantee; this catches a broken scan configuration.
        assertTrue(interfaces.size() >= 20,
                () -> "Classpath scan found only " + interfaces.size() + " API interfaces, which is too few to be trustworthy");
    }

    @Test
    void theEndpointInventoryMatchesExactlyOk() {
        var endpoints = RestEndpointInventory.endpoints();

        assertEquals(EXPECTED_ENDPOINT_COUNT, endpoints.size(), () -> """
                The REST endpoint count changed. Expected %d, found %d.

                Adding or removing an endpoint is fine, but it must be deliberate:
                  1. update EXPECTED_ENDPOINT_COUNT in this test;
                  2. declare @Operation, @ApiResponses and @SecurityRequirement on the new endpoint;
                  3. add its status-code test and update
                     service/src/test/resources/rest-endpoint-test-coverage-baseline.txt.
                """.formatted(EXPECTED_ENDPOINT_COUNT, endpoints.size()));
    }

    @Test
    void everyEndpointHasAUniqueMappingOk() {
        var seen = new HashSet<String>();
        var duplicates = new TreeSet<String>();

        for (var endpoint : RestEndpointInventory.endpoints()) {
            if (!seen.add(endpoint.httpMethod() + " " + endpoint.path())) {
                duplicates.add(endpoint.describe());
            }
        }

        assertTrue(duplicates.isEmpty(), () -> "The same HTTP method and path is mapped more than once: " + duplicates);
    }

    @Test
    void everyEndpointReturnsAResponseEntityOk() {
        var offenders = offenders(endpoint -> !ResponseEntity.class.isAssignableFrom(endpoint.method()
                                                                                             .getReturnType()));

        assertTrue(offenders.isEmpty(), () -> "These endpoints must return ResponseEntity:\n" + join(offenders));
    }

    @Test
    void everyEndpointDeclaresOpenApiOperationMetadataOk() {
        var offenders = offenders(endpoint -> !endpoint.method()
                                                       .isAnnotationPresent(Operation.class));

        assertTrue(offenders.isEmpty(), () -> "These endpoints are missing OpenAPI @Operation:\n" + join(offenders));
    }

    /**
     * The declaration half of "contract tests must cover 200 and every declared error code": an endpoint that declares
     * no statuses has no error contract for a test to cover.
     */
    @Test
    void everyEndpointDeclaresItsResponseStatusesOk() {
        var offenders = offenders(endpoint -> endpoint.declaredStatusCodes()
                                                      .isEmpty()
                && !ENDPOINTS_WITHOUT_DECLARED_RESPONSES.contains(endpoint.id()));

        assertTrue(offenders.isEmpty(), () -> """
                These endpoints declare no @ApiResponses, so their contract is undocumented:
                %s
                Declare the statuses the endpoint can return. Do not add them to
                ENDPOINTS_WITHOUT_DECLARED_RESPONSES -- that list may only shrink.
                """.formatted(join(offenders)));
    }

    @Test
    void everyDeclaredResponseSetCoversSuccessAndFailureOk() {
        var missingSuccess = new ArrayList<String>();
        var missingFailure = new ArrayList<String>();

        for (var endpoint : RestEndpointInventory.endpoints()) {
            var declared = endpoint.declaredStatusCodes();
            if (declared.isEmpty()) {
                continue;
            }
            if (declared.stream()
                        .noneMatch(code -> code >= 200 && code < 400)) {
                missingSuccess.add(endpoint.describe() + " declares " + declared);
            }
            if (declared.stream()
                        .noneMatch(code -> code >= 400)
                    && !ENDPOINTS_WITHOUT_DECLARED_ERROR_STATUS.contains(endpoint.id())) {
                missingFailure.add(endpoint.describe() + " declares " + declared);
            }
        }

        assertTrue(missingSuccess.isEmpty(), () -> "These endpoints declare no success status:\n" + join(missingSuccess));
        assertTrue(missingFailure.isEmpty(), () -> "These endpoints declare no error status:\n" + join(missingFailure));
    }

    @Test
    void everyEndpointIsEitherSecuredOrClassifiedPublicOk() {
        var offenders = offenders(endpoint -> !endpoint.method()
                                                       .isAnnotationPresent(SecurityRequirement.class)
                && !endpoint.isPublicRoute()
                && !ENDPOINTS_WITHOUT_SECURITY_DECLARATION.contains(endpoint.id()));

        assertTrue(offenders.isEmpty(), () -> """
                These endpoints neither declare @SecurityRequirement nor sit on a public path:
                %s
                Either declare the authentication requirement, or add the path to the public allow-list in
                RestEndpointInventory, WebSecurityConfig and OwaspEndpointAuthorizationUTC.
                """.formatted(join(offenders)));
    }

    /**
     * Keeps the three allow-lists honest. Without this an endpoint could be fixed while its exemption lingered,
     * silently exempting whatever later reused the name, and the lists would never shrink.
     */
    @Test
    void theAllowListsHaveNoStaleEntriesOk() {
        var endpoints = RestEndpointInventory.endpoints();
        var knownIds = endpoints.stream()
                                .map(Endpoint::id)
                                .collect(Collectors.toSet());
        var stale = new TreeSet<String>();

        for (var entry : allAllowListEntries()) {
            if (!knownIds.contains(entry)) {
                stale.add(entry + " (no such endpoint)");
            }
        }

        for (var endpoint : endpoints) {
            var id = endpoint.id();
            var declared = endpoint.declaredStatusCodes();

            if (ENDPOINTS_WITHOUT_DECLARED_RESPONSES.contains(id) && !declared.isEmpty()) {
                stale.add(id + " now declares @ApiResponses -- remove it from ENDPOINTS_WITHOUT_DECLARED_RESPONSES");
            }
            if (ENDPOINTS_WITHOUT_DECLARED_ERROR_STATUS.contains(id)
                    && declared.stream()
                               .anyMatch(code -> code >= 400)) {
                stale.add(id + " now declares an error status -- remove it from ENDPOINTS_WITHOUT_DECLARED_ERROR_STATUS");
            }
            if (ENDPOINTS_WITHOUT_SECURITY_DECLARATION.contains(id)
                    && endpoint.method()
                               .isAnnotationPresent(SecurityRequirement.class)) {
                stale.add(id + " now declares @SecurityRequirement -- remove it from ENDPOINTS_WITHOUT_SECURITY_DECLARATION");
            }
        }

        assertTrue(stale.isEmpty(), () -> """
                A contract allow-list is out of date. Remove these entries:
                %s
                """.formatted(join(stale)));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Endpoint-specific contracts. These pin exact paths, which the generic rules above cannot.
    // -------------------------------------------------------------------------------------------------------------

    @Test
    void certificateSearchEndpointsAreAuthenticatedOk() {
        var searchEndpoints = RestEndpointInventory.endpoints()
                                                   .stream()
                                                   .filter(endpoint -> endpoint.api()
                                                                               .getSimpleName()
                                                                               .equals("CertificateAPI"))
                                                   .filter(endpoint -> endpoint.httpMethod()
                                                                               .equals("GET"))
                                                   .filter(endpoint -> endpoint.path()
                                                                               .contains("/management/"))
                                                   .toList();

        assertTrue(searchEndpoints.stream()
                                  .anyMatch(endpoint -> endpoint.path()
                                                                .endsWith("/certificate-names")),
                "CertificateAPI must expose a /management/ certificate-names search endpoint");
        assertTrue(searchEndpoints.stream()
                                  .anyMatch(endpoint -> endpoint.path()
                                                                .endsWith("/organizations")),
                "CertificateAPI must expose a /management/ organizations search endpoint");
        searchEndpoints.forEach(endpoint -> assertTrue(endpoint.method()
                                                               .isAnnotationPresent(SecurityRequirement.class),
                () -> endpoint.describe() + " must declare authentication"));
    }

    @Test
    void diveGroupEndpointsAreAuthenticatedAndCompleteOk() {
        var diveGroupEndpoints = RestEndpointInventory.endpoints()
                                                      .stream()
                                                      .filter(endpoint -> endpoint.api()
                                                                                  .getSimpleName()
                                                                                  .equals("DiveGroupAPI"))
                                                      .toList();

        assertEquals(11, diveGroupEndpoints.size(), "DiveGroupAPI must declare all eleven dive group endpoints");

        for (var endpoint : diveGroupEndpoints) {
            assertTrue(endpoint.path()
                               .startsWith("/api/dive-groups"),
                    () -> endpoint.describe() + " must be mapped below /api/dive-groups");
            assertTrue(endpoint.method()
                               .isAnnotationPresent(SecurityRequirement.class),
                    () -> endpoint.describe() + " must declare authentication");
            assertTrue(endpoint.method()
                               .isAnnotationPresent(ApiResponses.class),
                    () -> endpoint.describe() + " is missing OpenAPI @ApiResponses");
            assertFalse(endpoint.isPublicRoute(), () -> endpoint.describe() + " must not be classified as a public route");
        }
    }

    @Test
    void diveGroupEndpointsUseTheExpectedHttpMethods() {
        assertEquals("/api/dive-groups", mappedPath("DiveGroupAPI", "POST", "createDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}", mappedPath("DiveGroupAPI", "PUT", "updateDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}/details", mappedPath("DiveGroupAPI", "PUT", "updateDiveGroupDetails"));
        assertEquals("/api/dive-groups/{diveGroupId}", mappedPath("DiveGroupAPI", "DELETE", "deleteDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}", mappedPath("DiveGroupAPI", "GET", "getDiveGroupById"));
        assertEquals("/api/dive-groups/events/{eventId}", mappedPath("DiveGroupAPI", "GET", "getDiveGroupsByEventId"));
        assertEquals("/api/dive-groups/events/{eventId}/order", mappedPath("DiveGroupAPI", "PUT", "reorderDiveGroups"));
        assertEquals("/api/dive-groups/{diveGroupId}/members", mappedPath("DiveGroupAPI", "POST", "joinDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}/members", mappedPath("DiveGroupAPI", "DELETE", "leaveDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}/members/{userId}", mappedPath("DiveGroupAPI", "POST", "addMemberToDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}/members/{userId}", mappedPath("DiveGroupAPI", "DELETE", "removeMemberFromDiveGroup"));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------------------------------

    private static String mappedPath(String apiName, String httpMethod, String methodName) {
        return RestEndpointInventory.endpoints()
                                    .stream()
                                    .filter(endpoint -> endpoint.api()
                                                                .getSimpleName()
                                                                .equals(apiName))
                                    .filter(endpoint -> endpoint.method()
                                                                .getName()
                                                                .equals(methodName))
                                    .filter(endpoint -> endpoint.httpMethod()
                                                                .equals(httpMethod))
                                    .map(Endpoint::path)
                                    .findFirst()
                                    .orElseThrow(() -> new AssertionError(
                                            apiName + "." + methodName + " is not mapped with " + httpMethod));
    }

    private static java.util.List<String> offenders(Predicate<Endpoint> isOffender) {
        return RestEndpointInventory.endpoints()
                                    .stream()
                                    .filter(isOffender)
                                    .map(Endpoint::describe)
                                    .sorted()
                                    .toList();
    }

    private static Set<String> allAllowListEntries() {
        var all = new TreeSet<String>();
        all.addAll(ENDPOINTS_WITHOUT_DECLARED_RESPONSES);
        all.addAll(ENDPOINTS_WITHOUT_DECLARED_ERROR_STATUS);
        all.addAll(ENDPOINTS_WITHOUT_SECURITY_DECLARATION);
        return all;
    }

    private static String join(Collection<String> lines) {
        return lines.stream()
                    .map(line -> "  - " + line)
                    .collect(Collectors.joining("\n"));
    }
}
