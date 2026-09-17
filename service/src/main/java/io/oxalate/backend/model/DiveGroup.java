package io.oxalate.backend.model;

import io.oxalate.backend.api.DiveGroupTypeEnum;
import io.oxalate.backend.api.response.DiveGroupMemberResponse;
import io.oxalate.backend.api.response.DiveGroupResponse;
import io.oxalate.backend.api.response.filetransfer.DiveFileResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /**
     * Free-text description of the group, for example the dive plan or the equipment the members bring along. The
     * maximum length is governed by the {@code frontend.dive-group-description-max-length} portal configuration.
     */
    @Column(name = "description")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private long ownerId;

    /**
     * Whether the group performs a normal dive or a special, project dive.
     */
    @Builder.Default
    @Column(name = "group_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DiveGroupTypeEnum groupType = DiveGroupTypeEnum.NORMAL;

    /**
     * Position of the group within the dive event, starting from 1. The default order is the order of creation.
     */
    @Column(name = "group_order", nullable = false)
    private int groupOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Converts this DiveGroup to a DiveGroupResponse.
     *
     * @param ownerName full name of the owner of the group
     * @param members   current members of the group
     * @param diveFiles dive files uploaded by the members of the group
     * @return the populated DiveGroupResponse
     */
    public DiveGroupResponse toDiveGroupResponse(String ownerName, List<DiveGroupMemberResponse> members, List<DiveFileResponse> diveFiles) {
        return DiveGroupResponse.builder()
                                .id(this.id)
                                .eventId(this.eventId)
                                .name(this.name)
                                .description(this.description)
                                .ownerId(this.ownerId)
                                .ownerName(ownerName)
                                .groupType(this.groupType)
                                .groupOrder(this.groupOrder)
                                .createdAt(this.createdAt)
                                .updatedAt(this.updatedAt)
                                .members(members)
                                .diveFiles(diveFiles)
                                .build();
    }
}
