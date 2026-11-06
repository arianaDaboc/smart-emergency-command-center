package com.smartemergency.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.smartemergency.domain.User;
import com.smartemergency.service.AuditService;

import java.io.IOException;

/** Writes every API request in the audit log (login attempts are logged by the AuthController). */
public class AuditFilter extends OncePerRequestFilter {
    private final AuditService audit;

    public AuditFilter(AuditService audit) { this.audit = audit; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        return !p.startsWith("/api/") || p.startsWith("/api/auth/login") || "OPTIONS".equals(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long t0 = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.getPrincipal() instanceof User u) ? u.getUsername() : null;
            try {
                audit.log(username, clientIp(req), "REQUEST", req.getMethod(), req.getRequestURI(),
                        res.getStatus(), System.currentTimeMillis() - t0);
            } catch (Exception ignored) {
                // the audit log must never break a request
            }
        }
    }

    static String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return fwd != null && !fwd.isBlank() ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
