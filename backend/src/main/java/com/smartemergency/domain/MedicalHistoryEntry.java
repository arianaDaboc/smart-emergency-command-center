package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "medical_history")
@Getter @Setter @NoArgsConstructor
public class MedicalHistoryEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id")
    private Long patientId;
    private Instant eventDate = Instant.now();
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id")
    private User doctor;
    @Column(name = "incident_id")
    private Long incidentId;
    private String diagnosis;
    private String treatment;
}
