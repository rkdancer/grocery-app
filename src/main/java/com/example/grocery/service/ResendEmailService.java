package com.example.grocery.service;

import com.example.grocery.entity.Product;
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

    // ===== OTP (Forgot password) =====
    public void sendOtp(String toEmail, String otp, int expireMin) {
        if (!mailEnabled) {
            throw new RuntimeException("Mail feature is disabled. Set APP_MAIL_ENABLED=true");
        }
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new RuntimeException("Missing BREVO_API_KEY in environment");
        }

        String subject = "OTP สำหรับรีเซ็ตรหัสผ่าน";
        String text = "OTP ของคุณคือ: " + otp + "\nหมดอายุภายใน " + expireMin + " นาที";

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
            log.error("Failed to send OTP via Brevo API to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Send OTP email failed: " + e.getMessage(), e);
        }
    }

    // ===== NEW: Low-stock email =====
    public void sendLowStockEmail(String toEmail, List<Product> products, int lowStockLimit) {
        if (!mailEnabled) {
            throw new RuntimeException("Mail feature is disabled. Set APP_MAIL_ENABLED=true");
        }
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new RuntimeException("Missing BREVO_API_KEY in environment");
        }

        String subject = "⚠️ แจ้งเตือนสินค้าใกล้หมด (คงเหลือ ≤ " + lowStockLimit + ")";
        String text = buildLowStockText(products, lowStockLimit);
        String html = buildLowStockHtml(products, lowStockLimit);

        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", fromEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", text,
                "htmlContent", html
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", brevoApiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Low-stock email sent via Brevo API to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send low-stock via Brevo API to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Send low-stock email failed: " + e.getMessage(), e);
        }
    }

    private String buildLowStockText(List<Product> products, int lowStockLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ สินค้าใกล้หมด (คงเหลือ ≤ ").append(lowStockLimit).append(")\n\n");
        for (Product p : products) {
            sb.append("- ")
                    .append(nullSafe(p.getName()))
                    .append(" | คงเหลือ: ").append(nullSafe(p.getStockQty()))
                    .append(" | ราคาขาย: ").append(nullSafe(p.getSellPrice()))
                    .append("\n");
        }
        sb.append("\nGrocery System");
        return sb.toString();
    }

    private String buildLowStockHtml(List<Product> products, int lowStockLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Arial,sans-serif;'>");
        sb.append("<h2 style='color:#d32f2f;'>⚠️ สินค้าใกล้หมด (คงเหลือ ≤ ").append(lowStockLimit).append(")</h2>");

        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;'>");
        sb.append("<tr style='background:#f5f5f5;'>")
                .append("<th align='left'>สินค้า</th>")
                .append("<th align='center'>คงเหลือ</th>")
                .append("<th align='right'>ราคาขาย</th>")
                .append("</tr>");

        for (Product p : products) {
            sb.append("<tr>")
                    .append("<td>").append(escapeHtml(nullSafe(p.getName()))).append("</td>")
                    .append("<td align='center' style='color:red;font-weight:bold;'>")
                    .append(escapeHtml(nullSafe(p.getStockQty())))
                    .append("</td>")
                    .append("<td align='right'>")
                    .append(escapeHtml(nullSafe(p.getSellPrice())))
                    .append("</td>")
                    .append("</tr>");
        }

        sb.append("</table>");
        sb.append("<p style='margin-top:16px;color:#777;'>Grocery System</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String nullSafe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}