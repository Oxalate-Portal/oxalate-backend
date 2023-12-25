package io.oxalate.backend.api;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractMessage {
    private long id;
    private String description;
    private String title;
    private String message;
    private long creator;
    private Instant createdAt;
}
