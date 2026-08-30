package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.CertificateClassificationRequest;
import io.oxalate.backend.api.response.CertificateClassificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "CertificateClassificationAPI", description = "Dive certificate classification management REST endpoints")
public interface CertificateClassificationAPI {
    String BASE_PATH = API + "/certificate-classifications";

    @Operation(description = "Get all dive certificate classifications")
    @ApiResponse(responseCode = "200", description = "Classifications retrieved successfully")
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<CertificateClassificationResponse>> getAll();

    @Operation(description = "Get a dive certificate classification")
    @ApiResponse(responseCode = "200", description = "Classification retrieved successfully")
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateClassificationResponse> getById(@PathVariable("id") long id);

    @Operation(description = "Create a dive certificate classification")
    @ApiResponse(responseCode = "200", description = "Classification created successfully")
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateClassificationResponse> create(@RequestBody CertificateClassificationRequest request);

    @Operation(description = "Update a dive certificate classification")
    @ApiResponse(responseCode = "200", description = "Classification updated successfully")
    @SecurityRequirement(name = JWT_COOKIE)
    @PutMapping(value = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateClassificationResponse> update(@RequestBody CertificateClassificationRequest request);

    @Operation(description = "Delete a dive certificate classification")
    @ApiResponse(responseCode = "200", description = "Classification deleted successfully")
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(BASE_PATH + "/{id}")
    ResponseEntity<Void> delete(@PathVariable("id") long id);
}
