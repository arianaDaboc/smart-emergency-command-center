package com.smartemergency.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.smartemergency.domain.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Creates and validates the JWT tokens (user id + role, valid 8 hours by default). */
@Service
public class JwtService {
    private final SecretKey key;
    private final Duration validity;

    public JwtService(@Value("${secc.jwt.secret}") String secret,
                      @Value("${secc.jwt.validity-hours:8}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validity = Duration.ofHours(hours);
    }

    public String create(User u) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("role", u.getRole().name())
                .claim("username", u.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /** Returns the user id from a valid token; throws on invalid/expired token. */
    public Long userId(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return Long.valueOf(c.getSubject());
    }
}
