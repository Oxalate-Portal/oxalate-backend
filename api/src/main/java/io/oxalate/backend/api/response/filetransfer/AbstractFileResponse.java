package io.oxalate.backend.api.response.filetransfer;

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
public class AbstractFileResponse {
    private long id;

    private String filename;

    private String creator;

    private Instant createdAt;

    private String mimetype;

    private long filesize;

    private String filechecksum;

    private String url;
}
