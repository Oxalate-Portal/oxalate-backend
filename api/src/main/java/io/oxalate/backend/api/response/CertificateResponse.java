package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {
    @JsonProperty("id")
    private long id;
    @JsonProperty("userId")
    private long userId;
    @JsonProperty("organization")
    private String organization;
    @JsonProperty("certificateName")
    private String certificateName;
    @JsonProperty("certificateId")
    private String certificateId;
    @JsonProperty("diverId")
    private String diverId;
    @JsonProperty("certificationDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    private Instant certificationDate;
    @JsonProperty("certificatePhotoUrl")
    private String certificatePhotoUrl;
}
