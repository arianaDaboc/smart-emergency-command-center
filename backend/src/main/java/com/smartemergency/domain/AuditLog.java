package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_at")
    private Instant at = Instant.now();
    private String username;
    private String ip;
    private String action;
    private String method;
    private String path;
    private Integer status;
    private Long durationMs;
}
