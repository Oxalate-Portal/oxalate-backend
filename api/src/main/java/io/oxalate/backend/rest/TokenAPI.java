package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.CreateTokenRequest;
import io.oxalate.backend.api.request.InvalidateTokenRequest;
import io.oxalate.backend.api.request.RefreshTokenRequest;
import io.oxalate.backend.api.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "TokenAPI", description = "Third-party token management endpoints")
public interface TokenAPI {
    String BASE_PATH = API + "/tokens";

    @PostMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a third-party token")
    @SecurityRequirement(name = JWT_COOKIE)
    ResponseEntity<TokenResponse> createToken(@Valid @RequestBody CreateTokenRequest request);

    @PostMapping(path = BASE_PATH + "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Refresh a third-party token")
    @SecurityRequirement(name = JWT_COOKIE)
    ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request);

    @DeleteMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Invalidate a third-party token")
    @SecurityRequirement(name = JWT_COOKIE)
    ResponseEntity<Void> invalidateToken(@Valid @RequestBody InvalidateTokenRequest request);

    @GetMapping(path = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List third-party tokens (token values are never returned)")
    @SecurityRequirement(name = JWT_COOKIE)
    ResponseEntity<List<TokenResponse>> listTokens();
}
