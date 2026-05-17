package io.oxalate.backend.api.response.filetransfer;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class AvatarFileResponse extends AbstractFileResponse {
    // An explicit constructor is needed to call the super constructor, as Lombok's @Data does not generate one with arguments
    public AvatarFileResponse(long id,
            String filename,
            String creator,
            Instant createdAt,
            String mimetype,
            long filesize,
            String filechecksum,
            String url) {
        super(id, filename, creator, createdAt, mimetype, filesize, filechecksum, url);
    }
}
