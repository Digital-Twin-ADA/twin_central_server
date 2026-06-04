package com.digitaltwin.central.dto;

import java.time.OffsetDateTime;

public class ParticipantLocationResponseDto {

    private Long id;
    private String participantId;
    private Long stageId;
    private String stageName;
    private Double latitude;
    private Double longitude;
    private String zoneCode;
    private OffsetDateTime recordedAt;

    public ParticipantLocationResponseDto(Long id, String participantId, Long stageId, String stageName,
                                          Double latitude, Double longitude, String zoneCode,
                                          OffsetDateTime recordedAt) {
        this.id = id;
        this.participantId = participantId;
        this.stageId = stageId;
        this.stageName = stageName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoneCode = zoneCode;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public String getParticipantId() {
        return participantId;
    }

    public Long getStageId() {
        return stageId;
    }

    public String getStageName() {
        return stageName;
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

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }
}
