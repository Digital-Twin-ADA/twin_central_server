package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.PointOfInterestResponseDto;
import com.digitaltwin.central.service.PointOfInterestService;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@RestController
@Tag(name = "Points of Interest", description = "Manage and query points of interest (POIs) such as food stalls, facilities, and services. Use to populate maps and info screens.")
@RequestMapping("/api/points-of-interest")
public class PointOfInterestController {

    private final PointOfInterestService pointOfInterestService;

    public PointOfInterestController(PointOfInterestService pointOfInterestService) {
        this.pointOfInterestService = pointOfInterestService;
    }

    @GetMapping
    @Operation(summary = "List points of interest", description = "Return POIs optionally filtered by type (e.g., food, restroom). Use to populate maps and find nearby services.")
    public List<PointOfInterestResponseDto> getAll(@Parameter(description = "Optional POI type to filter") @RequestParam(required = false) String type) {
        return pointOfInterestService.getAll(type);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get point of interest", description = "Fetch details for a POI by ID. Use to show detailed information on a map or info panel.")
    public PointOfInterestResponseDto getById(@Parameter(description = "POI ID") @PathVariable Long id) {
        return pointOfInterestService.getById(id);
    }
}
