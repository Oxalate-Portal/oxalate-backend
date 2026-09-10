package io.oxalate.backend.security;

import io.oxalate.backend.controller.UserController;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OWASP A01:2025 - Broken Access Control.
 * <p>
 * The single most valuable regression test in this suite. It walks every {@code @RestController} in the
 * application and asserts that each mapped endpoint either sits on the deliberately public allow-list or
 * declares a {@link PreAuthorize} rule.
 * <p>
 * This exists because {@code MembershipController} shipped with no authorization at all: every endpoint,
 * including membership creation, was reachable by any authenticated account, which allowed a plain member to
 * grant themselves an active membership. Nothing in the codebase would have failed because of it. A missing
 * annotation on a new controller now breaks the build instead.
 * <p>
 * The test runs without a Spring context or a database, so it is cheap enough to never be skipped.
 *
 * @see UserController for the reference pattern of role rule plus ownership check
 */
@DisplayName("OWASP A01: every endpoint declares an authorization rule")
class OwaspEndpointAuthorizationUTC {

    private static final String CONTROLLER_PACKAGE = "io.oxalate.backend.controller";

    /**
     * Path prefixes that {@code WebSecurityConfig} exposes without authentication. Keep this list in sync with
     * the {@code permitAll()} matchers; anything else must carry a {@code @PreAuthorize}.
     */
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/auth/",
            "/api/pages/",
            "/api/files/",
            "/api/documents/",
            "/api/dive-plans/",
            "/api/test/"
    );

    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/api/third-party/events",
            "/api/configurations/frontend"
    );

    @Test
    void everyNonPublicEndpointDeclaresPreAuthorizeOk() {
        var unprotected = new ArrayList<String>();
        var checkedEndpoints = 0;

        for (var controller : findControllers()) {
            for (var method : controller.getMethods()) {
                var mapping = findMapping(controller, method);

                if (mapping == null) {
                    continue;
                }

                var path = firstPath(mapping);

                if (isPublic(path)) {
                    continue;
                }

                checkedEndpoints++;

                if (!AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
                    unprotected.add(controller.getSimpleName() + "." + method.getName() + " -> " + path);
                }
            }
        }

        assertTrue(checkedEndpoints > 50, "The controller scan found too few endpoints to be trustworthy, found " + checkedEndpoints);
        assertTrue(unprotected.isEmpty(), """
                The following endpoints are reachable by any authenticated user because they declare no @PreAuthorize rule.
                Add an explicit rule, or add the path to PUBLIC_PATH_PREFIXES if it is genuinely public:
                %s""".formatted(String.join("\n", unprotected)));
    }

    /**
     * Administrative surfaces must be restricted to administrators, never merely to "any logged in user".
     * {@code hasAnyRole('USER','ORGANIZER','ADMIN')} covers every role that exists and is therefore equivalent
     * to {@code isAuthenticated()}; it must not be used to guard privileged operations.
     */
    @Test
    void adminOnlyControllersRequireTheAdminRoleOk() {
        var adminOnlyControllers = List.of(
                "io.oxalate.backend.controller.MembershipController",
                "io.oxalate.backend.controller.AuditController",
                "io.oxalate.backend.controller.DataDownloadController"
        );

        var violations = new ArrayList<String>();

        for (var controllerName : adminOnlyControllers) {
            var controller = loadClass(controllerName);

            for (var method : controller.getMethods()) {
                if (findMapping(controller, method) == null) {
                    continue;
                }

                var preAuthorize = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);

                if (preAuthorize == null) {
                    violations.add(controller.getSimpleName() + "." + method.getName() + " has no @PreAuthorize");
                    continue;
                }

                if (!preAuthorize.value()
                                 .contains("ADMIN")) {
                    violations.add(controller.getSimpleName() + "." + method.getName() + " does not require ADMIN: " + preAuthorize.value());
                }
            }
        }

        assertTrue(violations.isEmpty(), "Administrative endpoints must require the ADMIN role: " + violations);
    }

    /**
     * Membership state decides whether a member may sign up for events, so it must never be self-serviceable.
     */
    @Test
    void membershipMutationsAreAdminOnlyOk() {
        var controller = loadClass("io.oxalate.backend.controller.MembershipController");

        for (var methodName : List.of("createMembership", "updateMembership", "getAllActiveMemberships", "getMembership")) {
            var preAuthorize = findPreAuthorize(controller, methodName);
            assertTrue(preAuthorize.contains("hasRole('ADMIN')"),
                    () -> "MembershipController." + methodName + " must be restricted to administrators, was: " + preAuthorize);
        }
    }

    /**
     * A role annotation alone cannot express "this record belongs to me". Endpoints that take a user id and are
     * open to ordinary members must additionally perform an ownership check in the method body, which is what
     * {@link UserController} does and what {@code MembershipController.getMembershipsForUser} now does too.
     */
    @Test
    void selfServiceEndpointsAreNotRoleOnlyOk() {
        var preAuthorize = findPreAuthorize(loadClass("io.oxalate.backend.controller.MembershipController"), "getMembershipsForUser");

        assertFalse(preAuthorize.contains("permitAll"), "getMembershipsForUser must require authentication");
        assertTrue(preAuthorize.contains("USER"), "getMembershipsForUser is a member facing endpoint");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String findPreAuthorize(Class<?> controller, String methodName) {
        for (var method : controller.getMethods()) {
            if (!method.getName()
                       .equals(methodName)) {
                continue;
            }

            var preAuthorize = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);

            if (preAuthorize != null) {
                return preAuthorize.value();
            }
        }

        throw new AssertionError(controller.getSimpleName() + "." + methodName + " is missing @PreAuthorize");
    }

    private List<Class<?>> findControllers() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        return scanner.findCandidateComponents(CONTROLLER_PACKAGE)
                      .stream()
                      .map(BeanDefinition::getBeanClassName)
                      .filter(Objects::nonNull)
                      .map(this::loadClass)
                      .toList();
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Could not load controller " + className, e);
        }
    }

    /**
     * The HTTP mappings live on the API interfaces in the api module, while {@code @PreAuthorize} lives on the
     * implementing controller method. This resolves the mapping through the interface hierarchy.
     */
    private RequestMapping findMapping(Class<?> controller, Method method) {
        var mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);

        if (mapping != null) {
            return mapping;
        }

        for (var api : controller.getInterfaces()) {
            try {
                var interfaceMethod = api.getMethod(method.getName(), method.getParameterTypes());
                var interfaceMapping = AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, RequestMapping.class);

                if (interfaceMapping != null) {
                    return interfaceMapping;
                }
            } catch (NoSuchMethodException e) {
                // Not part of this API interface, keep looking
            }
        }

        return null;
    }

    private String firstPath(RequestMapping mapping) {
        if (mapping.path().length > 0) {
            return mapping.path()[0];
        }

        if (mapping.value().length > 0) {
            return mapping.value()[0];
        }

        return "";
    }

    private boolean isPublic(String path) {
        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }

        return PUBLIC_PATH_PREFIXES.stream()
                                   .anyMatch(path::startsWith);
    }
}
