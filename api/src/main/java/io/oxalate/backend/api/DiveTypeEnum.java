package io.oxalate.backend.api;

public enum DiveTypeEnum {
    BOAT("boat"),
    CAVE("cave"),
    COURSE("course"),
    CURRENT("current"),
    OPEN_AND_CAVE("open-and-cave"),
    OPEN_WATER("open-water"),
    SURFACE("surface");

    public final String typeName;

    private DiveTypeEnum(String typeName) {
        this.typeName = typeName;
    }
}
