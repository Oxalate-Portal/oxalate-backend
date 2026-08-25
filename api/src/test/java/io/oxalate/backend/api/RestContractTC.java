package io.oxalate.backend.api;

import io.oxalate.backend.rest.AuditAPI;
import io.oxalate.backend.rest.AuthAPI;
import io.oxalate.backend.rest.BlockedDateAPI;
import io.oxalate.backend.rest.CertificateAPI;
import io.oxalate.backend.rest.CommentAPI;
import io.oxalate.backend.rest.DataDownloadAPI;
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
import io.oxalate.backend.rest.UserAPI;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provider-neutral contract checks for the public REST API. These checks run in
 * the API module, so a contract regression is caught without a database or Docker.
 */
class RestContractTC {

    private static final Class<?>[] API_INTERFACES = {
            AuditAPI.class, AuthAPI.class, BlockedDateAPI.class, CertificateAPI.class,
            CommentAPI.class, DataDownloadAPI.class, EmailNotificationSubscriptionAPI.class,
            EventAPI.class, FileTransferAPI.class, MembershipAPI.class, NotificationAPI.class,
            PageAPI.class, PageManagementAPI.class, PaymentAPI.class,
            PortalConfigurationAPI.class, StatsAPI.class, TagAPI.class, TestAPI.class, UserAPI.class
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
                        || endpointPath.equals("/api/configurations/frontend");
                if (explicitlyPublic) {
                    publicEndpointCount++;
                }
            }
        }
        assertTrue(publicEndpointCount > 0, "The contract must contain at least one public endpoint");
    }

    private static Object mapping(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return method.getAnnotation(GetMapping.class);
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
