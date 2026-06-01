package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.PointOfInterestResponseDto;
import com.digitaltwin.central.service.PointOfInterestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/points-of-interest")
public class PointOfInterestController {

    private final PointOfInterestService pointOfInterestService;

    public PointOfInterestController(PointOfInterestService pointOfInterestService) {
        this.pointOfInterestService = pointOfInterestService;
    }

    @GetMapping
    public List<PointOfInterestResponseDto> getAll(@RequestParam(required = false) String type) {
        return pointOfInterestService.getAll(type);
    }

    @GetMapping("/{id}")
    public PointOfInterestResponseDto getById(@PathVariable Long id) {
        return pointOfInterestService.getById(id);
    }
}
