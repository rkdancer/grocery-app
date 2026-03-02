package com.example.grocery.controller;

import com.example.grocery.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/low-stock-email")
    public ResponseEntity<?> sendLowStockEmail(
            @RequestBody Map<String, List<Long>> body,
            HttpServletRequest request
    ) {
        try {
            Long authUserId = (Long) request.getAttribute("authUserId");

            List<Long> ownerIds = body.get("ownerIds");
            if (ownerIds == null || ownerIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "กรุณาเลือกเจ้าของอย่างน้อย 1 คน"));
            }

            notificationService.sendLowStockEmailToOwners(authUserId, ownerIds);
            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
