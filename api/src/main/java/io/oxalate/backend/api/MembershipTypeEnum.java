package io.oxalate.backend.api;

public enum MembershipTypeEnum {
    DISABLED("disabled"),
    PERPETUAL("perpetual"),
    PERIODICAL("periodical"),
    DURATIONAL("durational");

    public final String type;

    MembershipTypeEnum(String type) {
        this.type = type;
    }

    public static MembershipTypeEnum fromString(String type) {
        for (MembershipTypeEnum membershipType : MembershipTypeEnum.values()) {
            if (membershipType.type.equalsIgnoreCase(type)) {
                return membershipType;
            }
        }
        return null;
    }
}
