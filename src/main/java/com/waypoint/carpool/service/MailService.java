package com.waypoint.carpool.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
    }

    /**
     * Sends the password-reset code. If real SMTP isn't configured
     * (app.mail.enabled=false, the default), logs the code instead of
     * failing the request — lets the whole flow be exercised locally
     * without setting up a mail server.
     */
    public void sendOtp(String toEmail, String otp, int expiryMinutes) {
        if (!enabled) {
            log.info("[DEV — email disabled] Password reset code for {}: {} (expires in {} min)", toEmail, otp, expiryMinutes);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Your Waypoint password reset code");
        message.setText(
                "Your password reset code is: " + otp + "\n\n" +
                "It expires in " + expiryMinutes + " minutes.\n\n" +
                "If you didn't request this, you can safely ignore this email."
        );
        mailSender.send(message);
    }
}
