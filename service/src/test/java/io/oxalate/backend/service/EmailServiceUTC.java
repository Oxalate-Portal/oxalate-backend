package io.oxalate.backend.service;

import static io.oxalate.backend.api.PortalConfigEnum.EMAIL;
import static io.oxalate.backend.api.PortalConfigEnum.EmailConfigEnum.EMAIL_ENABLED;
import static io.oxalate.backend.api.PortalConfigEnum.EmailConfigEnum.SYSTEM_EMAIL;
import static io.oxalate.backend.api.PortalConfigEnum.GENERAL;
import static io.oxalate.backend.api.PortalConfigEnum.GeneralConfigEnum.DEFAULT_LANGUAGE;
import static io.oxalate.backend.api.PortalConfigEnum.GeneralConfigEnum.ORG_NAME;
import io.oxalate.backend.model.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceUTC {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private PortalConfigurationService portalConfigurationService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://frontend.test");
        ReflectionTestUtils.setField(emailService, "env", "test");
        ReflectionTestUtils.setField(emailService, "messageSource", messageSource);
    }

    @Test
    void sendEventCommentNotificationEmailUsesDefaultLanguageWhenMissingOk() {
        var user = User.builder()
                       .username("recipient@test.tld")
                       .language(null)
                       .build();
        var mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(portalConfigurationService.getBooleanConfiguration(EMAIL.group, EMAIL_ENABLED.key)).thenReturn(true);
        when(portalConfigurationService.getStringConfiguration(GENERAL.group, DEFAULT_LANGUAGE.key)).thenReturn("fi");
        when(portalConfigurationService.getStringConfiguration(GENERAL.group, ORG_NAME.key)).thenReturn("Oxalate");
        when(portalConfigurationService.getStringConfiguration(EMAIL.group, SYSTEM_EMAIL.key)).thenReturn("system@oxalate.tld");
        when(messageSource.getMessage(eq("email.notification.comment-event.subject"), isNull(), eq(Locale.forLanguageTag("fi"))))
                .thenReturn("Uusi kommentti sukellustapahtumassa");
        when(templateEngine.process(eq("eventCommentNotificationTemplate_fi"), any(Context.class))).thenReturn("<html>body</html>");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEventCommentNotificationEmail(user, "Uusi kommentti", "Kommentti on julkaistu", 42L);

        verify(templateEngine).process(eq("eventCommentNotificationTemplate_fi"), any(Context.class));
        verify(javaMailSender).send(any(MimeMessage.class));
    }
}
