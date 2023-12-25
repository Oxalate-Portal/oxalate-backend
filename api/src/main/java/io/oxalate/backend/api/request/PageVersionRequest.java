package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVersionRequest {

    @Min(0)
    @JsonProperty("id")
    private Long id;

    @Min(0)
    @JsonProperty("pageId")
    private Long pageId;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @JsonProperty("language")
    private String language;

    @Size(min = 2, max = 256, message = "Title must be between 2 and 256 characters long")
    @JsonProperty("title")
    private String title;

    @Size(min = 2, max = 512, message = "Ingress must be between 2 and 512 characters long")
    @JsonProperty("ingress")
    private String ingress;

    @Size(min = 2, message = "Body must be at least 2 characters long")
    @JsonProperty("body")
    private String body;
}
