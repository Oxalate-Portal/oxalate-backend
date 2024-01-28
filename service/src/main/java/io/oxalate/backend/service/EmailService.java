package io.oxalate.backend.service;

import io.oxalate.backend.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;

    private final TemplateEngine templateEngine;

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

    public EmailService(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    public void sendConfirmationEmail(User user, String token) {
        var confirmationUrlWithToken = confirmationUrl + "?token=" + token;
        var userLanguage = user.getLanguage() != null ? user.getLanguage() : defaultLanguage;
        // TODO Look into proper localization

        var subject = "Welcome to " + orgName + " portal";

        subject = switch (userLanguage) {
			case "fi" -> "Tervetuloa " + orgName + ":n portaaliin";
			case "sv" -> "Välkommen till " + orgName + " portal";
			case "de" -> "Willkommen zu " + orgName + " portal";
			default -> subject;
		};

		try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("url", confirmationUrlWithToken);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("orgName", orgName);
            context.setVariable("tokenTtl", tokenTtl);
            sendHtmlMail(systemEmail, user.getUsername(), subject, "confirmationTemplate_" + userLanguage, context);
        } catch (MailException e) {
            log.error("Sending user confirmation email failed: ", e);
        }
    }

    public boolean sendForgottenPassword(User user, String token) {
        var lostPasswordUrlWithToken = lostPasswordUrl + "/" + token;
        var userLanguage = user.getLanguage() != null ? user.getLanguage() : defaultLanguage;

        var subject = "Forgotten password";

        subject = switch (userLanguage) {
            case "fi" -> "Unohtunut salasana";
            case "sv" -> "Glömd lösenord";
            case "de" -> "Passwort vergessen";
            default -> subject;
        };

        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("url", lostPasswordUrlWithToken);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("orgName", orgName);
            context.setVariable("tokenTtl", tokenTtl);
            sendHtmlMail(systemEmail, user.getUsername(), subject, "lostPasswordTemplate_" + userLanguage, context);
            return true;
        } catch (MailException e) {
            log.error("Sending forgotten password email failed: ", e);
        }

        return false;
    }

    @Async
    protected void sendHtmlMail(String sender, String recipient, String subject, String templateName, IContext context) {
        String body = templateEngine.process(templateName, context);

        if (!smtpEnabled) {
            log.warn("Sending email failed. Email service disabled!");
            log.info("Logging email details: from:{}, to:{}, subject:{}, template:{}",
                    sender, recipient, subject, templateName);
            log.info("Logging email body: {}", body);
            return;
        }

        MimeMessage mail = javaMailSender.createMimeMessage();

        var subjectPrefix = "prod".equals(env) ? "" : String.format("[env=%s] ", env);
        subjectPrefix += "[Oxalate] ";

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

    /**
     * This method is currently unused but saved for later use.
     */
    @Async
    protected void sendTextMail(String sender, String recipient, String subject, String text) throws MailException {
        if (!smtpEnabled) {
            log.warn("Sending email failed. Email service disabled!");
            log.info("Logging email details: from:{}, to:{}, subject:{}, text:{}",
                    sender, recipient, subject, text);
            return;
        }

        var subjectPrefix = "prod".equals(env) ? "" : String.format("[env=%s] ", env);
        subjectPrefix += "[Oxalate] ";
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subjectPrefix + subject);
        message.setText(text);

        javaMailSender.send(message);
    }
}
