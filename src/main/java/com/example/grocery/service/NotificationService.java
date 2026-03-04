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

    // ✅ ใช้ตัวเดียวกับ OTP (ตอนนี้เป็น Brevo API)
    private final ResendEmailService resendEmailService;

    private final AuditLogService auditLogService;

    public void sendLowStockEmailToOwners(Long authUserId, List<Long> ownerIds) {

        List<Product> lowStockProducts = productRepository.findAll().stream()
                .filter(p -> p.getStockQty() != null && p.getStockQty() <= LOW_STOCK_LIMIT)
                .toList();

        if (lowStockProducts.isEmpty()) {
            return;
        }

        List<User> owners = userRepository.findAllById(ownerIds).stream()
                .filter(u -> "OWNER".equalsIgnoreCase(u.getRole()))
                .toList();

        if (owners.isEmpty()) {
            throw new RuntimeException("ไม่พบ OWNER ที่เลือก");
        }

        for (User owner : owners) {
            resendEmailService.sendLowStockEmail(owner.getEmail(), lowStockProducts, LOW_STOCK_LIMIT);
        }

        auditLogService.log(authUserId, "SEND_EMAIL", "EMAIL", null);
    }
}