package io.oxalate.backend.service;

import static io.oxalate.backend.api.PortalConfigEnum.GENERAL;
import static io.oxalate.backend.api.PortalConfigEnum.GeneralConfigEnum.DEFAULT_LANGUAGE;
import io.oxalate.backend.model.User;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class NotificationLocalizationServiceUTC {

    @Mock
    private MessageSource messageSource;

    @Mock
    private PortalConfigurationService portalConfigurationService;

    @InjectMocks
    private NotificationLocalizationService notificationLocalizationService;

    @Test
    void getMessageUsesGermanUserLanguageOk() {
        var user = User.builder()
                       .language("de")
                       .build();
        when(messageSource.getMessage(eq("notification.waiting-list.title"), eq(new Object[0]), eq(Locale.GERMAN)))
                .thenReturn("Wartelisten-Aktualisierung");

        var message = notificationLocalizationService.getMessage(user, "notification.waiting-list.title");

        assertEquals("Wartelisten-Aktualisierung", message);
        verify(messageSource).getMessage("notification.waiting-list.title", new Object[0], Locale.GERMAN);
    }

    @Test
    void getMessageUsesFinnishUserLanguageWithArgumentsOk() {
        var user = User.builder()
                       .language("fi")
                       .build();
        when(messageSource.getMessage(eq("notification.waiting-list.message"), eq(new Object[] { "Test event", 42L }),
                eq(Locale.forLanguageTag("fi")))).thenReturn("Sinut on siirretty tapahtumaan");

        var message = notificationLocalizationService.getMessage(
                user, "notification.waiting-list.message", new Object[] { "Test event", 42L });

        assertEquals("Sinut on siirretty tapahtumaan", message);
    }

    @Test
    void getMessageUsesConfiguredDefaultLanguageWhenUserLanguageMissingOk() {
        when(portalConfigurationService.getStringConfiguration(GENERAL.group, DEFAULT_LANGUAGE.key)).thenReturn("sv");
        when(messageSource.getMessage(eq("notification.dive-group.title"), eq(new Object[0]), eq(Locale.forLanguageTag("sv"))))
                .thenReturn("Uppdatering av dykgrupp");

        var message = notificationLocalizationService.getMessage(User.builder()
                                                                     .build(), "notification.dive-group.title");

        assertEquals("Uppdatering av dykgrupp", message);
        verify(messageSource).getMessage("notification.dive-group.title", new Object[0], Locale.forLanguageTag("sv"));
    }
}
