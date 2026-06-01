package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.FestivalInfoRequestDto;
import com.digitaltwin.central.dto.FestivalInfoResponseDto;
import com.digitaltwin.central.service.FestivalInfoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Festival Info", description = "Get and update global festival metadata such as name, dates, and contact info. Use for site-wide display and configuration.")
@RequestMapping("/api/festival")
public class FestivalInfoController {

    private final FestivalInfoService festivalInfoService;

    public FestivalInfoController(FestivalInfoService festivalInfoService) {
        this.festivalInfoService = festivalInfoService;
    }

    @GetMapping("/info")
    @Operation(summary = "Get festival info", description = "Retrieve global festival metadata for display on client apps or admin UIs.")
    public FestivalInfoResponseDto getInfo() {
        return festivalInfoService.getInfo();
    }

    @PutMapping("/info")
    @Operation(summary = "Update festival info", description = "Update global festival metadata. Use from admin UI or configuration scripts.")
    public FestivalInfoResponseDto saveInfo(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Festival info payload") @RequestBody FestivalInfoRequestDto dto) {
        return festivalInfoService.saveInfo(dto);
    }
}
