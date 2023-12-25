package io.oxalate.backend.api.response;

import io.oxalate.backend.api.request.EventDiveListRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Schema(description = "Event dive list response")
public class EventDiveListResponse extends EventDiveListRequest {
    public EventDiveListResponse() {
        super();
    }
}
