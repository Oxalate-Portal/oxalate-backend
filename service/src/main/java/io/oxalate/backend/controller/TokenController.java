package io.oxalate.backend.controller;

import io.oxalate.backend.api.request.CreateTokenRequest;
import io.oxalate.backend.api.request.InvalidateTokenRequest;
import io.oxalate.backend.api.request.RefreshTokenRequest;
import io.oxalate.backend.api.response.TokenResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_TOKEN_CREATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_TOKEN_CREATE_START;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_TOKEN_INVALIDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_TOKEN_INVALIDATE_START;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_TOKEN_LIST_OK;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_TOKEN_LIST_START;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_TOKEN_REFRESH_OK;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_TOKEN_REFRESH_START;
import io.oxalate.backend.rest.TokenAPI;
import io.oxalate.backend.service.ThirdPartyTokenService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@AuditSource("TokenController")
public class TokenController implements TokenAPI {
    private final ThirdPartyTokenService tokenService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = THIRD_PARTY_TOKEN_CREATE_START, okMessage = THIRD_PARTY_TOKEN_CREATE_OK)
    public ResponseEntity<TokenResponse> createToken(CreateTokenRequest request) {
        return ResponseEntity.status(201)
                             .body(tokenService.create(request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = THIRD_PARTY_TOKEN_REFRESH_START, okMessage = THIRD_PARTY_TOKEN_REFRESH_OK)
    public ResponseEntity<TokenResponse> refreshToken(RefreshTokenRequest request) {
        return ResponseEntity.ok(tokenService.refresh(request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = THIRD_PARTY_TOKEN_INVALIDATE_START, okMessage = THIRD_PARTY_TOKEN_INVALIDATE_OK)
    public ResponseEntity<Void> invalidateToken(InvalidateTokenRequest request) {
        tokenService.invalidate(request.getTokenValue());
        return ResponseEntity.noContent()
                             .build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = THIRD_PARTY_TOKEN_LIST_START, okMessage = THIRD_PARTY_TOKEN_LIST_OK)
    public ResponseEntity<List<TokenResponse>> listTokens() {
        return ResponseEntity.ok(tokenService.list());
    }
}
