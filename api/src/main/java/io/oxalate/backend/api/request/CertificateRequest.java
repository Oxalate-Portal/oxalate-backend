package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@Schema(description = "Certificate request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CertificateRequest {
    @Schema(description = "ID of the certificate entity", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private long id;

    @Schema(description = "Name of the diving organization", example = "IANTD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String organization;

    @Schema(description = "Which certificate it is", example = "Rebreather full cave diver", requiredMode = Schema.RequiredMode.REQUIRED)
    private String certificateName;

    @Schema(description = "Certificate identifier, either this or diver ID must be filled", example = "123456", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String certificateId;

    @Schema(description = "Diver identifier, either this or certificate ID must be filled", example = "123456", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String diverId;

    @Schema(description = "Certification date in yyyy-mm-dd format", example = "2012-06-21", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate certificationDate;

    private Long classificationId;
}
