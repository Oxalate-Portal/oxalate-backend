package io.oxalate.backend.security.jwt;

import io.oxalate.backend.security.service.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtUtils {

    @Value("${oxalate.app.jwt-secret}")
    private String jwtSecret;
    @Value("${oxalate.app.jwt-expiration-ms}")
    private int jwtExpirationMs;
    private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    public String generateJwtToken(Authentication authentication) {
        var userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        var nowDate = Instant.now();
        var expDate = nowDate.plus(jwtExpirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                   .setSubject((userPrincipal.getUsername()))
                   .setId(UUID.randomUUID()
                              .toString())
                   .setIssuedAt(Date.from(nowDate))
                   .setExpiration(Date.from(expDate))
                   .signWith(getSignInKey(), SignatureAlgorithm.HS512)
                   .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUserNameFromJwtToken(String authToken) {
        return getJwsClaims(authToken).getBody()
                                      .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            getJwsClaims(authToken);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private Jws<Claims> getJwsClaims(String jwtToken) {
        return Jwts.parser()
                   .setSigningKey(getSignInKey())
                   .build()
                   .parseSignedClaims(jwtToken);
    }
}
