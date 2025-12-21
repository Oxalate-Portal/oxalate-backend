package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UpdateStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionResponse {
    @JsonProperty("status")
    private UpdateStatusEnum status;
    @JsonProperty("message")
    private String message;
}
