package io.oxalate.backend.api;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DiveTypeEnum {
    BOAT("boat"),
    CAVE("cave"),
    COURSE("course"),
    CURRENT("current"),
    OPEN_AND_CAVE("open-and-cave"),
    OPEN_WATER("open-water"),
    SURFACE("surface");

    public final String typeName;

    DiveTypeEnum(String typeName) {
        this.typeName = typeName;
    }

    @JsonValue
    public String getTypeName() {
        return typeName;
    }
}
