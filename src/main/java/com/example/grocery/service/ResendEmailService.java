package com.example.grocery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class ResendEmailService {

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${app.mail.resend.apiKey:${RESEND_API_KEY:}}")
    private String apiKey;

    @Value("${app.mail.resend.from:${RESEND_FROM:onboarding@resend.dev}}")
    private String from;

    public void sendText(String to, String subject, String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("RESEND_API_KEY ยังไม่ได้ตั้งค่า");
        }
        if (to == null || to.isBlank()) {
            throw new RuntimeException("Email ปลายทางว่าง");
        }

        // escape JSON แบบง่าย ๆ
        String safeSubject = jsonEscape(subject == null ? "" : subject);
        String safeText = jsonEscape(text == null ? "" : text);

        String body = """
            {
              "from": "%s",
              "to": ["%s"],
              "subject": "%s",
              "text": "%s"
            }
            """.formatted(from, to, safeSubject, safeText);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = resp.statusCode();
            if (code < 200 || code >= 300) {
                throw new RuntimeException("Resend ส่งเมลไม่สำเร็จ: HTTP " + code + " => " + resp.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Resend ส่งเมลไม่สำเร็จ: " + e.getMessage(), e);
        }
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}