package com.digitaltwin.central.dto;

public class PointOfInterestResponseDto {

    private Long id;
    private String name;
    private String type;
    private String description;
    private Double latitude;
    private Double longitude;
    private String zoneCode;
    private String openingHours;

    public PointOfInterestResponseDto(Long id, String name, String type, String description, Double latitude,
                                      Double longitude, String zoneCode, String openingHours) {
        this.id = id;
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
