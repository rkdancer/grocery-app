package com.example.grocery.service;

import com.example.grocery.entity.AuditLog;
import com.example.grocery.entity.User;
import com.example.grocery.repository.AuditLogRepository;
import com.example.grocery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(
            Long userId,
            String action,
            String target,
            Long targetId
    ) {
        if (userId == null) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        AuditLog log = AuditLog.builder()
                .userId(userId)
                .role(user.getRole())
                .action(action)
                .target(target)
                .targetId(targetId)
                .build();

        auditLogRepository.save(log);
    }
}
