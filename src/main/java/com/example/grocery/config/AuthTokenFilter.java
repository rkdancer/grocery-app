package com.example.grocery.config;

import com.example.grocery.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // ✅ public routes
        if (path.startsWith("/api/auth/")) return true;
        if (path.startsWith("/uploads/")) return true;

        // ✅ อนุญาต static pages (ถ้าคุณ serve ผ่าน spring)
        if (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js")) return true;

        // ที่เหลือให้ filter
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = extractBearer(authHeader);

        Long userId = tokenService.validateAndGetUserId(token);

        if (userId == null) {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {"success":false,"message":"Unauthorized: missing/invalid token"}
            """);
            return;
        }

        // เผื่อใช้ใน controller/service ต่อไป
        request.setAttribute("authUserId", userId);

        filterChain.doFilter(request, response);
    }

    private static String extractBearer(String authHeader) {
        if (authHeader == null) return null;
        String h = authHeader.trim();
        if (h.toLowerCase().startsWith("bearer ")) return h.substring(7).trim();
        return null;
    }
}
