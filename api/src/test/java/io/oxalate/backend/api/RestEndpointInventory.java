package io.oxalate.backend.api;

import io.github.classgraph.ClassGraph;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Single source of truth for "what endpoints does this API declare", discovered from the classpath rather than
 * maintained by hand.
 * <p>
 * It lives in the api module's test sources and is published as a test-jar so that
 * {@code RestContractTC} (api) and {@code RestEndpointTestCoverageUTC} (service) agree on the inventory. Two
 * independent copies of this reflection logic would drift, and a contract gate that disagrees with itself is worse
 * than no gate.
 */
public final class RestEndpointInventory {

    public static final String API_PACKAGE = "io.oxalate.backend.rest";

    /**
     * Path prefixes {@code WebSecurityConfig} exposes without authentication.
     */
    public static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/auth/",
            "/api/pages/",
            "/api/files/",
            "/api/documents/",
            "/api/dive-plans/",
            "/api/test/"
    );

    public static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/api/third-party/events",
            "/api/configurations/frontend"
    );

    private static List<Class<?>> cachedInterfaces;
    private static List<Endpoint> cachedEndpoints;

    private RestEndpointInventory() {
        // Utility class.
    }

    /**
     * One declared REST endpoint.
     */
    public record Endpoint(Class<?> api, Method method, String httpMethod, String path) {

        /**
         * Stable identifier, for example {@code DiveGroupAPI.createDiveGroup}.
         */
        public String id() {
            return api.getSimpleName() + "." + method.getName();
        }

        public String describe() {
            return id() + " (" + httpMethod + " " + path + ")";
        }

        /**
         * The literal part of the path, up to the first path variable.
         */
        public String staticPathPrefix() {
            var brace = path.indexOf('{');
            return brace < 0 ? path : path.substring(0, brace);
        }

        /**
         * Numeric statuses declared through OpenAPI {@code @ApiResponses}, empty when none are declared.
         */
        public Set<Integer> declaredStatusCodes() {
            var annotation = method.getAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponses.class);
            if (annotation == null) {
                return Set.of();
            }

            var codes = new TreeSet<Integer>();
            for (var response : annotation.value()) {
                try {
                    codes.add(Integer.parseInt(response.responseCode()
                                                       .trim()));
                } catch (NumberFormatException ignored) {
                    // "default" and similar non-numeric codes carry no status contract.
                }
            }
            return codes;
        }

        public boolean isPublicRoute() {
            return PUBLIC_EXACT_PATHS.contains(path)
                    || PUBLIC_PATH_PREFIXES.stream()
                                           .anyMatch(path::startsWith);
        }
    }

    public static synchronized List<Class<?>> apiInterfaces() {
        if (cachedInterfaces == null) {
            try (var scan = new ClassGraph().enableClassInfo()
                                            .acceptPackages(API_PACKAGE)
                                            .scan()) {
                List<Class<?>> interfaces = new ArrayList<>(scan.getAllInterfaces()
                                                                .loadClasses());
                interfaces.sort(Comparator.comparing(Class::getSimpleName));
                cachedInterfaces = List.copyOf(interfaces);
            }
        }
        return cachedInterfaces;
    }

    public static synchronized List<Endpoint> endpoints() {
        if (cachedEndpoints != null) {
            return cachedEndpoints;
        }

        var endpoints = new ArrayList<Endpoint>();

        for (var api : apiInterfaces()) {
            for (var method : api.getDeclaredMethods()) {
                var mapping = mapping(method);
                if (mapping == null) {
                    continue;
                }
                endpoints.add(new Endpoint(api, method, httpMethodOf(mapping), path(mapping)));
            }
        }

        endpoints.sort(Comparator.comparing(Endpoint::describe));
        cachedEndpoints = List.copyOf(endpoints);
        return cachedEndpoints;
    }

    private static Annotation mapping(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return method.getAnnotation(GetMapping.class);
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return method.getAnnotation(PostMapping.class);
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return method.getAnnotation(PutMapping.class);
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return method.getAnnotation(DeleteMapping.class);
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            return method.getAnnotation(PatchMapping.class);
        }
        return method.getAnnotation(RequestMapping.class);
    }

    private static String httpMethodOf(Annotation mapping) {
        if (mapping instanceof GetMapping) {
            return "GET";
        }
        if (mapping instanceof PostMapping) {
            return "POST";
        }
        if (mapping instanceof PutMapping) {
            return "PUT";
        }
        if (mapping instanceof DeleteMapping) {
            return "DELETE";
        }
        if (mapping instanceof PatchMapping) {
            return "PATCH";
        }
        if (mapping instanceof RequestMapping annotation && annotation.method().length > 0) {
            return annotation.method()[0].name();
        }
        return "ANY";
    }

    private static String path(Annotation mapping) {
        if (mapping instanceof GetMapping annotation) {
            return first(annotation.path(), annotation.value());
        }
        if (mapping instanceof PostMapping annotation) {
            return first(annotation.path(), annotation.value());
        }
        if (mapping instanceof PutMapping annotation) {
            return first(annotation.path(), annotation.value());
        }
        if (mapping instanceof DeleteMapping annotation) {
            return first(annotation.path(), annotation.value());
        }
        if (mapping instanceof PatchMapping annotation) {
            return first(annotation.path(), annotation.value());
        }
        if (mapping instanceof RequestMapping annotation) {
            return first(annotation.path(), annotation.value());
        }
        return "";
    }

    private static String first(String[] paths, String[] values) {
        if (paths.length == 0 && values.length == 0) {
            throw new IllegalStateException("Every mapping must declare a path");
        }
        return paths.length > 0 ? paths[0] : values[0];
    }
}
