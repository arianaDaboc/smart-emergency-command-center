package com.smartemergency.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.smartemergency.config.JwtService;
import com.smartemergency.domain.User;
import com.smartemergency.repo.UserRepository;
import com.smartemergency.service.AuditService;
import com.smartemergency.service.CurrentUser;
import com.smartemergency.web.Dto.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final CurrentUser current;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit,
                          CurrentUser current) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.current = current;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        User u = users.findByUsername(req.username().trim()).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            audit.log(req.username(), ip, "LOGIN_FAILED", "POST", "/api/auth/login", 401, null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong username or password");
        }
        if (!u.isEnabled()) {
            audit.log(u.getUsername(), ip, "LOGIN_FAILED", "POST", "/api/auth/login", 403, null);
            throw new ApiException(HttpStatus.FORBIDDEN, "Your account is disabled");
        }
        u.setLastLogin(Instant.now());
        users.save(u);
        audit.log(u.getUsername(), ip, "LOGIN", "POST", "/api/auth/login", 200, null);
        return new LoginResponse(jwt.create(u), u.getId(), u.getUsername(), u.getFullName(), u.getRole());
    }

    @GetMapping("/me")
    public LoginResponse me() {
        User u = current.get();
        return new LoginResponse(null, u.getId(), u.getUsername(), u.getFullName(), u.getRole());
    }
}
