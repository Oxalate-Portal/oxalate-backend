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

    public EmailService(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    public void sendConfirmationEmail(User user, String token) {
        var confirmationUrlWithToken = confirmationUrl + "?token=" + token;

        // TODO Figure out how to not have this hardcoded, rather use a template, with different language support
        var subject = "Tervetuloa " + orgName + ":n portaaliin";

        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("url", confirmationUrlWithToken);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("orgName", orgName);
            context.setVariable("tokenTtl", tokenTtl);
            sendHtmlMail(systemEmail, user.getUsername(), subject, "confirmationTemplate_fi", context);
        } catch (MailException e) {
            log.error("Sending user confirmation email failed: ", e);
        }
    }

    public boolean sendForgottenPassword(User user, String token) {
        var lostPasswordUrlWithToken = lostPasswordUrl + "/" + token;
        var subject = "Unohtunut salasana";

        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("url", lostPasswordUrlWithToken);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("orgName", orgName);
            context.setVariable("tokenTtl", tokenTtl);
            sendHtmlMail(systemEmail, user.getUsername(), subject, "lostPasswordTemplate_fi", context);
            return true;
        } catch (MailException e) {
            log.error("Sending user confirmation email failed: ", e);
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
