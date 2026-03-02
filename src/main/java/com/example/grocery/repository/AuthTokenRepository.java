package com.example.grocery.repository;

import com.example.grocery.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);

    Optional<AuthToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    // ✅ NEW: revoke all token by user
    List<AuthToken> findByUserIdAndRevokedAtIsNull(Long userId);
}