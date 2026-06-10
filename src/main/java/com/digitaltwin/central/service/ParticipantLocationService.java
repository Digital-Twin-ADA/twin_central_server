package com.digitaltwin.central.service;

import com.digitaltwin.central.dto.ParticipantHeatmapPointDto;
import com.digitaltwin.central.dto.ParticipantLocationRequestDto;
import com.digitaltwin.central.dto.ParticipantLocationResponseDto;
import com.digitaltwin.central.dto.AlertRequestDto;
import com.digitaltwin.central.model.ParticipantLocation;
import com.digitaltwin.central.model.Stage;
import com.digitaltwin.central.repository.AlertRepository;
import com.digitaltwin.central.repository.ParticipantLocationRepository;
import com.digitaltwin.central.repository.StageRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParticipantLocationService {

    private static final int LIVE_HEATMAP_WINDOW_MINUTES = 10;

    private final ParticipantLocationRepository participantLocationRepository;
    private final StageRepository stageRepository;
    private final AlertRepository alertRepository;
    private final HeatmapPublisher heatmapPublisher;
    private final AlertService alertService;

    public ParticipantLocationService(ParticipantLocationRepository participantLocationRepository,
                                      StageRepository stageRepository,
                                      AlertRepository alertRepository,
                                      HeatmapPublisher heatmapPublisher,
                                      AlertService alertService) {
        this.participantLocationRepository = participantLocationRepository;
        this.stageRepository = stageRepository;
        this.alertRepository = alertRepository;
        this.heatmapPublisher = heatmapPublisher;
        this.alertService = alertService;
    }

    public ParticipantLocationResponseDto recordLocation(ParticipantLocationRequestDto dto) {
        ParticipantLocation saved = saveLocation(dto);
        updateStageCrowdsAndAlerts();
        publishLiveHeatmap();
        return toResponse(saved);
    }

    public List<ParticipantLocationResponseDto> recordLocations(List<ParticipantLocationRequestDto> dtos) {
        List<ParticipantLocationResponseDto> savedLocations = dtos.stream()
                .map(this::saveLocation)
                .map(this::toResponse)
                .toList();
        updateStageCrowdsAndAlerts();
        publishLiveHeatmap();
        return savedLocations;
    }

    private ParticipantLocation saveLocation(ParticipantLocationRequestDto dto) {
        Stage stage = null;
        if (dto.getStageId() != null) {
            stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new RuntimeException("Stage not found with id: " + dto.getStageId()));
        }

        ParticipantLocation location = participantLocationRepository.findByParticipantId(dto.getParticipantId())
                .orElseGet(ParticipantLocation::new);
        location.setParticipantId(dto.getParticipantId());
        location.setStage(stage);
        location.setLatitude(dto.getLatitude());
        location.setLongitude(dto.getLongitude());
        location.setZoneCode(dto.getZoneCode());
        location.setRecordedAt(dto.getRecordedAt() != null ? dto.getRecordedAt() : OffsetDateTime.now());

        return participantLocationRepository.save(location);
    }

    public List<ParticipantLocationResponseDto> getLocations(Integer minutes) {
        List<ParticipantLocation> locations = minutes == null
                ? participantLocationRepository.findAllByOrderByRecordedAtDesc()
                : participantLocationRepository.findByRecordedAtGreaterThanEqualOrderByRecordedAtDesc(
                        OffsetDateTime.now().minusMinutes(minutes));

        return locations.stream().map(this::toResponse).toList();
    }

    public List<ParticipantHeatmapPointDto> getHeatmap(Integer minutes) {
        List<ParticipantLocation> locations = minutes == null
                ? participantLocationRepository.findAllByOrderByRecordedAtDesc()
                : participantLocationRepository.findByRecordedAtGreaterThanEqualOrderByRecordedAtDesc(
                        OffsetDateTime.now().minusMinutes(minutes));

        return locations.stream().map(location -> {
            Stage stage = location.getStage();
            return new ParticipantHeatmapPointDto(
                    location.getLatitude(),
                    location.getLongitude(),
                    stage != null ? stage.getId() : null,
                    stage != null ? stage.getName() : null,
                    location.getZoneCode(),
                    location.getRecordedAt(),
                    1
            );
        }).toList();
    }

    private ParticipantLocationResponseDto toResponse(ParticipantLocation location) {
        Stage stage = location.getStage();
        return new ParticipantLocationResponseDto(
                location.getId(),
                location.getParticipantId(),
                stage != null ? stage.getId() : null,
                stage != null ? stage.getName() : null,
                location.getLatitude(),
                location.getLongitude(),
                location.getZoneCode(),
                location.getRecordedAt()
        );
    }

    private void publishLiveHeatmap() {
        try {
            heatmapPublisher.publish(getHeatmap(LIVE_HEATMAP_WINDOW_MINUTES));
        } catch (Exception ignored) {
            // Location ingestion should continue even if WebSocket clients are unavailable.
        }
    }

    private void updateStageCrowdsAndAlerts() {
        Map<Long, Long> crowdByStageId = participantLocationRepository.findAll().stream()
                .map(ParticipantLocation::getStage)
                .filter(stage -> stage != null && stage.getId() != null)
                .collect(Collectors.groupingBy(Stage::getId, Collectors.counting()));

        stageRepository.findAll().forEach(stage -> {
            boolean wasOvercrowded = stage.isOvercrowded();
            int previousCrowd = stage.getCurrentCrowd();
            int currentCrowd = crowdByStageId.getOrDefault(stage.getId(), 0L).intValue();
            boolean nowOvercrowded = currentCrowd >= stage.getCapacity();

            stage.setCurrentCrowd(currentCrowd);
            stage.setOvercrowded(nowOvercrowded);
            stageRepository.save(stage);

            if (shouldCreateOvercrowdAlert(stage, wasOvercrowded, previousCrowd, currentCrowd, nowOvercrowded)) {
                createOvercrowdAlert(stage);
            }
        });
    }

    private boolean shouldCreateOvercrowdAlert(Stage stage,
                                               boolean wasOvercrowded,
                                               int previousCrowd,
                                               int currentCrowd,
                                               boolean nowOvercrowded) {
        if (!nowOvercrowded) {
            return false;
        }

        if (!wasOvercrowded) {
            return true;
        }

        if (currentCrowd > previousCrowd) {
            return true;
        }

        return !alertRepository.existsByStageIdAndTypeAndResolvedFalse(stage.getId(), "OVER_CROWD");
    }

    private void createOvercrowdAlert(Stage stage) {
        AlertRequestDto dto = new AlertRequestDto();
        dto.setStageId(stage.getId());
        dto.setType("OVER_CROWD");
        dto.setMessage("Stage " + stage.getName() + " is overcrowded: " + stage.getCurrentCrowd() + "/" + stage.getCapacity());
        dto.setSeverity("HIGH");
        alertService.createAlert(dto);
    }
}
