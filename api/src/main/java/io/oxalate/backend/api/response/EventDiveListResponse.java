package io.oxalate.backend.api.response;

import io.oxalate.backend.api.request.EventDiveListRequest;
import io.oxalate.backend.api.request.EventDiveRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@SuperBuilder
@Data
@NoArgsConstructor
@Schema(description = "Event dive list response")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventDiveListResponse extends EventDiveListRequest {
    // An explicit constructor is needed to call the super constructor, as Lombok's @Data does not generate one with arguments
    public EventDiveListResponse(Set<EventDiveRequest> dives) {
        super(dives);
    }
}
