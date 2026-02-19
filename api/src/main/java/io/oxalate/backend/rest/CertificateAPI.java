package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.CertificateRequest;
import io.oxalate.backend.api.response.CertificateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "CertificateAPI", description = "Dive certificate REST endpoints")
public interface CertificateAPI {
    String BASE_PATH = API + "/certificates";

    @Operation(description = "Get all dive certificates", tags = "CertificateAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of certificates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<CertificateResponse>> getAllCertificates();

    @Operation(description = "Get all dive certificates of a user", tags = "CertificateAPI")
    @Parameter(name = "userId", description = "User ID who's certificates should be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of certificates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<CertificateResponse>> getUserCertificates(@PathVariable("userId") long userId);

    @Operation(description = "Get specific certificate", tags = "CertificateAPI")
    @Parameter(name = "certificateId", description = "Certificate ID that should be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/{certificateId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateResponse> getCertificate(@PathVariable("certificateId") long certificateId);

    @Operation(description = "Add a new dive certificate to the user. The user is always the current caller as the userId in the request is ignored and " +
            "pulled from the session instead. The userId in the request should thus be set to 0", tags = "CertificateAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CertificateRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate added successfully"),
            @ApiResponse(responseCode = "400", description = "User access violation"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateResponse> addCertificate(@RequestBody CertificateRequest certificateRequest);

    @Operation(description = "Update an existing dive certificate of a user", tags = "CertificateAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CertificateRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Certificate does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PutMapping(value = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateResponse> updateCertificate(@RequestBody CertificateRequest certificateRequest);

    @Operation(description = "Remove given certificate from the user", tags = "CertificateAPI")
    @Parameter(name = "certificateId", description = "certificate ID which should be removed", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Certificate does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(value = BASE_PATH + "/{certificateId}")
    ResponseEntity<Void> deleteCertificate(@PathVariable("certificateId") long certificateId);
}
