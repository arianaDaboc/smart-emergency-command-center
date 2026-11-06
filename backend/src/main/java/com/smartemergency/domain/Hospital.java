package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hospital")
@Getter @Setter @NoArgsConstructor
public class Hospital {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double lat;
    private double lng;

    public Hospital(String name, double lat, double lng) {
        this.name = name; this.lat = lat; this.lng = lng;
    }
}
