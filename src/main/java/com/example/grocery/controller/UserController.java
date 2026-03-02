package com.example.grocery.controller;

import com.example.grocery.dto.LoginRequest;
import com.example.grocery.dto.LoginResponse;
import com.example.grocery.dto.RegisterRequest;
import com.example.grocery.dto.RegisterResponse;
import com.example.grocery.entity.User;
import com.example.grocery.repository.UserRepository;
import com.example.grocery.service.AuthService;
import com.example.grocery.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    // =====================
    // AUTH (ของเดิม)
    // =====================
    @PostMapping("/api/auth/register")
    public RegisterResponse register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/api/auth/logout")
    public Map<String, Object> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String token = extractBearerToken(authorization);
        boolean revoked = tokenService.revoke(token);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", revoked ? "Logged out" : "No active token to revoke");
        return resp;
    }

    // =====================
    // USERS (OWNER)
    // =====================
    @GetMapping("/api/users/owners")
    public List<Map<String, Object>> listOwners() {

        List<Map<String, Object>> result = new ArrayList<>();

        for (User u : userRepository.findAll()) {
            if (!"OWNER".equalsIgnoreCase(u.getRole())) continue;

            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());

            result.add(m);
        }

        return result;
    }

    // =====================
    // helper
    // =====================
    private static String extractBearerToken(String authorization) {
        if (authorization == null) return "";
        String v = authorization.trim();
        if (v.toLowerCase().startsWith("bearer ")) {
            return v.substring(7).trim();
        }
        return "";
    }
}
