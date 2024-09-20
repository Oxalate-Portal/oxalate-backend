package io.oxalate.backend.api.response.filetransfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UploadStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class DiveFileResponse extends AbstractFileResponse {
    @JsonProperty("eventId")
    private long eventId;

    @JsonProperty("diveGroupId")
    private long diveGroupId;

    @JsonProperty("status")
    private UploadStatusEnum status;
}
