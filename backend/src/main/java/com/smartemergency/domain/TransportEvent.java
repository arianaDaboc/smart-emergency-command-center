package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "transport_event")
@Getter @Setter @NoArgsConstructor
public class TransportEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "assignment_id")
    private Long assignmentId;
    @Enumerated(EnumType.STRING)
    private TransportStatus status;
    @Column(name = "event_at")
    private Instant at = Instant.now();

    public TransportEvent(Long assignmentId, TransportStatus status) {
        this.assignmentId = assignmentId; this.status = status;
    }
}
