package com.digitaltwin.central.service;

import com.digitaltwin.central.dto.ParticipantHeatmapPointDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HeatmapPublisher {

    private final SimpMessagingTemplate template;

    public HeatmapPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void publish(List<ParticipantHeatmapPointDto> points) {
        template.convertAndSend("/topic/heatmap", points);
    }
}
