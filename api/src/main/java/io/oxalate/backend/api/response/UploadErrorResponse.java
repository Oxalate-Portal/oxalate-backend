package io.oxalate.backend.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadErrorResponse {
    private UploadErrorMessage error;

    @Builder
    @Data
    public static class UploadErrorMessage {
        private String message;
    }
}
