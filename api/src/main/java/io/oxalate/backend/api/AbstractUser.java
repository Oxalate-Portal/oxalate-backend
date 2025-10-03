package io.oxalate.backend.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractUser {

    @Schema(description = "ID of the user to be updated", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    private long id;

    @Size(max = 80)
    @Email
    @Schema(description = "Username/email of the user", example = "someone@somewhere.tld", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("username")
    private String username;

    @Size(max = 120)
    @Schema(description = "First name of the user", example = "Erkki", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("firstName")
    private String firstName;

    @Size(max = 120)
    @Schema(description = "Last name of the user", example = "Toivonen", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("lastName")
    private String lastName;

    @Size(max = 255)
    @Schema(description = "Phone number, should not contain the + prefix", example = "358403214321", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @Schema(description = "Datetime of the registration of user account", example = "2023-01-15T13:45:30Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("registered")
    private Instant registered;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @Schema(description = "Preferred language", example = "en", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("language")
    private String language;

    @Schema(description = "Status of the user", example = "ANONYMIZED", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("status")
    private UserStatusEnum status;

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

    @Schema(description = "Primary user type", example = "FREE_DIVER", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("primaryUserType")
    private UserTypeEnum primaryUserType;
}
