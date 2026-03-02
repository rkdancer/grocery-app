package com.example.grocery.service;

import com.example.grocery.dto.RegisterRequest;
import com.example.grocery.entity.User;
import com.example.grocery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest req) {

        if (req.getUsername() == null || req.getUsername().trim().isEmpty())
            throw new RuntimeException("Username is required");

        if (req.getEmail() == null || req.getEmail().trim().isEmpty())
            throw new RuntimeException("Email is required");

        if (req.getPassword() == null || req.getPassword().trim().isEmpty())
            throw new RuntimeException("Password is required");

        if (req.getRole() == null || req.getRole().trim().isEmpty())
            throw new RuntimeException("Role is required");

        // normalize
        String username = req.getUsername().trim();
        String email = req.getEmail().trim(); // ไม่จำเป็นต้อง lower เพราะ repo ใช้ IgnoreCase อยู่แล้ว
        String role = req.getRole().trim().toUpperCase();

        // allow only ADMIN / OWNER
        if (!role.equals("ADMIN") && !role.equals("OWNER")) {
            throw new RuntimeException("Role must be ADMIN or OWNER");
        }

        // ✅ ใช้เมธอดล่าสุดของคุณ
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .build();

        return userRepository.save(user);
    }
}
