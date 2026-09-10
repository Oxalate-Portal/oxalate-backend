package io.oxalate.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.oxalate.backend.security.service.UserDetailsImpl;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Issues and validates the JWTs used for authentication.
 * <p>
 * OWASP A07:2025 / A04:2025 notes:
 * <ul>
 *     <li>Tokens are signed with HS512 using {@code oxalate.app.jwt-secret}. The secret is validated at
 *     startup by {@code SecurityPreflight}; a weak or known-development secret aborts a non-local boot.</li>
 *     <li>Every token carries {@code iss} and {@code aud} claims, and both are <em>required</em> when
 *     parsing. This stops tokens minted by another Oxalate deployment (or another service that happens to
 *     share the key) from being accepted here.</li>
 *     <li>Clock skew is bounded, and validation fails closed: any parsing or claim problem returns
 *     {@code false}.</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * Maximum accepted clock drift between issuer and verifier.
     */
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30L;

    @Value("${oxalate.app.jwt-secret}")
    private String jwtSecret;
    @Value("${oxalate.app.jwt-expiration}")
    private int jwtExpiration;
    @Value("${oxalate.app.jwt-issuer:oxalate-portal}")
    private String jwtIssuer;
    @Value("${oxalate.app.jwt-audience:oxalate-portal-frontend}")
    private String jwtAudience;

    public String generateJwtToken(Authentication authentication) {
        var userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        var nowDate = Instant.now();
        var expDate = nowDate.plus(jwtExpiration, ChronoUnit.SECONDS);

        return Jwts.builder()
                   .subject(userPrincipal.getUsername())
                   .id(UUID.randomUUID()
                           .toString())
                   .issuer(jwtIssuer)
                   .audience()
                   .add(jwtAudience)
                   .and()
                   .issuedAt(Date.from(nowDate))
                   .expiration(Date.from(expDate))
                   .signWith(getSignInKey(), Jwts.SIG.HS512)
                   .compact();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUserNameFromJwtToken(String authToken) {
        return getJwsClaims(authToken).getPayload()
                                      .getSubject();
    }

    /**
     * Validates signature, expiry, issuer and audience of the given token.
     *
     * @param authToken raw compact JWT
     * @return {@code true} only when the token is fully valid
     */
    public boolean validateJwtToken(String authToken) {
        try {
            getJwsClaims(authToken);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            // This is such a common exception that we log it as debug
            log.debug("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            // Covers MissingClaimException / IncorrectClaimException for iss and aud, and anything else the
            // parser may raise. Validation must always fail closed.
            log.error("JWT token validation failed: {}", e.getMessage());
        }

        return false;
    }

    private Jws<Claims> getJwsClaims(String jwtToken) {
        return Jwts.parser()
                   .verifyWith(getSignInKey())
                   .requireIssuer(jwtIssuer)
                   .requireAudience(jwtAudience)
                   .clockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                   .build()
                   .parseSignedClaims(jwtToken);
    }
}
