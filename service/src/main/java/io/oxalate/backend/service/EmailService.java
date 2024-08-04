package io.oxalate.backend.service;

import io.oxalate.backend.api.EmailNotificationDetailEnum;
import io.oxalate.backend.exception.EmailNotificationException;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.PageVersion;
import io.oxalate.backend.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    private MessageSource messageSource;

    @Value("${oxalate.mail.enabled}")
    private boolean smtpEnabled;

    @Value("${oxalate.app.env}")
    private String env;

    @Value("${oxalate.app.org-name}")
    private String orgName;

    @Value("${oxalate.mail.system-email}")
    private String systemEmail;

    @Value("${oxalate.mail.support-email}")
    private String supportEmail;

    @Value("${oxalate.token.expires-after}")
    private String tokenTtl;

    @Value("${oxalate.token.confirmation-url}")
    private String confirmationUrl;

    @Value("${oxalate.token.lost-password-url}")
    private String lostPasswordUrl;

    @Value("${oxalate.language.default}")
    private String defaultLanguage;

    @Value("${oxalate.app.frontend-url}")
    private String frontendUrl;

    public void sendConfirmationEmail(User user, String token) {
        var confirmationUrlWithToken = confirmationUrl + "?token=" + token;
        var userLanguage = user.getLanguage() != null ? user.getLanguage() : defaultLanguage;
        var locale = Locale.forLanguageTag(userLanguage);
        var subject = messageSource.getMessage("email.confirmation.subject", new Object[] { orgName }, locale);

        Context context = new Context(locale);
        context.setVariable("name", user.getFirstName());
        context.setVariable("url", confirmationUrlWithToken);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("orgName", orgName);
        context.setVariable("tokenTtl", tokenTtl);
        String body = templateEngine.process("confirmationTemplate_" + locale.getLanguage(), context);

        try {
            sendHtmlMail(systemEmail, user.getUsername(), subject, body);
        } catch (MailException e) {
            log.error("Sending user confirmation email failed: ", e);
        }
    }

    public boolean sendForgottenPassword(User user, String token) {
        var lostPasswordUrlWithToken = lostPasswordUrl + "/" + token;
        var userLanguage = user.getLanguage() != null ? user.getLanguage() : defaultLanguage;
        var locale = Locale.forLanguageTag(userLanguage);
        var subject = messageSource.getMessage("email.forgotten-password.subject", null, locale);

        Context context = new Context(locale);
        context.setVariable("name", user.getFirstName());
        context.setVariable("url", lostPasswordUrlWithToken);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("orgName", orgName);
        context.setVariable("tokenTtl", tokenTtl);
        String body = templateEngine.process("lostPasswordTemplate_" + locale.getLanguage(), context);

        try {
            sendHtmlMail(systemEmail, user.getUsername(), subject, body);
            return true;
        } catch (MailException e) {
            log.error("Sending forgotten password email failed: ", e);
        }

        return false;
    }

    public void sendEventNotificationEmail(String emailAddress, String language, EmailNotificationDetailEnum detail, Event event) {
        var locale = Locale.forLanguageTag(language);
        var subject = messageSource.getMessage("email.notification." + detail.name().toLowerCase() + "-event.subject", null, locale);
        var templateName = "eventNotificationTemplate_" + detail.name().toLowerCase() + "_" + locale.getLanguage();

        log.debug("Event: {}", event);

        Context context = new Context(locale);
        context.setVariable("orgName", orgName);
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("eventTitle", event.getTitle());
        context.setVariable("eventDate", event.getStartTime());

        String body = templateEngine.process(templateName, context);

        try {
            sendHtmlMail(systemEmail, emailAddress, subject, body);
        } catch (MailException e) {
            log.error("Sending event notification email failed: ", e);
            throw new EmailNotificationException("Failed to send event notification email", e);
        }
    }

    public void sendPageNotificationEmail(String emailAddress, String language, EmailNotificationDetailEnum detail, PageVersion pageVersion) {
        var locale = Locale.forLanguageTag(language);
        var subject = messageSource.getMessage("email.notification." + detail.name().toLowerCase() + "-page.subject", null, locale);
        var templateName = "pageNotificationTemplate_" + detail.name().toLowerCase() + "_" + locale.getLanguage();

        log.debug("Page version: {}", pageVersion);

        Context context = new Context(locale);
        context.setVariable("orgName", orgName);
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("pageTitle", pageVersion.getTitle());
        context.setVariable("pageId", pageVersion.getPageId());
        context.setVariable("event", null);

        String body = templateEngine.process(templateName, context);

        try {
            sendHtmlMail(systemEmail, emailAddress, subject, body);
        } catch (MailException e) {
            log.error("Sending page notification email failed: ", e);
            throw new EmailNotificationException("Failed to send page notification email", e);
        }
    }

    @Async
    protected void sendHtmlMail(String sender, String recipient, String subject, String body) {

        if (!smtpEnabled) {
            log.info("Not sending email. Email service disabled!");
            log.info("Logging email details: from:{}, to:{}, subject:{} body: {}", sender, recipient, subject, body.replace("\n", " "));
            return;
        }

        MimeMessage mail = javaMailSender.createMimeMessage();

        var subjectPrefix = "prod".equals(env) ? "" : String.format("[env=%s] ", env);
        subjectPrefix += "[" + orgName + "] ";

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mail, true);
            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject(subjectPrefix + subject);
            helper.setText(body, true);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        javaMailSender.send(mail);
    }

    @Async
    protected void sendTextMail(String sender, String recipient, String subject, String text) throws MailException {
        if (!smtpEnabled) {
            log.info("Not sending email. Email service disabled!");
            log.info("Logging email details: from:{}, to:{}, subject:{}, text:{}",
                    sender, recipient, subject, text);
            return;
        }

        var subjectPrefix = "prod".equals(env) ? "" : String.format("[env=%s] ", env);
        subjectPrefix += "[" + orgName + "] ";
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subjectPrefix + subject);
        message.setText(text);

        javaMailSender.send(message);
    }
}
