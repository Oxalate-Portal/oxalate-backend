package io.oxalate.backend.service;

import static io.oxalate.backend.api.PortalConfigEnum.GENERAL;
import static io.oxalate.backend.api.PortalConfigEnum.GeneralConfigEnum.DEFAULT_LANGUAGE;

import io.oxalate.backend.model.User;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationLocalizationService {

    private final MessageSource messageSource;
    private final PortalConfigurationService portalConfigurationService;

    public String getMessage(User user, String messageKey) {
        return getMessage(user, messageKey, new Object[0]);
    }

    public String getMessage(User user, String messageKey, Object[] arguments) {
        return messageSource.getMessage(messageKey, arguments, resolveLocale(user));
    }

    private Locale resolveLocale(User user) {
        var language = user == null ? null : user.getLanguage();
        if (language == null) {
            language = portalConfigurationService.getStringConfiguration(GENERAL.group, DEFAULT_LANGUAGE.key);
        }
        return Locale.forLanguageTag(language == null ? "en" : language);
    }
}
