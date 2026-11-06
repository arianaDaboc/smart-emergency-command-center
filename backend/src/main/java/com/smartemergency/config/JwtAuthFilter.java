package com.smartemergency.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.smartemergency.domain.User;
import com.smartemergency.repo.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads the "Authorization: Bearer <token>" header. The user is loaded from the database on every request,
 * so a disabled account is rejected immediately even if its token is still valid.
 */
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Optional<User> user;
            try {
                user = users.findById(jwt.userId(header.substring(7)));
            } catch (Exception e) {
                user = Optional.empty();
            }
            if (user.isEmpty() || !user.get().isEnabled()) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Session expired or account disabled\"}");
                return;
            }
            User u = user.get();
            var auth = new UsernamePasswordAuthenticationToken(u, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res);
    }
}
