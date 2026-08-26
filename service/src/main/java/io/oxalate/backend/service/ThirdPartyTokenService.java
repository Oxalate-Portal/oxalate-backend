package io.oxalate.backend.service;

import static io.oxalate.backend.api.AuditLevelEnum.WARN;
import io.oxalate.backend.api.request.CreateTokenRequest;
import io.oxalate.backend.api.request.RefreshTokenRequest;
import io.oxalate.backend.api.response.TokenResponse;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.model.ThirdPartyToken;
import io.oxalate.backend.repository.ThirdPartyTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThirdPartyTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ThirdPartyTokenRepository repository;
    private final AppEventPublisher appEventPublisher;

    @Transactional
    public TokenResponse create(CreateTokenRequest request) {
        var now = Instant.now();
        var entity = ThirdPartyToken.builder()
                                    .tokenValue(generateUniqueToken())
                                    .createdAt(now)
                                    .expiresAt(request.getExpiresAt())
                                    .description(request.getDescription())
                                    .build();
        return toResponse(repository.save(entity), true);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        var token = findOrThrow(request.getTokenValue());
        var now = Instant.now();
        token.setTokenValue(generateUniqueToken());
        token.setExpiresAt(token.getExpiresAt()
                                .isAfter(now)
                ?
                token.getExpiresAt()
                     .plus(request.getDays(), ChronoUnit.DAYS)
                :
                now.plus(request.getDays(), ChronoUnit.DAYS));
        return toResponse(repository.save(token), true);
    }

    @Transactional
    public void invalidate(String tokenValue) {
        var token = findOrThrow(tokenValue);
        token.setExpiresAt(Instant.now());
        repository.save(token);
    }

    @Transactional(readOnly = true)
    public List<TokenResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc()
                         .stream()
                         .map(token -> toResponse(token, true))
                         .toList();
    }

    @Transactional(readOnly = true)
    public void validate(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            auditInvalid("Missing third-party token");
            throw new io.oxalate.backend.exception.OxalateUnauthorizedException(
                    "Third-party token is required", HttpStatus.UNAUTHORIZED);
        }
        var token = repository.findByTokenValue(tokenValue);
        if (token.isEmpty() || !token.get()
                                     .getExpiresAt()
                                     .isAfter(Instant.now())) {
            auditInvalid("Invalid or expired third-party token");
            throw new io.oxalate.backend.exception.OxalateUnauthorizedException(
                    "Invalid or expired third-party token", HttpStatus.FORBIDDEN);
        }
    }

    private ThirdPartyToken findOrThrow(String value) {
        return repository.findByTokenValue(value)
                         .orElseThrow(() ->
                                 new io.oxalate.backend.exception.OxalateValidationException(
                                         "Third-party token was not found", HttpStatus.NOT_FOUND));
    }

    private String generateUniqueToken() {
        String value;
        do {
            byte[] bytes = new byte[48];
            RANDOM.nextBytes(bytes);
            value = Base64.getUrlEncoder()
                          .withoutPadding()
                          .encodeToString(bytes);
        } while (repository.findByTokenValue(value)
                           .isPresent());
        return value;
    }

    private void auditInvalid(String message) {
        appEventPublisher.publishAuditEvent(message, WARN, "ThirdPartyController", null);
    }

    private TokenResponse toResponse(ThirdPartyToken token, boolean includeValue) {
        return TokenResponse.builder()
                            .tokenId(token.getTokenId())
                            .tokenValue(includeValue ? token.getTokenValue() : null)
                            .createdAt(token.getCreatedAt())
                            .expiresAt(token.getExpiresAt())
                            .description(token.getDescription())
                            .build();
    }
}
