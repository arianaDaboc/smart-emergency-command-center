package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "incident")
@Getter @Setter @NoArgsConstructor
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private IncidentType type;
    @Enumerated(EnumType.STRING)
    private Severity severity;
    private int affectedPeople;
    private String description;
    private double lat;
    private double lng;
    private boolean locationApprox;
    @Enumerated(EnumType.STRING)
    private IncidentSource source;
    @Enumerated(EnumType.STRING)
    private IncidentStatus status = IncidentStatus.NEW;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private User patient;
    private Instant createdAt = Instant.now();
    private Instant firstDispatchedAt;
    private Instant closedAt;
    private String closingNote;
    private String cancelReason;
}
