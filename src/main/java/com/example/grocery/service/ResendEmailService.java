package com.example.grocery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final RestClient restClient;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromEmail;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    public ResendEmailService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    public void sendOtp(String toEmail, String otp, int expireMin) {
        if (!mailEnabled) {
            throw new RuntimeException("Mail feature is disabled. Set APP_MAIL_ENABLED=true");
        }
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new RuntimeException("Missing BREVO_API_KEY in environment");
        }

        String subject = "OTP สำหรับรีเซ็ตรหัสผ่าน";
        String text = "OTP ของคุณคือ: " + otp + "\nหมดอายุภายใน " + expireMin + " นาที";

        // Brevo payload (minimal)
        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", fromEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", text
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", brevoApiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("OTP email sent via Brevo API to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email via Brevo API to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Send OTP email failed: " + e.getMessage(), e);
        }
    }
}