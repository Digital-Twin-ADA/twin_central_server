package com.digitaltwin.central.dto;

import java.time.OffsetDateTime;

public class EventResponseDto {
    private Long id;
    private String type; // "lineup" or "spontaneous"
    private String title;
    private String description;
    private Long artistId;
    private String artistName;
    private String artistGenre;
    private Long stageId;
    private String stageName;
    private String stageZoneCode;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String status;

    public EventResponseDto() {}

    // Lineup constructor
    public EventResponseDto(Long id, Long artistId, String artistName, String artistGenre, Long stageId,
                            String stageName, String stageZoneCode, OffsetDateTime startsAt,
                            OffsetDateTime endsAt, String title, String status) {
        this.id = id;
        this.type = "lineup";
        this.artistId = artistId;
        this.artistName = artistName;
        this.artistGenre = artistGenre;
        this.stageId = stageId;
        this.stageName = stageName;
        this.stageZoneCode = stageZoneCode;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.title = title;
        this.status = status;
    }

    // Spontaneous constructor
    public EventResponseDto(Long id, String title, String description, Long stageId, String stageName, String stageZoneCode,
                            OffsetDateTime startsAt, OffsetDateTime endsAt, String status) {
        this.id = id;
        this.type = "spontaneous";
        this.title = title;
        this.description = description;
        this.stageId = stageId;
        this.stageName = stageName;
        this.stageZoneCode = stageZoneCode;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
    }

    // getters
    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getArtistId() { return artistId; }
    public String getArtistName() { return artistName; }
    public String getArtistGenre() { return artistGenre; }
    public Long getStageId() { return stageId; }
    public String getStageName() { return stageName; }
    public String getStageZoneCode() { return stageZoneCode; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public String getStatus() { return status; }
}