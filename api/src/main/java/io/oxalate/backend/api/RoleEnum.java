package io.oxalate.backend.api;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum RoleEnum {
    ROLE_ANONYMOUS,
    ROLE_USER,
    ROLE_ORGANIZER,
    ROLE_ADMIN;

    private static final Map<String, RoleEnum> stringToERole = Arrays.stream(RoleEnum.values())
            .collect(Collectors.toMap(RoleEnum::toString, e -> e));

    public static RoleEnum fromString(String name) {
        return stringToERole.get(name);
    }
}
