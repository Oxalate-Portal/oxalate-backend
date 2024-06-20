package io.oxalate.backend.model;

import io.oxalate.backend.api.response.download.DownloadDiveResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
public class MemberDiveCount {
    @Id
    @Column(name = "user_id")
    private long userId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "dive_count")
    private int diveCount;

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
