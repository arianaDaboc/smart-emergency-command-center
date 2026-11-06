package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "medical_card")
@Getter @Setter @NoArgsConstructor
public class MedicalCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private User patient;
    private LocalDate dateOfBirth;
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String currentMedication;
    private String medicalNotes;
    private Double homeLat;
    private Double homeLng;
}
