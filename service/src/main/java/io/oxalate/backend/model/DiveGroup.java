package io.oxalate.backend.model;

import io.oxalate.backend.api.response.DiveGroupMemberResponse;
import io.oxalate.backend.api.response.DiveGroupResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A dive group groups together participants of a single dive event.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dive_groups")
public class DiveGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "event_id", nullable = false)
    private long eventId;

    @Size(min = 1, max = 255, message = "Dive group name must be between 1 and 255 characters long")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private long ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Converts this DiveGroup to a DiveGroupResponse.
     *
     * @param ownerName full name of the owner of the group
     * @param members   current members of the group
     * @return the populated DiveGroupResponse
     */
    public DiveGroupResponse toDiveGroupResponse(String ownerName, List<DiveGroupMemberResponse> members) {
        return DiveGroupResponse.builder()
                                .id(this.id)
                                .eventId(this.eventId)
                                .name(this.name)
                                .ownerId(this.ownerId)
                                .ownerName(ownerName)
                                .createdAt(this.createdAt)
                                .updatedAt(this.updatedAt)
                                .members(members)
                                .build();
    }
}
