package io.oxalate.backend.api.response.filetransfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UploadStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class PageFileResponse extends AbstractFileResponse {
    @JsonProperty("pageId")
    private long pageId;

    @JsonProperty("language")
    private String language;

    @JsonProperty("status")
    private UploadStatusEnum status;
}
