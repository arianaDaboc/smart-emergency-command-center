package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A resource assigned to an incident. For ambulances it also represents the patient transport. */
@Entity
@Table(name = "assignment")
@Getter @Setter @NoArgsConstructor
public class Assignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_id")
    private Incident incident;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resource_id")
    private Resource resource;
    /** True while the resource is engaged for this incident. */
    private boolean active = true;
    private Instant assignedAt = Instant.now();
    private Instant releasedAt;
    @Enumerated(EnumType.STRING)
    private TransportStatus transportStatus;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_hospital_id")
    private Hospital destinationHospital;
    private String handoverNote;
}
