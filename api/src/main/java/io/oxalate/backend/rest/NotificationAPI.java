package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.MarkReadRequest;
import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.MessageResponse;
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

@Tag(name = "NotificationAPI", description = "Notification REST endpoints")
public interface NotificationAPI {
    String BASE_PATH = API + "/notifications";

    @Operation(description = "Get unread notifications for the logged in user", tags = "NotificationAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/unread", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MessageResponse>> getUnreadNotifications(HttpServletRequest request);

    @Operation(description = "Mark notifications as read for the logged in user", tags = "NotificationAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "MarkReadRequest containing list of message IDs to mark as read", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH + "/mark-read", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> markNotificationsAsRead(@RequestBody MarkReadRequest markReadRequest, HttpServletRequest request);

    @Operation(description = "Create a new notification for a user", tags = "NotificationAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "MessageRequest containing the notification details", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification created successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH + "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MessageResponse> createNotification(@RequestBody MessageRequest messageRequest, HttpServletRequest request);

    @Operation(description = "Create new notifications for a list of users or all active users", tags = "NotificationAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "MessageRequest containing the notification details and recipient list or sendAll flag", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications created successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH + "/create-bulk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> createBulkNotifications(@RequestBody MessageRequest messageRequest, HttpServletRequest request);

    @Operation(description = "Get all notifications for the logged in user (both read and unread)", tags = "NotificationAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MessageResponse>> getAllNotifications(HttpServletRequest request);
}
