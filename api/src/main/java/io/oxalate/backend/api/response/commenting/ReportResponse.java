package io.oxalate.backend.api.response.commenting;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UpdateStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Report response")
@Builder
@Data
public class ReportResponse {
    @JsonProperty("status")
    private UpdateStatusEnum status;

    @JsonProperty("errorCode")
    private long errorCode;

    @JsonProperty("errorMessage")
    private String errorMessage;
}
