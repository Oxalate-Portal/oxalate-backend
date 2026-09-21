package io.oxalate.backend.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public abstract class AbstractUser {

    @Schema(description = "ID of the user to be updated", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private long id;

    @Size(max = 80)
    @Email
    @Schema(description = "Username/email of the user", example = "someone@somewhere.tld", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Size(max = 120)
    @Schema(description = "First name of the user", example = "Erkki", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Size(max = 120)
    @Schema(description = "Last name of the user", example = "Toivonen", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Size(max = 1024)
    @Schema(description = "Avatar URL of the user. Null when no avatar exists.", example = "http://localhost:8080/api/files/avatars/123",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String avatarUrl;

    @Size(max = 255)
    @Schema(description = "Phone number, should not contain the + prefix", example = "358403214321", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @Schema(description = "Datetime of the registration of user account", example = "2023-01-15T13:45:30Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant registered;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @Schema(description = "Preferred language", example = "en", requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @Schema(description = "Status of the user", example = "ANONYMIZED", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserStatusEnum status;

    @Schema(description = "Boolean whether the user wants to keep their username and phone number private", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean privacy;

    @Size(max = 255)
    @Schema(description = "Next of kin information, usually name and phone number", example = "Jaana Toivonen +358404325432", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nextOfKin;

    @Schema(description = "Boolean whether the user has accepted the terms and conditions", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean approvedTerms;

    @Schema(description = """
            Document ID if the user has uploaded the health statement document. If the value is 0, then the user has confirmed the health statement.
            If the value is null, then the user has not agreed to the health statement.
            """,
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long healthStatementId;

    @Schema(description = "Primary user type", example = "FREE_DIVER", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserTypeEnum primaryUserType;

    @Schema(description = "Most advanced certificate classification title in the requester's language")
    private String certificateClassificationTitle;
}
