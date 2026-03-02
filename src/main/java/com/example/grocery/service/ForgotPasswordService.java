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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;

    private static final SecureRandom RNG = new SecureRandom();

    // OTP settings
    private static final int OTP_EXPIRE_MIN = 5;
    private static final int MAX_VERIFY_FAIL = 5;

    // Anti-spam (ง่าย ๆ)
    // หมายเหตุ: ถ้าคุณต้องการ rate limit แบบจริงจัง แนะนำทำเพิ่มที่ API gateway / filter หรือใช้ Redis
    private static final int COOLDOWN_SECONDS = 30; // ขอ OTP ซ้ำได้ทุก 30 วิ ต่อ user

    /**
     * ขอ OTP เพื่อรีเซ็ตรหัสผ่าน
     * - ใช้ findByEmailIgnoreCase ให้ตรงกับ UserRepository ล่าสุดของคุณ
     * - ป้องกัน enumeration: ไม่บอกว่า email มี/ไม่มีในระบบ (ตอบเหมือนกัน)
     * - กัน spam เบื้องต้น: cooldown ต่อ user
     */
    public ForgotPasswordResponse requestOtp(ForgotPasswordRequest req) {
        String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();

        if (email.isBlank()) {
            return ForgotPasswordResponse.builder()
                    .success(false)
                    .message("กรุณากรอกอีเมล")
                    .otpRefId(null)
                    .build();
        }

        // ✅ ใช้เมธอดล่าสุดของคุณ
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        // ✅ ป้องกัน user enumeration: ตอบเหมือนกัน ไม่บอกว่ามี/ไม่มี
        // แต่ถ้าไม่มี user ก็จบเลยแบบ success=true เพื่อไม่ให้เดาได้
        if (userOpt.isEmpty()) {
            return ForgotPasswordResponse.builder()
                    .success(true)
                    .message("ถ้าอีเมลนี้มีอยู่ในระบบ ระบบได้ส่ง OTP ไปให้แล้ว")
                    .otpRefId(null)
                    .build();
        }

        User user = userOpt.get();

        // ✅ กันยิงรัว: ดู record ล่าสุด แล้วเช็ค cooldown
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

        // ✅ นับ sendCount ต่อ user จาก record ล่าสุด (ถ้ามี)
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

        // ส่งเมล OTP
        sendOtpEmail(email, otpPlain);

        return ForgotPasswordResponse.builder()
                .success(true)
                .message("ถ้าอีเมลนี้มีอยู่ในระบบ ระบบได้ส่ง OTP ไปให้แล้ว")
                .otpRefId(rec.getId()) // ถ้าคุณไม่อยากให้ refId หลุดตอนกัน enumeration สามารถตั้งเป็น null ได้
                .build();
    }

    /**
     * ตรวจ OTP แล้วออก resetToken
     * - กัน NPE verifyFailCount
     * - เช็คหมดอายุ / used / fail limit
     */
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

        // ออก token
        String resetToken = UUID.randomUUID().toString();
        rec.setResetToken(resetToken);
        otpRepository.save(rec);

        return VerifyOtpResponse.builder()
                .success(true)
                .message("ยืนยัน OTP สำเร็จ")
                .resetToken(resetToken)
                .build();
    }

    /**
     * รีเซ็ตรหัสผ่านด้วย resetToken
     * - ทำเป็น transaction: เปลี่ยนรหัส + mark used ให้ครบชุด
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String token = req.getResetToken();
        String newPassword = req.getNewPassword();

        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("ข้อมูลไม่ครบ");
        }

        // แนะนำขั้นต่ำง่าย ๆ (ปรับตามต้องการ)
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
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("OTP สำหรับรีเซ็ตรหัสผ่าน");
        msg.setText("OTP ของคุณคือ: " + otp + "\nหมดอายุภายใน " + OTP_EXPIRE_MIN + " นาที");
        mailSender.send(msg);
    }
}
