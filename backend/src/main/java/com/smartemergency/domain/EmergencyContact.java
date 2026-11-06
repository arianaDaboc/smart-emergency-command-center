package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "emergency_contact")
@Getter @Setter @NoArgsConstructor
public class EmergencyContact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id")
    private Long patientId;
    private String name;
    private String relationship;
    private String phone;
}
