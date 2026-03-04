package com.example.grocery.service;

import com.example.grocery.entity.Product;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromEmail;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendLowStockEmail(String to, List<Product> products) {
        if (!mailEnabled) {
            log.info("Mail disabled (app.mail.enabled=false). Skip sending email to {}", to);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException(
                    "Mail is enabled (app.mail.enabled=true) but JavaMailSender bean is missing. " +
                            "Check spring.mail.* SMTP config."
            );
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("⚠️ แจ้งเตือนสินค้าใกล้หมด - Grocery System");
            helper.setText(buildHtml(products), true);

            mailSender.send(message);
            log.info("Low stock email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("ส่งอีเมลไม่สำเร็จ", e);
        }
    }

    private String buildHtml(List<Product> products) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body style='font-family:Arial,sans-serif;'>");
        sb.append("<h2 style='color:#d32f2f;'>⚠️ สินค้าใกล้หมด (คงเหลือ ≤ 5)</h2>");

        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;'>");
        sb.append("<tr style='background:#f5f5f5;'>");
        sb.append("<th align='left'>สินค้า</th>");
        sb.append("<th align='center'>คงเหลือ</th>");
        sb.append("<th align='right'>ราคาขาย</th>");
        sb.append("</tr>");

        for (Product p : products) {
            sb.append("<tr>");
            sb.append("<td>").append(escapeHtml(p.getName())).append("</td>");
            sb.append("<td align='center' style='color:red;font-weight:bold;'>")
                    .append(p.getStockQty())
                    .append("</td>");
            sb.append("<td align='right'>")
                    .append(p.getSellPrice())
                    .append("</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("<p>กรุณาตรวจสอบและเติมสินค้าโดยเร็ว</p>");
        sb.append("</body></html>");

        return sb.toString();
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