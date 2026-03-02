package com.example.grocery.service;

import com.example.grocery.dto.LoginRequest;
import com.example.grocery.dto.LoginResponse;
import com.example.grocery.dto.RegisterRequest;
import com.example.grocery.dto.RegisterResponse;
import com.example.grocery.entity.User;
import com.example.grocery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    private static final int ACCESS_TOKEN_TTL_MIN = 60;

    // รองรับ role
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_STAFF = "STAFF";

    public RegisterResponse register(RegisterRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        String password = req.getPassword() == null ? "" : req.getPassword();

        // default role = OWNER
        String role = req.getRole() == null ? ROLE_OWNER : req.getRole().trim().toUpperCase();

        if (username.isBlank()) {
            return RegisterResponse.builder().success(false).message("กรุณากรอก username").build();
        }
        if (email.isBlank()) {
            return RegisterResponse.builder().success(false).message("กรุณากรอกอีเมล").build();
        }
        if (password.isBlank()) {
            return RegisterResponse.builder().success(false).message("กรุณากรอกรหัสผ่าน").build();
        }

        // ✅ รองรับ STAFF เพิ่ม
        if (!role.equals(ROLE_ADMIN) && !role.equals(ROLE_OWNER) && !role.equals(ROLE_STAFF)) {
            return RegisterResponse.builder()
                    .success(false)
                    .message("role ต้องเป็น ADMIN หรือ OWNER หรือ STAFF")
                    .build();
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return RegisterResponse.builder().success(false).message("Username ถูกใช้งานแล้ว").build();
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return RegisterResponse.builder().success(false).message("Email ถูกใช้งานแล้ว").build();
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .lastLoginAt(null)
                .build();

        userRepository.save(user);

        return RegisterResponse.builder()
                .success(true)
                .message("Register successful")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public LoginResponse login(LoginRequest req) {
        String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        String password = req.getPassword() == null ? "" : req.getPassword();

        if (email.isBlank() || password.isBlank()) {
            throw new RuntimeException("กรุณากรอกอีเมลและรหัสผ่าน");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Email or password is incorrect"));

        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new RuntimeException("บัญชีถูกปิดการใช้งาน");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Email or password is incorrect");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        TokenService.IssuedToken tok = tokenService.issueToken(user.getId(), ACCESS_TOKEN_TTL_MIN);

        return LoginResponse.builder()
                .success(true)
                .message("Login successful")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(tok.getAccessToken())
                .expiresAt(tok.getExpiresAt())
                .build();
    }
}
