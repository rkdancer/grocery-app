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

    /**
     * ปิดได้ชั่วคราวเพื่อให้ deploy ผ่าน (Render ยังไม่ตั้งค่า SMTP)
     * เปิดได้ด้วย env var: APP_MAIL_ENABLED=true
     */
    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendLowStockEmail(String to, List<Product> products) {
        if (!mailEnabled) {
            log.info("Mail feature disabled (app.mail.enabled=false). Skip sending email to {}", to);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            // เปิด mail แล้ว แต่ไม่มี JavaMailSender = config ฝั่ง SMTP ยังไม่ครบ
            throw new IllegalStateException(
                    "Mail is enabled (app.mail.enabled=true) but JavaMailSender bean is missing. " +
                            "Please configure spring.mail.* (SMTP) or disable mail."
            );
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("⚠️ แจ้งเตือนสินค้าใกล้หมด - ร้านสุขใจ");
            helper.setText(buildHtml(products), true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("ส่งอีเมลไม่สำเร็จ", e);
        }
    }

    private String buildHtml(List<Product> products) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html>");
        sb.append("<body style='font-family:Kanit,sans-serif;'>");
        sb.append("<h2 style='color:#d32f2f;'>⚠️ สินค้าใกล้หมด (คงเหลือ ≤ 5)</h2>");

        sb.append("<table border='1' cellpadding='8' cellspacing='0' ")
                .append("style='border-collapse:collapse;width:100%;'>");

        sb.append("<tr style='background:#ffe0e0;'>")
                .append("<th align='left'>สินค้า</th>")
                .append("<th align='center'>คงเหลือ</th>")
                .append("<th align='right'>ราคาขาย</th>")
                .append("</tr>");

        for (Product p : products) {
            sb.append("<tr>")
                    .append("<td>").append(p.getName()).append("</td>")
                    .append("<td align='center' style='color:red;font-weight:bold;'>")
                    .append(p.getStockQty())
                    .append("</td>")
                    .append("<td align='right'>")
                    .append(p.getPrice())
                    .append("</td>")
                    .append("</tr>");
        }

        sb.append("</table>");
        sb.append("<p>กรุณาตรวจสอบและเติมสินค้าโดยเร็ว</p>");
        sb.append("</body></html>");

        return sb.toString();
    }
}