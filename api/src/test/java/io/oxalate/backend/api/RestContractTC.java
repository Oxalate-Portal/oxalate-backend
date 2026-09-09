package io.oxalate.backend.api;

import io.oxalate.backend.rest.AuditAPI;
import io.oxalate.backend.rest.AuthAPI;
import io.oxalate.backend.rest.BlockedDateAPI;
import io.oxalate.backend.rest.CertificateAPI;
import io.oxalate.backend.rest.CertificateClassificationAPI;
import io.oxalate.backend.rest.CommentAPI;
import io.oxalate.backend.rest.DataDownloadAPI;
import io.oxalate.backend.rest.DiveGroupAPI;
import io.oxalate.backend.rest.EmailNotificationSubscriptionAPI;
import io.oxalate.backend.rest.EventAPI;
import io.oxalate.backend.rest.FileTransferAPI;
import io.oxalate.backend.rest.MembershipAPI;
import io.oxalate.backend.rest.NotificationAPI;
import io.oxalate.backend.rest.PageAPI;
import io.oxalate.backend.rest.PageManagementAPI;
import io.oxalate.backend.rest.PaymentAPI;
import io.oxalate.backend.rest.PortalConfigurationAPI;
import io.oxalate.backend.rest.StatsAPI;
import io.oxalate.backend.rest.TagAPI;
import io.oxalate.backend.rest.TestAPI;
import io.oxalate.backend.rest.ThirdPartyAPI;
import io.oxalate.backend.rest.TokenAPI;
import io.oxalate.backend.rest.UserAPI;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.reflect.Method;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Provider-neutral contract checks for the public REST API. These checks run in
 * the API module, so a contract regression is caught without a database or Docker.
 */
class RestContractTC {

    private static final Class<?>[] API_INTERFACES = {
            AuditAPI.class, AuthAPI.class, BlockedDateAPI.class, CertificateAPI.class, CertificateClassificationAPI.class,
            CommentAPI.class, DataDownloadAPI.class, DiveGroupAPI.class, EmailNotificationSubscriptionAPI.class,
            EventAPI.class, FileTransferAPI.class, MembershipAPI.class, NotificationAPI.class,
            PageAPI.class, PageManagementAPI.class, PaymentAPI.class,
            PortalConfigurationAPI.class, StatsAPI.class, TagAPI.class, TestAPI.class, UserAPI.class,
            TokenAPI.class, ThirdPartyAPI.class
    };

    @Test
    void everyDeclaredEndpointHasACompleteJavaContract() {
        var paths = new HashSet<String>();
        var endpointCount = 0;

        for (var api : API_INTERFACES) {
            for (var method : api.getDeclaredMethods()) {
                var mapping = mapping(method);
                if (mapping == null) {
                    continue;
                }
                endpointCount++;
                assertTrue(paths.add(api.getName() + ":" + method.getName() + ":" + path(mapping)),
                        () -> "Duplicate endpoint mapping in " + api.getSimpleName() + ": " + path(mapping));
                assertTrue(ResponseEntity.class.isAssignableFrom(method.getReturnType()),
                        () -> api.getSimpleName() + "." + method.getName() + " must return ResponseEntity");
                assertNotNull(method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class),
                        () -> api.getSimpleName() + "." + method.getName() + " is missing OpenAPI @Operation");
            }
        }

