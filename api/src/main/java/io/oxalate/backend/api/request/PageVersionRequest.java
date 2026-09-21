package io.oxalate.backend.api.request;

import jakarta.validation.constraints.Min;
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
public class PageVersionRequest {

    @Min(0)
    private Long id;

    @Min(0)
    private Long pageId;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    private String language;

    @Size(min = 2, max = 256, message = "Title must be between 2 and 256 characters long")
    private String title;

    @Size(min = 2, max = 512, message = "Ingress must be between 2 and 512 characters long")
    private String ingress;

    @Size(min = 2, message = "Body must be at least 2 characters long")
    private String body;
}
