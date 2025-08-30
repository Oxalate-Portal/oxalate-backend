package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Signup request")
public class SignupRequest {
    @NotBlank
    @Size(max = 80)
    @Email
    @Schema(description = "Username/email of the user logging in", example = "someone@somewhere.tld", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("username")
    private String username;

    @NotBlank
    @Size(min = 6, max = 40)
    @Schema(description = "Password of the user logging in", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("password")
    private String password;

    @NotBlank
    @Size(max = 120)
    @Schema(description = "First name of the user", example = "Erkki", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("firstName")
    private String firstName;

    @NotBlank
    @Size(max = 120)
    @Schema(description = "Last name of the user", example = "Toivonen", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("lastName")
    private String lastName;

    @Size(max = 255)
    @Schema(description = "Phone number, should not contain the + prefix", example = "358403214321", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @Schema(description = "Boolean whether the user wants to keep their username and phone number private", example = "yes", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("privacy")
    private boolean privacy;

    @Size(max = 255)
    @Schema(description = "Next of kin information, usually name and phone number", example = "Jaana Toivonen +358404325432", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("nextOfKin")
    private String nextOfKin;

    @Schema(description = "Boolean whether the user has accepted the terms and conditions", example = "yes", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("approvedTerms")
    private boolean approvedTerms;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @Schema(description = "Preferred language", example = "en", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("language")
    private String language;

    @NotNull
    @NotBlank
    @Schema(description = "Primary type of user", example = "CCR_DIVER", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("primaryUserType")
    private UserTypeEnum primaryUserType;
}
