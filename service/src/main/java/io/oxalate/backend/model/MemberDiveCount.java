package io.oxalate.backend.model;

import io.oxalate.backend.api.response.download.DownloadDiveResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
public class MemberDiveCount {
    @Id
    @Column(name = "user_id")
    private final long userId;

    @Column(name = "first_name")
    private final String firstName;

    @Column(name = "last_name")
    private final String lastName;

    @Column(name = "dive_count")
    private final int diveCount;

    public MemberDiveCount(long userId, String firstName, String lastName, int diveCount) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.diveCount = diveCount;
    }

    public DownloadDiveResponse toDownloadDiveResponse() {
        return DownloadDiveResponse.builder()
                .id(userId)
                .name(firstName + " " + lastName)
                .diveCount(diveCount)
                .build();
    }
}
