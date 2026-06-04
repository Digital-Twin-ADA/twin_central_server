package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.ParticipantHeatmapPointDto;
import com.digitaltwin.central.dto.ParticipantLocationRequestDto;
import com.digitaltwin.central.dto.ParticipantLocationResponseDto;
import com.digitaltwin.central.service.ParticipantLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participant-locations")
@Tag(name = "Participant Locations", description = "Ingest each participant's current GPS location from the mobile app and expose latest locations for admin heatmaps.")
public class ParticipantLocationController {

    private final ParticipantLocationService participantLocationService;

    public ParticipantLocationController(ParticipantLocationService participantLocationService) {
        this.participantLocationService = participantLocationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit participant location",
            description = "Receives the current location of a participant from the mobile app. If the participant already exists, their previous location is updated so admin heatmaps count each participant once."
    )
    public ParticipantLocationResponseDto recordLocation(@Valid @RequestBody ParticipantLocationRequestDto dto) {
        return participantLocationService.recordLocation(dto);
    }

    @GetMapping
    @Operation(
            summary = "List participant locations",
            description = "Returns the latest known location for each participant for the admin web app. Use the minutes parameter to fetch only recently updated participants."
    )
    public List<ParticipantLocationResponseDto> getLocations(
            @Parameter(description = "Only return locations recorded in the last N minutes. Omit to return all stored locations.")
            @RequestParam(required = false) Integer minutes
    ) {
        return participantLocationService.getLocations(minutes);
    }

    @GetMapping("/heatmap")
    @Operation(
            summary = "Get participant heatmap points",
            description = "Returns heatmap-ready latest participant location points for the admin dashboard. Each participant is counted once and each point contains latitude, longitude, optional stage/zone metadata, timestamp, and weight."
    )
    public List<ParticipantHeatmapPointDto> getHeatmap(
            @Parameter(description = "Only return heatmap points recorded in the last N minutes. Omit to return all stored points.")
            @RequestParam(required = false) Integer minutes
    ) {
        return participantLocationService.getHeatmap(minutes);
    }
}
