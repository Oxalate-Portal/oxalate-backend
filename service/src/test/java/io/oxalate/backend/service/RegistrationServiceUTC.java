package io.oxalate.backend.service;

import io.oxalate.backend.model.Token;
import static io.oxalate.backend.model.TokenType.REGISTRATION;
import io.oxalate.backend.repository.TokenRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceUTC {

    @Mock
    private UserService userService;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registrationService, "tokenExpiresAfter", 12L);
        ReflectionTestUtils.setField(registrationService, "maxRetryCount", 3L);
    }

    @Test
    void generateRegistrationTokenPersistsTokenBeforeReturningIt() {
        when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var tokenValue = registrationService.generateToken(42L, REGISTRATION);

        var tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        var token = tokenCaptor.getValue();
        assertEquals(tokenValue, token.getToken());
        assertEquals(REGISTRATION, token.getTokenType());
        assertEquals(42L, token.getUserId());
        assertEquals(0, token.getRetryCount());
        assertNotNull(token.getCreatedAt());
        assertNotNull(token.getExpiresAt());
    }
}
