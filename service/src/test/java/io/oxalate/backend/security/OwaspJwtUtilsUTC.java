package io.oxalate.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OWASP A07:2025 (Authentication Failures) and A04:2025 (Cryptographic Failures).
 * <p>
 * The JWT cookie is the only thing standing between an anonymous request and a fully privileged session, so
 * every way of producing an unacceptable token must be rejected. In particular these tests pin down that
 * validation fails closed: before this suite existed, {@code validateJwtToken} caught the deprecated
 * {@code io.jsonwebtoken.SignatureException} while jjwt 0.13 throws
 * {@code io.jsonwebtoken.security.SignatureException}, so a forged signature took an unintended code path.
 */
@DisplayName("OWASP A07: JWT validation fails closed")
class OwaspJwtUtilsUTC {

    private static final String ISSUER = "oxalate-portal";
    private static final String AUDIENCE = "oxalate-portal-frontend";
    private static final String SECRET = base64Key('a');
    private static final String OTHER_SECRET = base64Key('b');

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpiration", 3600);
        ReflectionTestUtils.setField(jwtUtils, "jwtIssuer", ISSUER);
        ReflectionTestUtils.setField(jwtUtils, "jwtAudience", AUDIENCE);
    }

    @Test
    void validateJwtTokenWithOwnTokenOk() {
        var token = jwtUtils.generateJwtToken(authentication());

        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("member@example.tld", jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    void validateJwtTokenWithTamperedSignatureFail() {
        var token = jwtUtils.generateJwtToken(authentication());
        var signatureStart = token.lastIndexOf('.') + 1;
        // Flip a character in the middle of the signature segment. Touching the final character is not enough,
        // because base64url padding bits mean several encodings can map to the same bytes.
        var flipIndex = signatureStart + 5;
        var original = token.charAt(flipIndex);
        var tampered = token.substring(0, flipIndex) + (original == 'A' ? 'B' : 'A') + token.substring(flipIndex + 1);

        assertFalse(jwtUtils.validateJwtToken(tampered), "A token whose signature does not verify must be rejected");
    }

    @Test
    void validateJwtTokenWithTamperedPayloadFail() {
        var token = jwtUtils.generateJwtToken(authentication());
        var parts = token.split("\\.");
        var forgedPayload = Base64.getUrlEncoder()
                                  .withoutPadding()
                                  .encodeToString(("{\"sub\":\"admin@example.tld\",\"iss\":\"" + ISSUER + "\",\"aud\":\"" + AUDIENCE
                                          + "\",\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) + "}").getBytes());

        assertFalse(jwtUtils.validateJwtToken(parts[0] + "." + forgedPayload + "." + parts[2]),
                "Rewriting the subject must invalidate the signature");
    }

    @Test
    void validateJwtTokenSignedWithAnotherKeyFail() {
        var foreign = Jwts.builder()
                          .subject("attacker@example.tld")
                          .issuer(ISSUER)
                          .audience()
                          .add(AUDIENCE)
                          .and()
                          .issuedAt(new Date())
                          .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                          .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(OTHER_SECRET)), Jwts.SIG.HS512)
                          .compact();

        assertFalse(jwtUtils.validateJwtToken(foreign), "A token signed with a different key must be rejected");
    }

    @Test
    void validateJwtTokenWithNoneAlgorithmFail() {
        var unsigned = Jwts.builder()
                           .subject("attacker@example.tld")
                           .issuer(ISSUER)
                           .audience()
                           .add(AUDIENCE)
                           .and()
                           .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                           .compact();

        assertFalse(jwtUtils.validateJwtToken(unsigned), "An unsigned token must never be accepted");
    }

    @Test
    void validateJwtTokenWithWrongIssuerFail() {
        assertFalse(jwtUtils.validateJwtToken(tokenWith("some-other-portal", AUDIENCE, 3_600_000)),
                "A token issued by another deployment must be rejected even when the key matches");
    }

    @Test
    void validateJwtTokenWithWrongAudienceFail() {
        assertFalse(jwtUtils.validateJwtToken(tokenWith(ISSUER, "some-other-service", 3_600_000)),
                "A token minted for another audience must be rejected even when the key matches");
    }

    @Test
    void validateJwtTokenWithoutIssuerAndAudienceFail() {
        var legacy = Jwts.builder()
                         .subject("member@example.tld")
                         .issuedAt(new Date())
                         .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                         .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), Jwts.SIG.HS512)
                         .compact();

        assertFalse(jwtUtils.validateJwtToken(legacy), "A token missing the required claims must be rejected");
    }

    @Test
    void validateJwtTokenWhenExpiredFail() {
        assertFalse(jwtUtils.validateJwtToken(tokenWith(ISSUER, AUDIENCE, -3_600_000)), "An expired token must be rejected");
    }

    @Test
    void validateJwtTokenWithGarbageFail() {
        assertFalse(jwtUtils.validateJwtToken("not-a-token"));
        assertFalse(jwtUtils.validateJwtToken(""));
        assertFalse(jwtUtils.validateJwtToken("a.b.c"));
    }

    @Test
    void generateJwtTokenIssuesRequiredClaimsOk() {
        var claims = Jwts.parser()
                         .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                         .build()
                         .parseSignedClaims(jwtUtils.generateJwtToken(authentication()))
                         .getPayload();

        assertEquals(ISSUER, claims.getIssuer());
        assertTrue(claims.getAudience()
                         .contains(AUDIENCE));
        assertTrue(claims.getExpiration()
                         .after(new Date()), "Tokens must carry an expiry");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String tokenWith(String issuer, String audience, long millisFromNow) {
        return Jwts.builder()
                   .subject("member@example.tld")
                   .issuer(issuer)
                   .audience()
                   .add(audience)
                   .and()
                   .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                   .expiration(new Date(System.currentTimeMillis() + millisFromNow))
                   .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), Jwts.SIG.HS512)
                   .compact();
    }

    private UsernamePasswordAuthenticationToken authentication() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var principal = new UserDetailsImpl(1L, "member@example.tld", "irrelevant", authorities, true, 1L, false, "en");
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private static String base64Key(char filler) {
        return Base64.getEncoder()
                     .encodeToString(String.valueOf(filler)
                                           .repeat(64)
                                           .getBytes());
    }
}
