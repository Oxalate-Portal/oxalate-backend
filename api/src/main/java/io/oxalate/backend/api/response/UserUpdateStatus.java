package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UpdateStatusEnum;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserUpdateStatus {
    @JsonProperty("status")
    private UpdateStatusEnum status;
    @JsonProperty("message")
    private String message;
}
