package com.example.grocery.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class ResendEmailService {

    private final HttpClient http = HttpClient.newHttpClient();

    private final String apiKey = System.getenv("RESEND_API_KEY");
    private final String from = System.getenv().getOrDefault("RESEND_FROM", "onboarding@resend.dev");

    public void sendOtp(String toEmail, String otp, int expireMin) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Missing RESEND_API_KEY in environment");
        }

        String subject = "OTP สำหรับรีเซ็ตรหัสผ่าน";
        String text = "OTP ของคุณคือ: " + otp + "\nหมดอายุภายใน " + expireMin + " นาที";

        // JSON แบบง่าย (หลีกเลี่ยง dependency เพิ่ม)
        String json = "{"
                + "\"from\":\"" + escape(from) + "\","
                + "\"to\":[\"" + escape(toEmail) + "\"],"
                + "\"subject\":\"" + escape(subject) + "\","
                + "\"text\":\"" + escape(text) + "\""
                + "}";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("Resend send failed: HTTP " + resp.statusCode() + " => " + resp.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Resend send error: " + e.getMessage(), e);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}