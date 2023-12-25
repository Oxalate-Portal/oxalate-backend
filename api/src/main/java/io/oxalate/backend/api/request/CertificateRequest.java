package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Schema(description = "Certificate request")
@Data
@AllArgsConstructor
public class CertificateRequest {
    @Schema(description = "ID of the certificate entity", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("id")
    private long id;

    @Schema(description = "Name of the diving organization", example = "IANTD", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("organization")
    private String organization;

    @Schema(description = "Which certificate it is", example = "Rebreather full cave diver", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("certificateName")
    private String certificateName;

    @Schema(description = "Certificate identifier, either this or diver ID must be filled", example = "123456", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("certificateId")
    private String certificateId;

    @Schema(description = "Diver identifier, either this or certificate ID must be filled", example = "123456", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("diverId")
    private String diverId;

    @Schema(description = "Certification date in yyyy-mm-dd format", example = "2012-06-21", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("certificationDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Timestamp certificationDate;
}
