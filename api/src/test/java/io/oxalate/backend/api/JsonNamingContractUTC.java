package io.oxalate.backend.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Wire naming contract shared with {@code oxalate-frontend}: every JSON field name is snake_case. The DTO classes are
 * discovered from the classpath so that a new DTO without the class-level naming strategy, or a field whose explicit
 * {@link JsonProperty} value reintroduces camelCase, fails the build instead of quietly breaking the frontend.
 */
@DisplayName("JSON naming contract: every DTO serializes with snake_case field names")
class JsonNamingContractUTC {

    private static final String API_PACKAGE = "io.oxalate.backend.api";
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");

    @Test
    void everyDtoDeclaresSnakeCaseNamingOk() {
        var missing = new ArrayList<String>();

        for (var dtoClass : dtoClasses()) {
            if (!declaresSnakeCaseNaming(dtoClass)) {
                missing.add(dtoClass.getName());
            }
        }

        assertTrue(missing.isEmpty(), "DTO classes without @JsonNaming(SnakeCaseStrategy) on the class or a superclass: " + missing);
    }

    @Test
    void noJsonPropertyValueContainsUppercaseOk() {
        var offending = new ArrayList<String>();

        for (var dtoClass : dtoClasses()) {
            for (Field field : dtoClass.getDeclaredFields()) {
                var jsonProperty = field.getAnnotation(JsonProperty.class);

                if (jsonProperty != null && UPPERCASE.matcher(jsonProperty.value())
                                                     .find()) {
                    offending.add(dtoClass.getSimpleName() + "." + field.getName() + " -> " + jsonProperty.value());
                }
            }
        }

        assertTrue(offending.isEmpty(), "@JsonProperty values that are not snake_case: " + offending);
    }

    @Test
    void scanFindsTheKnownDtosOk() {
        var names = dtoClasses().stream()
                                .map(Class::getName)
                                .toList();

        assertTrue(names.contains("io.oxalate.backend.api.request.PagedRequest"), "PagedRequest must be scanned");
        assertTrue(names.contains("io.oxalate.backend.api.response.PagedResponse"), "PagedResponse must be scanned");
        assertTrue(names.contains("io.oxalate.backend.api.AbstractUser"), "abstract bases must be scanned");
        assertTrue(names.contains("io.oxalate.backend.api.response.UploadErrorResponse$UploadErrorMessage"), "nested DTOs must be scanned");
        assertFalse(names.contains("io.oxalate.backend.api.RoleEnum"), "enums are not DTOs");
        assertTrue(names.size() >= 100, "Expected at least 100 DTO classes, found " + names.size());
    }

    private static boolean declaresSnakeCaseNaming(Class<?> dtoClass) {
        for (Class<?> current = dtoClass; current != null && current != Object.class; current = current.getSuperclass()) {
            var naming = current.getAnnotation(JsonNaming.class);

            if (naming != null) {
                return PropertyNamingStrategies.SnakeCaseStrategy.class.equals(naming.value());
            }
        }

        return false;
    }

    /**
     * Every non-enum, non-interface class under the API package with at least one non-static field or a {@link Schema}
     * annotation, including nested DTOs but not the Lombok generated builders.
     */
    private static List<Class<?>> dtoClasses() {
        var result = new ArrayList<Class<?>>();

        try (ScanResult scanResult = new ClassGraph().acceptPackages(API_PACKAGE)
                                                     .enableClassInfo()
                                                     .scan()) {
            for (var classInfo : scanResult.getAllClasses()) {
                if (classInfo.isEnum() || classInfo.isInterface() || classInfo.isAnnotation() || classInfo.isRecord() || isLombokBuilder(classInfo)) {
                    continue;
                }

                var dtoClass = classInfo.loadClass();

                if (hasInstanceField(dtoClass) || dtoClass.isAnnotationPresent(Schema.class)) {
                    result.add(dtoClass);
                }
            }
        }

        return result;
    }

    /**
     * Lombok generates a nested {@code XBuilder} class for every {@code @Builder}/{@code @SuperBuilder} DTO. Builders never
     * cross the wire, so they are not part of the contract.
     */
    private static boolean isLombokBuilder(ClassInfo classInfo) {
        return classInfo.isInnerClass() && classInfo.getSimpleName()
                                                    .endsWith("Builder");
    }

    private static boolean hasInstanceField(Class<?> dtoClass) {
        for (Field field : dtoClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                return true;
            }
        }

        return false;
    }
}
