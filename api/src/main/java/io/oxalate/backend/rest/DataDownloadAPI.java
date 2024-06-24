package io.oxalate.backend.rest;

import io.oxalate.backend.api.response.download.DownloadCertificateResponse;
import io.oxalate.backend.api.response.download.DownloadDiveResponse;
import io.oxalate.backend.api.response.download.DownloadPaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "DataDownloadAPI", description = "Data download REST endpoints")
public interface DataDownloadAPI {
    String BASE_PATH = "/api/data-download";

    @Operation(description = "Download dive certificate data", tags = "DataDownloadAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of certificate download retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/certificates", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DownloadCertificateResponse>> downloadCertificates(HttpServletRequest request);

    @Operation(description = "Download dive data", tags = "DataDownloadAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of dive download retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/dives", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DownloadDiveResponse>> downloadDives(HttpServletRequest request);

    @Operation(description = "Download payments data", tags = "DataDownloadAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of payment download retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DownloadPaymentResponse>> downloadPayments(HttpServletRequest request);
}
