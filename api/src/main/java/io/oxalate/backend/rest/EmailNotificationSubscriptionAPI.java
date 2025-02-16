package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import io.oxalate.backend.api.request.EmailNotificationSubscriptionRequest;
import io.oxalate.backend.api.response.EmailNotificationSubscriptionResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "EmailNotificationSubscriptionAPI", description = "Email notification subscription REST endpoints")
public interface EmailNotificationSubscriptionAPI {
    String BASE_PATH = "/api/email-notification-subscriptions";

    @Operation(description = "Get all subscriptions of the user", tags = "EmailNotificationSubscriptionAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of subscriptions retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EmailNotificationSubscriptionResponse>> getAllEmailNotificationSubscriptions(HttpServletRequest request);

    @Operation(description = "Set subscriptions of the user, this always takes the complete list of subscriptions, including the existing ones" +
            "To unsubscribe, send an empty list",
            tags = "EmailNotificationSubscriptionAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Email notification subscription request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of subscriptions retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EmailNotificationSubscriptionResponse>> subscribeToEmailNotifications(HttpServletRequest request, @RequestBody EmailNotificationSubscriptionRequest subscriptions);

}
