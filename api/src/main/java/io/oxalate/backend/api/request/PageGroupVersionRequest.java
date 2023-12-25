package io.oxalate.backend.api.request;

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
public class PageGroupVersionRequest {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("pageGroupId")
    private Long pageGroupId;

    @Size(min = 2, max = 256, message = "Title must be between 2 and 256 characters long")
    @JsonProperty("title")
    private String title;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @JsonProperty("language")
    private String language;
}
