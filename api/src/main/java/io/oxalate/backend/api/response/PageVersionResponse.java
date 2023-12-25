package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVersionResponse {

    @Size(min = 1, message = "The ID must always be given")
    @JsonProperty("id")
    private long id;

    @Size(min = 1, message = "The page ID must always be given")
    @JsonProperty("pageId")
    private long pageId;

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
