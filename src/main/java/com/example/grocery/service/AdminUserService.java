package com.example.grocery.service;

import com.example.grocery.entity.User;
import com.example.grocery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // =====================
    // LIST ALL USERS
    // =====================
    public List<Map<String, Object>> listUsers() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (User u : userRepository.findAll()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("enabled", u.getEnabled());
            result.add(m);
        }
        return result;
    }

    // =====================
    // EDIT USER (ADMIN)
    // =====================
    public void updateUser(Long adminId, Long userId, String username, String email, String password) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        // username
        if (username != null && !username.trim().isEmpty()) {
            String clean = username.trim();
            var other = userRepository.findByUsernameIgnoreCase(clean).orElse(null);
            if (other != null && !Objects.equals(other.getId(), u.getId())) {
                throw new RuntimeException("Username ถูกใช้งานแล้ว");
            }
            u.setUsername(clean);
        }

        // email
        if (email != null && !email.trim().isEmpty()) {
            String clean = email.trim().toLowerCase();
            var other = userRepository.findByEmailIgnoreCase(clean).orElse(null);
            if (other != null && !Objects.equals(other.getId(), u.getId())) {
                throw new RuntimeException("Email ถูกใช้งานแล้ว");
            }
            u.setEmail(clean);
        }

        // password (optional)
        if (password != null && !password.trim().isEmpty()) {
            String pw = password.trim();
            if (pw.length() < 6) {
                throw new RuntimeException("รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร");
            }
            u.setPassword(passwordEncoder.encode(pw));

            // เปลี่ยนรหัส -> revoke token เดิมทั้งหมด (ให้ login ใหม่)
            tokenService.revokeAllByUserId(u.getId());
        }

        userRepository.save(u);

        auditLogService.log(
                adminId,
                "ADMIN_EDIT_USER",
                "USER",
                u.getId()
        );
    }

    // =====================
    // DELETE USER (ADMIN)
    // =====================
    public void deleteUser(Long adminId, Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        String role = (u.getRole() == null) ? "" : u.getRole().trim().toUpperCase();

        // กันลบ admin คนสุดท้าย
        if ("ADMIN".equals(role)) {
            long enabledAdmins = userRepository.findAll().stream()
                    .filter(x -> x != null)
                    .filter(x -> "ADMIN".equalsIgnoreCase(String.valueOf(x.getRole())))
                    .filter(x -> x.getEnabled() == null || x.getEnabled())
                    .count();
            if (enabledAdmins <= 1) {
                throw new RuntimeException("ไม่สามารถลบ ADMIN คนสุดท้ายได้");
            }
        }

        // revoke token ก่อนลบ (กัน token เก่ายังใช้ได้)
        tokenService.revokeAllByUserId(u.getId());

        userRepository.delete(u);

        auditLogService.log(
                adminId,
                "ADMIN_DELETE_USER",
                "USER",
                userId
        );
    }

    // =====================
    // CHANGE ROLE (ADMIN)
    // =====================
    public void changeRole(Long adminId, Long userId, String role) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        String r = role == null ? "" : role.trim().toUpperCase();
        if (!List.of("ADMIN", "OWNER", "STAFF").contains(r)) {
            throw new RuntimeException("role ไม่ถูกต้อง");
        }

        u.setRole(r);
        userRepository.save(u);

        auditLogService.log(
                adminId,
                "ADMIN_CHANGE_ROLE",
                "USER",
                userId
        );
    }

    // =====================
    // TOGGLE ENABLED (ADMIN)
    // =====================
    public void toggleEnabled(Long adminId, Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        u.setEnabled(u.getEnabled() == null || !u.getEnabled());
        userRepository.save(u);

        // ปิดบัญชี -> revoke token ทั้งหมด
        if (u.getEnabled() != null && !u.getEnabled()) {
            tokenService.revokeAllByUserId(u.getId());
        }

        auditLogService.log(
                adminId,
                "ADMIN_TOGGLE_USER",
                "USER",
                userId
        );
    }
}