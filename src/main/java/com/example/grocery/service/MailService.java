package com.example.grocery.service;

import com.example.grocery.entity.Product;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendLowStockEmail(String to, List<Product> products) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

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
                    .append(p.getSellPrice())
                    .append("</td>")
                    .append("</tr>");
        }

        sb.append("</table>");

        sb.append("<p style='margin-top:20px;color:#777;'>")
                .append("ระบบ Stock ร้านสุขใจ")
                .append("</p>");

        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }
}
