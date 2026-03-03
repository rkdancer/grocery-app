package com.example.grocery.service;

import com.example.grocery.dto.forgot.ForgotPasswordRequest;
import com.example.grocery.dto.forgot.ForgotPasswordResponse;
import com.example.grocery.dto.forgot.ResetPasswordRequest;
import com.example.grocery.dto.forgot.VerifyOtpRequest;
import com.example.grocery.dto.forgot.VerifyOtpResponse;
import com.example.grocery.entity.PasswordResetOtp;
import com.example.grocery.entity.User;
import com.example.grocery.repository.PasswordResetOtpRepository;
import com.example.grocery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ เปลี่ยนมาใช้ Resend
    private final ResendEmailService resendEmailService;

    private static final SecureRandom RNG = new SecureRandom();

    // OTP settings
    private static final int OTP_EXPIRE_MIN = 5;
    private static final int MAX_VERIFY_FAIL = 5;

    // Anti-spam
    private static final int COOLDOWN_SECONDS = 30;

    public ForgotPasswordResponse requestOtp(ForgotPasswordRequest req) {
        String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();

        if (email.isBlank()) {
            return ForgotPasswordResponse.builder()
                    .success(false)
                    .message("กรุณากรอกอีเมล")
                    .otpRefId(null)
                    .build();
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        // กัน enumeration
        if (userOpt.isEmpty()) {
            return ForgotPasswordResponse.builder()
                    .success(true)
                    .message("ถ้าอีเมลนี้มีอยู่ในระบบ ระบบได้ส่ง OTP ไปให้แล้ว")
                    .otpRefId(null)
                    .build();
        }

        User user = userOpt.get();

        PasswordResetOtp last = otpRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        if (last != null && last.getCreatedAt() != null) {
            Instant allowAt = last.getCreatedAt().plus(COOLDOWN_SECONDS, ChronoUnit.SECONDS);
            if (allowAt.isAfter(Instant.now())) {
                long waitSec = Math.max(1, ChronoUnit.SECONDS.between(Instant.now(), allowAt));
                return ForgotPasswordResponse.builder()
                        .success(false)
                        .message("คุณขอ OTP ถี่เกินไป กรุณารอประมาณ " + waitSec + " วินาที แล้วลองใหม่")
                        .otpRefId(null)
                        .build();
            }
        }

        int nextSendCount = 1;
        if (last != null && last.getSendCount() != null) {
            nextSendCount = last.getSendCount() + 1;
        }

        String otpPlain = genOtp5();

        PasswordResetOtp rec = PasswordResetOtp.builder()
                .userId(user.getId())
                .otpHash(passwordEncoder.encode(otpPlain))
                .sendCount(nextSendCount)
                .verifyFailCount(0)
                .used(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(OTP_EXPIRE_MIN, ChronoUnit.MINUTES))
                .resetToken(null)
                .build();

        rec = otpRepository.save(rec);

        // ✅ ส่งเมลผ่าน Resend
        sendOtpEmail(email, otpPlain);

        return ForgotPasswordResponse.builder()
                .success(true)
                .message("ถ้าอีเมลนี้มีอยู่ในระบบ ระบบได้ส่ง OTP ไปให้แล้ว")
                .otpRefId(rec.getId())
                .build();
    }

    public VerifyOtpResponse verifyOtp(VerifyOtpRequest req) {
        Long refId = req.getOtpRefId();
        String otp = req.getOtp() == null ? "" : req.getOtp().trim();

        if (refId == null || otp.isEmpty()) {
            return VerifyOtpResponse.builder()
                    .success(false)
                    .message("ข้อมูลไม่ครบ")
                    .build();
        }

        PasswordResetOtp rec = otpRepository.findById(refId)
                .orElseThrow(() -> new RuntimeException("OTP reference ไม่ถูกต้อง"));

        if (Boolean.TRUE.equals(rec.getUsed())) {
            return VerifyOtpResponse.builder().success(false).message("OTP ถูกใช้ไปแล้ว").build();
        }

        if (rec.getExpiresAt() != null && rec.getExpiresAt().isBefore(Instant.now())) {
            return VerifyOtpResponse.builder().success(false).message("OTP หมดอายุ").build();
        }

        int failCount = rec.getVerifyFailCount() == null ? 0 : rec.getVerifyFailCount();
        if (failCount >= MAX_VERIFY_FAIL) {
            return VerifyOtpResponse.builder().success(false).message("กรอก OTP ผิดเกินจำนวนที่กำหนด").build();
        }

        boolean ok = passwordEncoder.matches(otp, rec.getOtpHash());
        if (!ok) {
            rec.setVerifyFailCount(failCount + 1);
            otpRepository.save(rec);
            return VerifyOtpResponse.builder().success(false).message("OTP ไม่ถูกต้อง").build();
        }

        String resetToken = UUID.randomUUID().toString();
        rec.setResetToken(resetToken);
        otpRepository.save(rec);

        return VerifyOtpResponse.builder()
                .success(true)
                .message("ยืนยัน OTP สำเร็จ")
                .resetToken(resetToken)
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String token = req.getResetToken();
        String newPassword = req.getNewPassword();

        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("ข้อมูลไม่ครบ");
        }

        if (newPassword.trim().length() < 6) {
            throw new RuntimeException("รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร");
        }

        PasswordResetOtp rec = otpRepository.findTopByResetTokenOrderByCreatedAtDesc(token)
                .orElseThrow(() -> new RuntimeException("resetToken ไม่ถูกต้อง"));

        if (Boolean.TRUE.equals(rec.getUsed())) {
            throw new RuntimeException("token ถูกใช้ไปแล้ว");
        }

        if (rec.getExpiresAt() != null && rec.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("token หมดอายุ");
        }

        User user = userRepository.findById(rec.getUserId())
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        rec.setUsed(true);
        otpRepository.save(rec);
    }

    private static String genOtp5() {
        int n = RNG.nextInt(100000);
        return String.format("%05d", n);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        String subject = "OTP สำหรับรีเซ็ตรหัสผ่าน";
        String text = "OTP ของคุณคือ: " + otp + "\nหมดอายุภายใน " + OTP_EXPIRE_MIN + " นาที";
        resendEmailService.sendText(toEmail, subject, text);
    }
}