package io.oxalate.backend.api.response.filetransfer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class AvatarFileResponse extends AbstractFileResponse {
}
