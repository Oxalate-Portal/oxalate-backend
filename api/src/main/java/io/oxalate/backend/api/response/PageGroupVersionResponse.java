package io.oxalate.backend.api.response;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PageGroupVersionResponse {

    private long id;

    private long pageGroupId;

    @Size(min = 2, max = 256, message = "Title must be between 2 and 256 characters long")
    private String title;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    private String language;
}
