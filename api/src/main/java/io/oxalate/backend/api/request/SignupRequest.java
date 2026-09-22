package io.oxalate.backend.api.request;

import io.oxalate.backend.api.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Signup request")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SignupRequest {
    @NotBlank
    @Size(max = 80)
    @Email
    @Schema(description = "Username/email of the user logging in", example = "someone@somewhere.tld", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank
    @Size(min = 6, max = 40)
    @Schema(description = "Password of the user logging in", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank
    @Size(max = 120)
    @Schema(description = "First name of the user", example = "Erkki", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank
    @Size(max = 120)
    @Schema(description = "Last name of the user", example = "Toivonen", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Size(max = 255)
    @Schema(description = "Phone number, should not contain the + prefix", example = "358403214321", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @Schema(description = "Boolean whether the user wants to keep their username and phone number private", example = "yes", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean privacy;

    @Size(max = 255)
    @Schema(description = "Next of kin information, usually name and phone number", example = "Jaana Toivonen +358404325432", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nextOfKin;

    @Schema(description = "Boolean whether the user has accepted the terms and conditions", example = "yes", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean approvedTerms;

    @Schema(description = "Boolean whether the user has confirmed their health check", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long healthStatementId;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @Schema(description = "Preferred language", example = "en", requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @Schema(description = "Primary type of user", example = "CCR_DIVER", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserTypeEnum primaryUserType;
}
