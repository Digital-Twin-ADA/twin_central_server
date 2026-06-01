package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.LineupEventResponseDto;
import com.digitaltwin.central.service.LineupService;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@Tag(name = "Lineup", description = "Retrieve the festival lineup ordered by time. Use to display the scheduled performances in apps and on screens.")
@RequestMapping("/api/lineup")
public class LineupController {

    private final LineupService lineupService;

    public LineupController(LineupService lineupService) {
        this.lineupService = lineupService;
    }

    @GetMapping
    @Operation(summary = "Get lineup", description = "Return the full performance lineup ordered by start time. Use for schedule displays and planning.")
    public List<LineupEventResponseDto> getLineup() {
        return lineupService.getLineup();
    }
}
