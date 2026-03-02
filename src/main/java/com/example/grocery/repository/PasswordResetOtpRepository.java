package com.example.grocery.repository;

import com.example.grocery.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PasswordResetOtp> findTopByResetTokenOrderByCreatedAtDesc(String resetToken);
}
