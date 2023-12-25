package io.oxalate.backend.api.response.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiverListItemResponse {
    @JsonProperty("userId")
    private long userId;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("position")
    private long position;

    @JsonProperty("diveCount")
    private long diveCount;
}
