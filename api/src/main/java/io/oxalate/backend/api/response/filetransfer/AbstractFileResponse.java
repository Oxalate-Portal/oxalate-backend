package io.oxalate.backend.api.response.filetransfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AbstractFileResponse {
    @JsonProperty("id")
    private long id;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("creator")
    private String creator;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("mimetype")
    private String mimetype;

    @JsonProperty("filesize")
    private long filesize;

    @JsonProperty("filechecksum")
    private String filechecksum;

    @JsonProperty("url")
    private String url;
}
