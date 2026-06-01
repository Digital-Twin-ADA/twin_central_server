package com.digitaltwin.central.controller;

import com.digitaltwin.central.model.Stage;
import com.digitaltwin.central.service.StageService;
import com.digitaltwin.central.dto.StageRequestDto;
import com.digitaltwin.central.dto.StageResponseDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@RestController
@Tag(name = "Stages", description = "Manage stages/venues: list, view details, and create stages. Use for venue configuration and capacity management.")
@RequestMapping("/api/stages")
public class StageController {

    private final StageService stageService;

    public StageController(StageService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    @Operation(summary = "List stages", description = "Return all configured stages with metadata like capacity and zone code. Use to populate maps and management UIs.")
    public List<StageResponseDto> getAllStages() {
        return stageService.getAllStages();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stage details", description = "Fetch stage information by ID including capacity and current crowd. Use for detailed stage pages.")
    public StageResponseDto getStageById(@Parameter(description = "Stage ID") @PathVariable Long id) {
        return stageService.getStageById(id);
    }

    @PostMapping
    @Operation(summary = "Create stage", description = "Create a new stage with configuration such as capacity and zone. Use from admin tools.")
    public StageResponseDto createStage(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Stage creation payload") @RequestBody StageRequestDto dto) {
        return stageService.createStage(dto);
    }
}