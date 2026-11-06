package com.smartemergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resource")
@Getter @Setter @NoArgsConstructor
public class Resource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    @Enumerated(EnumType.STRING)
    private ResourceType type;
    @Enumerated(EnumType.STRING)
    private ResourceStatus status = ResourceStatus.AVAILABLE;
    private double lat;
    private double lng;
    /** Only for ambulances: the crew account of the ambulance. */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "crew_user_id")
    private User crewUser;

    public Resource(String code, ResourceType type, double lat, double lng) {
        this.code = code; this.type = type; this.lat = lat; this.lng = lng;
    }
}
