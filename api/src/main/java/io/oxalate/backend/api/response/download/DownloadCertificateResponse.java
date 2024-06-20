package io.oxalate.backend.api.response.download;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.response.CertificateResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadCertificateResponse extends CertificateResponse {
    @JsonProperty("memberName")
    private String memberName;
}
