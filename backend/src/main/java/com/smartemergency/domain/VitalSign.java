package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** One measurement of the Emergency Transport Record. */
@Entity
@Table(name = "vital_sign")
@Getter @Setter @NoArgsConstructor
public class VitalSign {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "assignment_id")
    private Long assignmentId;
    private Instant recordedAt = Instant.now();
    private Integer pulse;
    private Integer spo2;
    private Integer systolic;
    private Integer diastolic;
    private BigDecimal temperature;
    private BigDecimal oxygenLpm;
    private boolean iv;
    private String medication;
    private String interventions;
    @Enumerated(EnumType.STRING)
    @Column(name = "patient_condition")
    private PatientCondition condition;
    private String observations;
}
