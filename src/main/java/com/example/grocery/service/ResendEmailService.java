package com.example.grocery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromEmail;

    public ResendEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendOtp(String toEmail, String otp, int expireMin) {
        if (!mailEnabled) {
            throw new RuntimeException("Mail feature is disabled. Please set APP_MAIL_ENABLED=true");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new RuntimeException("JavaMailSender bean not found. Please configure SPRING_MAIL_* (SMTP).");
        }

        String subject = "OTP สำหรับรีเซ็ตรหัสผ่าน";
        String text = "OTP ของคุณคือ: " + otp + "\nหมดอายุภายใน " + expireMin + " นาที";

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(text);

            mailSender.send(msg);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Send OTP email failed: " + e.getMessage(), e);
        }
    }
}