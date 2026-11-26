package ru.tsu.bugtracker.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;
    private final String frontendUrl;

    public MailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            MailProperties mailProperties,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.frontendUrl = frontendUrl;
    }

    public void sendEmail(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured, skipping email to {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendVerificationEmail(String to, String fullName, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        String subject = "Verify your email address";
        String body = "Hello, " + fullName + "!\n\n"
                + "To verify your email address, follow the link below:\n"
                + link + "\n\n"
                + "If you did not sign up for BugTracker, you can safely ignore this email.";
        sendEmail(to, subject, body);
    }

    public void sendPasswordResetEmail(String to, String fullName, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset your password";
        String body = "Hello, " + fullName + "!\n\n"
                + "To reset your password, follow the link below:\n"
                + link + "\n\n"
                + "If you did not request a password reset, you can safely ignore this email.";
        sendEmail(to, subject, body);
    }

    public void sendProjectInvitationEmail(String to, String projectName, String inviterName, String token) {
        String link = frontendUrl + "/invitations/" + token;
        String subject = "Invitation to project \"" + projectName + "\"";
        String body = "Hello!\n\n"
                + inviterName + " invites you to join the project \"" + projectName + "\" on BugTracker.\n"
                + "To accept the invitation, follow the link below:\n"
                + link;
        sendEmail(to, subject, body);
    }
}
