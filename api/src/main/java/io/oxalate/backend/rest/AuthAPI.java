package io.oxalate.backend.rest;

import io.oxalate.backend.api.request.EmailRequest;
import io.oxalate.backend.api.request.LoginRequest;
import io.oxalate.backend.api.request.SignupRequest;
import io.oxalate.backend.api.request.TokenRequest;
import io.oxalate.backend.api.request.UserResetPasswordRequest;
import io.oxalate.backend.api.request.UserUpdatePasswordRequest;
import io.oxalate.backend.api.response.RegistrationResponse;
import io.oxalate.backend.api.response.UserUpdateStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AuthAPI", description = "Authentication REST endpoints")
public interface AuthAPI {
    String BASE_PATH = "/api/auth";

    @Operation(description = "Login endpoint", tags = "AuthAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Login request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "403", description = "Authentication failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = BASE_PATH + "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request);

    @Operation(description = "Registration endpoint", tags = "AuthAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Registration request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "403", description = "Registration failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = BASE_PATH + "/register")
    ResponseEntity<RegistrationResponse> registerUser(@Valid @RequestBody SignupRequest signupRequest, HttpServletRequest request);

    @Operation(description = "Update user password", tags = "AuthAPI")
    @Parameter(name = "userId", description = "User ID whose password is updated", example = "123", required = true)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PasswordUpdateRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = BASE_PATH + "/{userId}/password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserUpdateStatus> updateUserPassword(@PathVariable("userId") long userId, @RequestBody UserUpdatePasswordRequest updatePasswordRequest,
            HttpServletRequest request);

    @Operation(description = "Endpoint used in the confirmation email link", tags = "AuthAPI")
    @Parameter(name = "token", description = "Token string generated when the user has registered. This is a SHA256SUM string",
            example = "b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to the frontend page with either a success or error message"),
    })
    @GetMapping(path = BASE_PATH + "/registrations")
    ResponseEntity<Void> verifyRegistration(@RequestParam(name = "token") String token, HttpServletRequest request);

    @Operation(description = "Endpoint used to resend confirmation email link", tags = "AuthAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "TokenRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request was OK. Endpoint does not reveal whether mail sending was successful to prevent "
                    + "email phishing."),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping(path = BASE_PATH + "/registrations/resend-confirmation")
    ResponseEntity<?> resendConfirmationEmail(@RequestBody TokenRequest tokenRequest, HttpServletRequest request);

    @Operation(description = "Endpoint used to resend confirmation email link", tags = "AuthAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "EmailRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request was OK. Endpoint does not reveal whether mail sending was successful to prevent "
                    + "email phishing."),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping(path = BASE_PATH + "/lost-password")
    ResponseEntity<?> lostPassword(@RequestBody EmailRequest emailRequest, HttpServletRequest request);

    @Operation(description = "Endpoint used to reset a forgotten password. This is different from the /api/auth/{userId}/password as the latter does "
            + "not require authentication. This requires a valid token", tags = "AuthAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "UserResetPasswordRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request was OK. Endpoint does not reveal whether mail sending was successful to prevent "
                    + "email phishing."),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping(path = BASE_PATH + "/reset-password")
    ResponseEntity<?> resetPassword(@RequestBody UserResetPasswordRequest userResetPasswordRequest, HttpServletRequest request);
}
