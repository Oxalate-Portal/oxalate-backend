package io.oxalate.backend.service;

import com.google.common.hash.Hashing;
import io.oxalate.backend.model.Token;
import io.oxalate.backend.model.TokenType;
import static io.oxalate.backend.model.TokenType.EMAIL_RESEND;
import static io.oxalate.backend.model.TokenType.REGISTRATION;
import io.oxalate.backend.repository.TokenRepository;
import jakarta.transaction.Transactional;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegistrationService {
    private final UserService userService;
    private final TokenRepository tokenRepository;

    @Value("${oxalate.token.expires-after}")
    private long tokenExpiresAfter;

    @Value("${oxalate.token.maxRetryCount}")
    private long maxRetryCount;

    @Transactional
    public String generateToken(long userId, TokenType tokenType) {
        var token = Hashing.sha256().hashString(UUID.randomUUID().toString(), UTF_8).toString();
        var registrationToken = Token.builder()
                .token(token)
                .tokenType(tokenType)
                .userId(userId)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60 * 60 * tokenExpiresAfter))
                .build();
        tokenRepository.save(registrationToken);

        return token;
    }

    @Transactional
    public void removeToken(String token) {
        tokenRepository.deleteByToken(token);
    }


    @Transactional
    public void removeTokenByUserId(long userId) {
        tokenRepository.deleteByUserId(userId);
    }

    /**
     * Returns the Token object if the token is valid, otherwise returns null and removes the invalid token as well as the user
     *
     * @param tokenString Token to be validated
     * @param tokenType Type of token, see {@link TokenType}
     * @return user ID if the token is valid, otherwise 0
     */

    @Transactional
    public Token getValidToken(String tokenString, TokenType tokenType) {
        var optionalToken = tokenRepository.findByTokenAndTokenType(tokenString, tokenType);

        if (optionalToken.isEmpty()) {
            log.warn("No token of type {} could be found with: {}", tokenType, tokenString);
            return null;
        }

        var token = optionalToken.get();

        if (token.getRetryCount() >= maxRetryCount ||
                token.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Registration token has expired ({}) or exceeded max retry count: {}", token.getExpiresAt(), token.getRetryCount());
            var userId = token.getUserId();
            tokenRepository.deleteByUserId(userId);

            // Let's only delete the user account if the token is a registration token or an email resend token, at which point the user has not yet activated their account
            if (tokenType == REGISTRATION || tokenType == EMAIL_RESEND) {
                userService.deleteUser(userId);
            }

            return null;
        }

        return token;
    }

    @Transactional
    public void increaseTokenCounter(Token token) {
        token.setRetryCount(token.getRetryCount() + 1);
        tokenRepository.save(token);
    }

    public Token findByTokenAndTokenType(String token, TokenType tokenType) {
        var optionalToken = tokenRepository.findByTokenAndTokenType(token, tokenType);
        return optionalToken.orElse(null);
    }

    public Token findByUserIdAndTokenType(long userId, TokenType tokenType) {
        var optionalToken = tokenRepository.findByUserIdAndTokenType(userId, tokenType);
        return optionalToken.orElse(null);
    }
}
