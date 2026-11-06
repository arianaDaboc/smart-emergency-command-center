package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "medical_case")
@Getter @Setter @NoArgsConstructor
public class MedicalCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "assignment_id")
    private Long assignmentId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_id")
    private Incident incident;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private User patient;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id")
    private User doctor;
    private String diagnosis;
    private String treatment;
    private String medication;
    private String procedures;
    private String notes;
    @Enumerated(EnumType.STRING)
    private PatientStatus patientStatus;
    private boolean finalized;
    private Instant finalizedAt;
    private Instant updatedAt = Instant.now();
}
