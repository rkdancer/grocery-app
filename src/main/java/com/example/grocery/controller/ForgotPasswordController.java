package com.example.grocery.controller;

import com.example.grocery.dto.forgot.*;
import com.example.grocery.service.ForgotPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth/forgot")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/request-otp")
    public ResponseEntity<ForgotPasswordResponse> requestOtp(@RequestBody ForgotPasswordRequest req) {
        ForgotPasswordResponse resp = forgotPasswordService.requestOtp(req);

        if (!resp.isSuccess()) {
            // ✅ 404 เมื่อไม่พบอีเมลในระบบ
            if ("ไม่พบอีเมลนี้ในระบบ".equals(resp.getMessage())) {
                return ResponseEntity.status(404).body(resp);
            }
            // ✅ 400 เมื่อกรอกไม่ครบ/ข้อมูลไม่ถูกต้อง
            return ResponseEntity.badRequest().body(resp);
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify-otp")
    public VerifyOtpResponse verifyOtp(@RequestBody VerifyOtpRequest req) {
        return forgotPasswordService.verifyOtp(req);
    }

    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestBody ResetPasswordRequest req) {
        forgotPasswordService.resetPassword(req);
        return Map.of("success", true, "message", "เปลี่ยนรหัสผ่านสำเร็จ");
    }
}
