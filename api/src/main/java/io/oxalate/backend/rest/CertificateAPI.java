package io.oxalate.backend.rest;

import io.oxalate.backend.api.request.CertificateRequest;
import io.oxalate.backend.api.response.CertificateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    String BASE_PATH = "/api/certificates";

    @Operation(description = "Get all dive certificates of a user", tags = "CertificateAPI")
    @Parameter(name = "userId", description = "User ID who's certificates should be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of certificates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<CertificateResponse>> getCertificates(@PathVariable("userId") long userId, HttpServletRequest request);

    @Operation(description = "Get specific certificate of a membuserer", tags = "CertificateAPI")
    @Parameter(name = "userId", description = "User ID who's certificate should be retrieved", example = "123", required = true)
    @Parameter(name = "certificateId", description = "Certificate ID that should be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/{userId}/{certificateId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateResponse> getCertificate(@PathVariable("userId") long userId, @PathVariable("certificateId") long certificateId,
            HttpServletRequest request);

    @Operation(description = "Add a new dive certificate to a user", tags = "CertificateAPI")
    @Parameter(name = "userId", description = "User ID to whom the new certificates is added", example = "123", required = true)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CertificateRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate added successfully"),
            @ApiResponse(responseCode = "400", description = "User access violation"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = BASE_PATH + "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateResponse> addCertificate(@PathVariable("userId") long userId, @RequestBody CertificateRequest certificateRequest,
            HttpServletRequest request);

    @Operation(description = "Update an existing dive certificate of a user", tags = "CertificateAPI")
    @Parameter(name = "userId", description = "User ID who's certificate should be updated", example = "123", required = true)
    @Parameter(name = "certificateId", description = "certificate ID which should be updated", example = "123", required = true)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CertificateRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Certificate does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = BASE_PATH + "/{userId}/{certificateId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CertificateResponse> updateCertificate(@PathVariable("userId") long userId,
            @PathVariable("certificateId") long certificateId,
            @RequestBody CertificateRequest certificateRequest, HttpServletRequest request);

    @Operation(description = "Remove given certificate from the user", tags = "CertificateAPI")
    @Parameter(name = "userId", description = "User ID who's certificate should be removed", example = "123", required = true)
    @Parameter(name = "certificateId", description = "certificate ID which should be removed", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Certificate does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping(value = BASE_PATH + "/{userId}/{certificateId}")
    ResponseEntity<Void> deleteCertificate(@PathVariable("userId") long userId, @PathVariable("certificateId") long certificateId, HttpServletRequest request);
}
