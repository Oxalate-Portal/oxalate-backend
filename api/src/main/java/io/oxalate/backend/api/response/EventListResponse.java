package io.oxalate.backend.api.response;

import io.oxalate.backend.api.AbstractEvent;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventListResponse extends AbstractEvent {

    private String organizerName;

    private int participantCount;

    private int waitingListCount;

    private long eventCommentId;

    private Set<TagResponse> tags;
}
