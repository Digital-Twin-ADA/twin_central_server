package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.AlertRequestDto;
import com.digitaltwin.central.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class AlertWebSocketController {

    private final AlertService alertService;

    public AlertWebSocketController(AlertService alertService) {
        this.alertService = alertService;
    }

    @MessageMapping("/alerts")
    public void createAlert(@Valid @Payload AlertRequestDto dto) {
        alertService.createAlert(dto);
    }
}
