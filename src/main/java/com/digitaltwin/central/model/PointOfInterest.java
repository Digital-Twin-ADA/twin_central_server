package com.digitaltwin.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "points_of_interest")
public class PointOfInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double latitude;

    private Double longitude;

    private String zoneCode;

    private String openingHours;

    public PointOfInterest() {
    }

    public PointOfInterest(String name, String type, String description, Double latitude, Double longitude,
                           String zoneCode, String openingHours) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoneCode = zoneCode;
        this.openingHours = openingHours;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public String getOpeningHours() {
        return openingHours;
    }
}
