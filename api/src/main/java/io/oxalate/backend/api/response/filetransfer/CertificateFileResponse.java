package io.oxalate.backend.api.response.filetransfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class CertificateFileResponse extends AbstractFileResponse {
    @JsonProperty("certificateId")
    private long certificateId;
}
