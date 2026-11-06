package com.smartemergency.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.smartemergency.repo.UserRepository;
import com.smartemergency.service.AuditService;

import java.util.List;

/** Role-based access: every /api/<role>/** path is reserved to one role. */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UserRepository users,
                                           AuditService audit) throws Exception {
        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwt, users);
        http.csrf(c -> c.disable())
            .cors(c -> c.configurationSource(cors()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/patient/**").hasRole("PATIENT")
                    .requestMatchers("/api/dispatcher/**").hasRole("DISPATCHER")
                    .requestMatchers("/api/crew/**").hasRole("CREW")
                    .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll())
            .exceptionHandling(e -> e
                    .authenticationEntryPoint((req, res, ex) -> {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"message\":\"Not authenticated\"}");
                    })
                    .accessDeniedHandler((req, res, ex) -> {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"message\":\"Access denied for your role\"}");
                    }))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new AuditFilter(audit), JwtAuthFilter.class);
        return http.build();
    }

    private CorsConfigurationSource cors() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("http://localhost:5173"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }
}
