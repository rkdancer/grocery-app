package com.example.grocery.service;

import com.example.grocery.entity.Product;
import com.example.grocery.entity.User;
import com.example.grocery.repository.ProductRepository;
import com.example.grocery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int LOW_STOCK_LIMIT = 5;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final AuditLogService auditLogService; // ✅ เพิ่ม

    public void sendLowStockEmailToOwners(
            Long authUserId,              // ✅ คนที่กดส่ง
            List<Long> ownerIds
    ) {

        List<Product> lowStockProducts =
                productRepository.findAll().stream()
                        .filter(p -> p.getStockQty() != null && p.getStockQty() <= LOW_STOCK_LIMIT)
                        .toList();

        if (lowStockProducts.isEmpty()) {
            return; // ไม่มีสินค้าใกล้หมด = ไม่ต้องส่ง ไม่ต้อง log
        }

        List<User> owners = userRepository.findAllById(ownerIds).stream()
                .filter(u -> "OWNER".equalsIgnoreCase(u.getRole()))
                .toList();

        if (owners.isEmpty()) {
            throw new RuntimeException("ไม่พบ OWNER ที่เลือก");
        }

        for (User owner : owners) {
            mailService.sendLowStockEmail(owner.getEmail(), lowStockProducts);
        }

        // ✅ AUDIT LOG (1 ครั้งต่อการกดส่ง)
        auditLogService.log(
                authUserId,
                "SEND_EMAIL",
                "EMAIL",
                null
        );
    }
}
