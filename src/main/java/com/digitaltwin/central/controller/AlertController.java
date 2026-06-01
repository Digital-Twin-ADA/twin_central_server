package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.AlertRequestDto;
import com.digitaltwin.central.dto.AlertResponseDto;
import com.digitaltwin.central.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@RestController
@Tag(name = "Alerts", description = "Manage system alerts: list, create, resolve, and fetch specific alerts. Use when monitoring stage issues or notifying staff.")
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
        @Operation(summary = "List alerts", description = "Returns paginated alerts filtered by stageId, resolved status, and severity. Use to browse and monitor current alerts.")
        public org.springframework.data.domain.Page<AlertResponseDto> getAll(
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) String severity,
            org.springframework.data.domain.Pageable pageable
    ) {
        return alertService.findAlerts(stageId, resolved, severity, pageable);
    }

    @GetMapping("/{id}")
        @Operation(summary = "Get alert", description = "Fetch a single alert by its ID. Use when inspecting details about a specific alert or troubleshooting an incident.")
        public ResponseEntity<?> getById(@Parameter(description = "ID of the alert to retrieve") @PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @PostMapping
        @Operation(summary = "Create alert", description = "Create a new alert for a stage. Use this endpoint to programmatically raise alerts from telemetry or admin actions.")
        public ResponseEntity<?> create(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Alert creation payload") @RequestBody AlertRequestDto dto) {
        AlertResponseDto created = alertService.createAlert(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/resolve")
        @Operation(summary = "Resolve alert", description = "Mark an alert as resolved. Use when the issue has been addressed or the condition cleared.")
        public ResponseEntity<?> resolve(@Parameter(description = "ID of the alert to resolve") @PathVariable Long id) {
        alertService.resolveAlert(id);
        return ResponseEntity.ok().build();
    }
}
