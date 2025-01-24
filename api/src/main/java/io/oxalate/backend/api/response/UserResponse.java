package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    @JsonProperty("id")
    protected long id;

    @JsonProperty("username")
    protected String username;

    @JsonProperty("firstName")
    protected String firstName;

    @JsonProperty("lastName")
    protected String lastName;

    @JsonProperty("phoneNumber")
    protected String phoneNumber;

    @JsonProperty("registered")
    protected Instant registered;

    @JsonProperty("diveCount")
    private long diveCount;

    @JsonProperty("payments")
    private Set<PaymentResponse> payments;

    @JsonProperty("memberships")
    private List<MembershipResponse> memberships;

    @JsonProperty("approvedTerms")
    private boolean approvedTerms;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @JsonProperty("language")
    private String language;
}
