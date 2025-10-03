package io.oxalate.backend.api.request;

import io.oxalate.backend.api.response.TagResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Signup request")
public class TagRequest extends TagResponse {
}
