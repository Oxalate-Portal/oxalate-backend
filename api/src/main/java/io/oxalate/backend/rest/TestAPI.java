package io.oxalate.backend.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("local")
@Tag(name = "TestAPI", description = "Test REST endpoints, this is only available in the local profile")
public interface TestAPI {
    String BASE_PATH = "/api/test";

    @Operation(description = "Generate users", tags = "TestAPI")
    @Parameter(name = "numberOfUsers", description = "How many users should be generated", example = "100")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Fail")
    })
    @GetMapping(path = BASE_PATH + "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> generateRandomUsers(@RequestParam("numberOfUsers") int numberOfUsers);

    @Operation(description = "Generate events beginning the given years ago. Should generate roughly 40 events per year", tags = "TestAPI")
    @Parameter(name = "yearsAgo", description = "How many years ago should the data start", example = "5")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Fail")
    })
    @GetMapping(path = BASE_PATH + "/events", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> generateYearsAgo(@RequestParam("yearsAgo") int yearsAgo);
}
