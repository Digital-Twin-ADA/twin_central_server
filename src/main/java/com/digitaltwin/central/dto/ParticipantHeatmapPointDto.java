package com.digitaltwin.central.dto;

import java.time.OffsetDateTime;

public class ParticipantHeatmapPointDto {

    private Double latitude;
    private Double longitude;
    private Long stageId;
    private String stageName;
    private String zoneCode;
    private OffsetDateTime recordedAt;
    private int weight;

    public ParticipantHeatmapPointDto(Double latitude, Double longitude, Long stageId, String stageName,
                                      String zoneCode, OffsetDateTime recordedAt, int weight) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.stageId = stageId;
        this.stageName = stageName;
        this.zoneCode = zoneCode;
        this.recordedAt = recordedAt;
        this.weight = weight;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Long getStageId() {
        return stageId;
    }

    public String getStageName() {
        return stageName;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public int getWeight() {
        return weight;
    }
}