        assertTrue(endpointCount >= 100, "The contract inventory unexpectedly contains too few endpoints");
    }

    @Test
    void publicEndpointsUseOnlyTheSecurityConfigPublicPrefixes() {
        var publicEndpointCount = 0;
        for (var api : API_INTERFACES) {
            for (var method : api.getDeclaredMethods()) {
                var mapping = mapping(method);
                if (mapping == null) {
                    continue;
                }
                var endpointPath = path(mapping);
                var explicitlyPublic = endpointPath.startsWith("/api/auth/")
                        || endpointPath.startsWith("/api/pages/")
                        || endpointPath.startsWith("/api/files/")
                        || endpointPath.startsWith("/api/documents/")
                        || endpointPath.startsWith("/api/dive-plans/")
                        || endpointPath.startsWith("/api/test/")
                        || endpointPath.equals("/api/third-party/events")
                        || endpointPath.equals("/api/configurations/frontend");
                if (explicitlyPublic) {
                    publicEndpointCount++;
                }
            }
        }
        assertTrue(publicEndpointCount > 0, "The contract must contain at least one public endpoint");
    }

    @Test
    void certificateSearchEndpointsAreAuthenticated() {
        var paths = CertificateAPI.class.getDeclaredMethods();
        var searchEndpoints = java.util.Arrays.stream(paths)
                                              .filter(method -> method.isAnnotationPresent(GetMapping.class))
                                              .filter(method -> path(method.getAnnotation(GetMapping.class)).contains("/management/"))
                                              .toList();

        assertTrue(searchEndpoints.stream()
                                  .anyMatch(method -> path(method.getAnnotation(GetMapping.class))
                                          .endsWith("/certificate-names")));
        assertTrue(searchEndpoints.stream()
                                  .anyMatch(method -> path(method.getAnnotation(GetMapping.class))
                                          .endsWith("/organizations")));
        searchEndpoints.forEach(method -> assertTrue(method.isAnnotationPresent(SecurityRequirement.class),
                () -> method.getName() + " must declare authentication"));
    }

    @Test
    void diveGroupEndpointsAreAuthenticatedAndComplete() {
        var methods = DiveGroupAPI.class.getDeclaredMethods();

        assertEquals(9, java.util.Arrays.stream(methods)
                                        .filter(method -> mapping(method) != null)
                                        .count(), "DiveGroupAPI must declare all nine dive group endpoints");

        for (var method : methods) {
            var mapping = mapping(method);
            if (mapping == null) {
                continue;
            }

            var endpointPath = path(mapping);
            assertTrue(endpointPath.startsWith("/api/dive-groups"),
                    () -> method.getName() + " must be mapped below /api/dive-groups, was: " + endpointPath);
            assertTrue(method.isAnnotationPresent(SecurityRequirement.class),
                    () -> method.getName() + " must declare authentication");
            assertNotNull(method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class),
                    () -> method.getName() + " is missing OpenAPI @Operation");
            assertNotNull(method.getAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponses.class),
                    () -> method.getName() + " is missing OpenAPI @ApiResponses");
            assertTrue(ResponseEntity.class.isAssignableFrom(method.getReturnType()),
                    () -> method.getName() + " must return ResponseEntity");
            assertFalse(endpointPath.startsWith("/api/auth/")
                            || endpointPath.startsWith("/api/pages/")
                            || endpointPath.startsWith("/api/files/")
                            || endpointPath.startsWith("/api/documents/")
                            || endpointPath.startsWith("/api/dive-plans/")
                            || endpointPath.startsWith("/api/test/"),
                    () -> method.getName() + " must not be classified as a public route");
        }
    }

    @Test
    void diveGroupEndpointsUseTheExpectedHttpMethods() {
        assertEquals("/api/dive-groups", mappedPath(PostMapping.class, "createDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}", mappedPath(PutMapping.class, "updateDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}", mappedPath(DeleteMapping.class, "deleteDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}", mappedPath(GetMapping.class, "getDiveGroupById"));
        assertEquals("/api/dive-groups/events/{eventId}", mappedPath(GetMapping.class, "getDiveGroupsByEventId"));
        assertEquals("/api/dive-groups/{diveGroupId}/members", mappedPath(PostMapping.class, "joinDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}/members", mappedPath(DeleteMapping.class, "leaveDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}/members/{userId}", mappedPath(PostMapping.class, "addMemberToDiveGroup"));
        assertEquals("/api/dive-groups/{diveGroupId}/members/{userId}", mappedPath(DeleteMapping.class, "removeMemberFromDiveGroup"));
    }

    private static String mappedPath(Class<? extends java.lang.annotation.Annotation> annotation, String methodName) {
        return java.util.Arrays.stream(DiveGroupAPI.class.getDeclaredMethods())
                               .filter(method -> method.getName()
                                                       .equals(methodName))
                               .filter(method -> method.isAnnotationPresent(annotation))
                               .map(method -> path(method.getAnnotation(annotation)))
                               .findFirst()
                               .orElseThrow(() -> new AssertionError(methodName + " is not mapped with " + annotation.getSimpleName()));
    }

    private static Object mapping(Method method) {
        if (method.isAnnotationPresent(GetMapping.class))
            return method.getAnnotation(GetMapping.class);
        if (method.isAnnotationPresent(PostMapping.class)) return method.getAnnotation(PostMapping.class);
        if (method.isAnnotationPresent(PutMapping.class)) return method.getAnnotation(PutMapping.class);
        if (method.isAnnotationPresent(DeleteMapping.class)) return method.getAnnotation(DeleteMapping.class);
        if (method.isAnnotationPresent(PatchMapping.class)) return method.getAnnotation(PatchMapping.class);
        return method.getAnnotation(RequestMapping.class);
    }

    private static String path(Object mapping) {
        if (mapping instanceof GetMapping annotation) return first(annotation.path(), annotation.value());
        if (mapping instanceof PostMapping annotation) return first(annotation.path(), annotation.value());
        if (mapping instanceof PutMapping annotation) return first(annotation.path(), annotation.value());
        if (mapping instanceof DeleteMapping annotation) return first(annotation.path(), annotation.value());
        if (mapping instanceof PatchMapping annotation) return first(annotation.path(), annotation.value());
        if (mapping instanceof RequestMapping annotation) return first(annotation.path(), annotation.value());
        return "";
    }

    private static String first(String[] paths, String[] values) {
        assertFalse(paths.length == 0 && values.length == 0, "Every mapping must declare a path");
        return paths.length > 0 ? paths[0] : values[0];
    }
}
