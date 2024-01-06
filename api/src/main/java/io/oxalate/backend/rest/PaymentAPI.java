package io.oxalate.backend.rest;

import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.api.response.PaymentStatusResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "PaymentAPI", description = "Payment REST endpoints")
public interface PaymentAPI {
    String BASE_PATH = "/api/payments";

    @Operation(description = "Get all active payment status", tags = "PaymentAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PaymentStatusResponse>> getAllActivePaymentStatus(HttpServletRequest request);

    @Operation(description = "Get the payment status a specific user", tags = "PaymentAPI")
    @Parameter(name = "userId", description = "User ID for which current payment status should be fetched", example = "123")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PaymentStatusResponse> getPaymentStatusForUser(@PathVariable(name = "userId") long userId, HttpServletRequest request);

    @Operation(description = "Add a payment entry to a specific user", tags = "PaymentAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New payment", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PaymentStatusResponse> addPaymentForUser(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request);

    @Operation(description = "Update payment status for a specific user. This is only effective on one-time payments where you can decrease the count",
            tags = "PaymentAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Payment to be updated, defined by the payment type", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(path = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PaymentStatusResponse> updatePaymentForUser(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request);

    @Operation(description = "Reset all period payments immediately. This will update the period payment expiration time to now()", tags = "PaymentAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reset completed successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/reset")
    ResponseEntity<Void> resetAllPeriodicPayments(HttpServletRequest request);
}
