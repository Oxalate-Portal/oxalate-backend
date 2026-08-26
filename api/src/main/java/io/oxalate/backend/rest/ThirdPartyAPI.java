package io.oxalate.backend.rest;

import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.response.ThirdPartyEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "ThirdPartyAPI", description = "Token-authenticated third-party endpoints")
public interface ThirdPartyAPI {
    String BASE_PATH = API + "/third-party";

    @GetMapping(path = BASE_PATH + "/events", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get upcoming published events. Requires a valid third-party token in the X-Third-Party-Token header.")
    ResponseEntity<List<ThirdPartyEventResponse>> getUpcomingEvents(
            @RequestHeader(name = "X-Third-Party-Token") String tokenValue);
}
