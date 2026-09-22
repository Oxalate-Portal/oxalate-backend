package io.oxalate.backend.api.response;

import io.oxalate.backend.api.AbstractEvent;
import io.oxalate.backend.api.EventStatusEnum;
import java.util.List;
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
public class EventResponse extends AbstractEvent {

    private EventStatusEnum status;

    private UserResponse organizer;

    private List<ListUserResponse> participants;

    private List<ListUserResponse> waitingList;

    private long eventCommentId;

    private Set<TagResponse> tags;
}
