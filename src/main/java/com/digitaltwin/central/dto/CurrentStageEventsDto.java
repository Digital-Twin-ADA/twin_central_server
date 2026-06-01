package com.digitaltwin.central.dto;

import java.util.List;

public class CurrentStageEventsDto {

    private Long stageId;
    private String stageName;
    private String stageZoneCode;
    private List<EventResponseDto> events;

    public CurrentStageEventsDto(Long stageId, String stageName, String stageZoneCode, List<EventResponseDto> events) {
        this.stageId = stageId;
        this.stageName = stageName;
        this.stageZoneCode = stageZoneCode;
        this.events = events;
    }

    public Long getStageId() {
        return stageId;
    }

    public String getStageName() {
        return stageName;
    }

    public String getStageZoneCode() {
        return stageZoneCode;
    }

    public List<EventResponseDto> getEvents() {
        return events;
    }
}
