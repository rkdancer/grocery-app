package com.example.grocery.controller;

import com.example.grocery.entity.User;
import com.example.grocery.repository.UserRepository;
import com.example.grocery.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserRepository userRepository;

    // =====================
    // helper: check ADMIN
    // =====================
    private User requireAdmin(Long authUserId) {
        User me = userRepository.findById(authUserId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        if (!"ADMIN".equalsIgnoreCase(me.getRole())) {
            throw new RuntimeException("เฉพาะ ADMIN เท่านั้น");
        }
        return me;
    }

    // =====================
    // LIST USERS
    // =====================
    @GetMapping("/users")
    public List<Map<String, Object>> listUsers(
            @RequestAttribute("authUserId") Long authUserId
    ) {
        requireAdmin(authUserId);
        return adminUserService.listUsers();
    }

    // =====================
    // EDIT USER (username/email/password)
    // body: { "username": "...", "email":"...", "password":"..." }
    // =====================
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(
            @RequestAttribute("authUserId") Long authUserId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        requireAdmin(authUserId);

        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        adminUserService.updateUser(authUserId, id, username, email, password);
        return Map.of("success", true);
    }

    // =====================
    // DELETE USER
    // =====================
    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(
            @RequestAttribute("authUserId") Long authUserId,
            @PathVariable Long id
    ) {
        User me = requireAdmin(authUserId);

        if (me.getId().equals(id)) {
            throw new RuntimeException("ไม่สามารถลบบัญชีตัวเองได้");
        }

        adminUserService.deleteUser(authUserId, id);
        return Map.of("success", true);
    }

    // =====================
    // CHANGE ROLE
    // =====================
    @PutMapping("/users/{id}/role")
    public Map<String, Object> changeRole(
            @RequestAttribute("authUserId") Long authUserId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        User me = requireAdmin(authUserId);

        if (me.getId().equals(id)) {
            throw new RuntimeException("ไม่สามารถเปลี่ยน role ของตัวเองได้");
        }

        adminUserService.changeRole(authUserId, id, body.get("role"));
        return Map.of("success", true);
    }

    // =====================
    // ENABLE / DISABLE
    // =====================
    @PutMapping("/users/{id}/enabled")
    public Map<String, Object> toggleEnabled(
            @RequestAttribute("authUserId") Long authUserId,
            @PathVariable Long id
    ) {
        User me = requireAdmin(authUserId);

        if (me.getId().equals(id)) {
            throw new RuntimeException("ไม่สามารถปิดบัญชีตัวเองได้");
        }

        adminUserService.toggleEnabled(authUserId, id);
        return Map.of("success", true);
    }
}