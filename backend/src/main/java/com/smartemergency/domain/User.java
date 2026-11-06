package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_user")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String passwordHash;
    private String fullName;
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean enabled = true;
    /** Only for doctors: the hospital where the doctor works. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;
    private Instant lastLogin;
    private Instant createdAt = Instant.now();
}
