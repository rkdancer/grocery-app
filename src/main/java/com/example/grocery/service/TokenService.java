package com.example.grocery.service;

import com.example.grocery.entity.AuthToken;
import com.example.grocery.repository.AuthTokenRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final AuthTokenRepository authTokenRepository;

    @Data
    @AllArgsConstructor
    public static class IssuedToken {
        private String accessToken;
        private Instant expiresAt;
    }

    public IssuedToken issueToken(Long userId, int ttlMinutes) {
        String rawToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttlMinutes, ChronoUnit.MINUTES);

        AuthToken rec = AuthToken.builder()
                .userId(userId)
                .tokenHash(sha256(rawToken))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .revokedAt(null)
                .build();

        authTokenRepository.save(rec);
        return new IssuedToken(rawToken, expiresAt);
    }

    public Long validateAndGetUserId(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;

        String hash = sha256(rawToken);
        Optional<AuthToken> opt = authTokenRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash, Instant.now());

        return opt.map(AuthToken::getUserId).orElse(null);
    }

    public boolean revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;

        String hash = sha256(rawToken);
        AuthToken rec = authTokenRepository
                .findByTokenHashAndRevokedAtIsNull(hash)
                .orElse(null);

        if (rec == null) return false;

        rec.setRevokedAt(Instant.now());
        authTokenRepository.save(rec);
        return true;
    }

    // ✅ NEW: revoke token ทั้งหมดของ user
    public int revokeAllByUserId(Long userId) {
        if (userId == null) return 0;

        List<AuthToken> tokens = authTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        if (tokens == null || tokens.isEmpty()) return 0;

        Instant now = Instant.now();
        for (AuthToken t : tokens) {
            t.setRevokedAt(now);
        }
        authTokenRepository.saveAll(tokens);
        return tokens.size();
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}