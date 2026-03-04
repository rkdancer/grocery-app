package com.example.grocery.service;

import com.example.grocery.entity.Product;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
public class ResendEmailService {

    private final HttpClient http = HttpClient.newHttpClient();

    private final String apiKey = System.getenv("RESEND_API_KEY");

    // ตั้งค่า default ให้ก่อน (แนะนำให้ override ด้วย env บน Render)
    private final String from = System.getenv().getOrDefault("RESEND_FROM", "onboarding@resend.dev");

    public void sendOtp(String toEmail, String otp, int expireMin) {
        requireApiKey();

        String subject = "OTP สำหรับรีเซ็ตรหัสผ่าน";
        String text = "OTP ของคุณคือ: " + otp + "\nหมดอายุภายใน " + expireMin + " นาที";

        String json = "{"
                + "\"from\":\"" + escape(from) + "\","
                + "\"to\":[\"" + escape(toEmail) + "\"],"
                + "\"subject\":\"" + escape(subject) + "\","
                + "\"text\":\"" + escape(text) + "\""
                + "}";

        postToResend(json);
    }

    // ✅ NEW: ส่งเมลแจ้งสินค้าใกล้หมด (HTML)
    public void sendLowStockEmail(String toEmail, List<Product> products, int lowStockLimit) {
        requireApiKey();

        String subject = "⚠️ แจ้งเตือนสินค้าใกล้หมด - ร้านสุขใจ";

        String html = buildLowStockHtml(products, lowStockLimit);

        // เผื่อ client บางตัวไม่รองรับ html
        String text = buildLowStockText(products, lowStockLimit);

        String json = "{"
                + "\"from\":\"" + escape(from) + "\","
                + "\"to\":[\"" + escape(toEmail) + "\"],"
                + "\"subject\":\"" + escape(subject) + "\","
                + "\"text\":\"" + escape(text) + "\","
                + "\"html\":\"" + escape(html) + "\""
                + "}";

        postToResend(json);
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Missing RESEND_API_KEY in environment");
        }
    }

    private void postToResend(String json) {
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

    private String buildLowStockText(List<Product> products, int lowStockLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ สินค้าใกล้หมด (คงเหลือ ≤ ").append(lowStockLimit).append(")\n\n");
        for (Product p : products) {
            sb.append("- ").append(nullSafe(p.getName()))
                    .append(" | คงเหลือ: ").append(nullSafe(p.getStockQty()))
                    .append(" | ราคาขาย: ").append(nullSafe(p.getSellPrice()))
                    .append("\n");
        }
        sb.append("\nระบบ Stock ร้านสุขใจ");
        return sb.toString();
    }

    private String buildLowStockHtml(List<Product> products, int lowStockLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Kanit,sans-serif;'>");
        sb.append("<h2 style='color:#d32f2f;'>⚠️ สินค้าใกล้หมด (คงเหลือ ≤ ").append(lowStockLimit).append(")</h2>");
        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;'>");
        sb.append("<tr style='background:#ffe0e0;'>")
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
        sb.append("<p style='margin-top:20px;color:#777;'>ระบบ Stock ร้านสุขใจ</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String nullSafe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    // JSON escape
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}