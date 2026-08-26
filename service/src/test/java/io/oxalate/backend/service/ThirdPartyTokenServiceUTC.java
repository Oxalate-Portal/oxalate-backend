package io.oxalate.backend.service;

import io.oxalate.backend.api.request.CreateTokenRequest;
import io.oxalate.backend.api.request.RefreshTokenRequest;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.model.ThirdPartyToken;
import io.oxalate.backend.repository.ThirdPartyTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThirdPartyTokenServiceUTC {
    @Mock
    private ThirdPartyTokenRepository repository;
    @Mock
    private AppEventPublisher appEventPublisher;
    @InjectMocks
    private ThirdPartyTokenService service;

    @Test
    void createGeneratesLongTokenAndReturnsItOnce() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(new CreateTokenRequest(Instant.now()
                                                                    .plus(1, ChronoUnit.DAYS), "test"));

        assertEquals("test", response.getDescription());
        assertEquals(64, response.getTokenValue()
                                 .length());
    }

    @Test
    void refreshReplacesTokenAndExtendsExpiry() {
        var old = ThirdPartyToken.builder()
                                 .tokenId(1)
                                 .tokenValue("old")
                                 .createdAt(Instant.now())
                                 .expiresAt(Instant.now()
                                                   .plus(1, ChronoUnit.DAYS))
                                 .build();
        when(repository.findByTokenValue("old")).thenReturn(Optional.of(old));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.refresh(new RefreshTokenRequest("old", 10));

        assertEquals(64, response.getTokenValue()
                                 .length());
        verify(repository).save(old);
        org.junit.jupiter.api.Assertions.assertNotEquals("old", old.getTokenValue());
    }

    @Test
    void invalidTokenIsForbiddenAndAudited() {
        when(repository.findByTokenValue("bad")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.validate("bad"));
        verify(appEventPublisher).publishAuditEvent("Invalid or expired third-party token",
                io.oxalate.backend.api.AuditLevelEnum.WARN, "ThirdPartyController", null);
    }

    @Test
    void expiredTokenIsRejected() {
        var token = ThirdPartyToken.builder()
                                   .tokenValue("expired")
                                   .expiresAt(Instant.now()
                                                     .minusSeconds(1))
                                   .build();
        when(repository.findByTokenValue("expired")).thenReturn(Optional.of(token));

        assertThrows(RuntimeException.class, () -> service.validate("expired"));
    }
}
