package com.digitaltwin.central.service;

import com.digitaltwin.central.dto.PointOfInterestResponseDto;
import com.digitaltwin.central.model.PointOfInterest;
import com.digitaltwin.central.repository.PointOfInterestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointOfInterestService {

    private final PointOfInterestRepository pointOfInterestRepository;

    public PointOfInterestService(PointOfInterestRepository pointOfInterestRepository) {
        this.pointOfInterestRepository = pointOfInterestRepository;
    }

    public List<PointOfInterestResponseDto> getAll(String type) {
        List<PointOfInterest> points = type == null || type.isBlank()
                ? pointOfInterestRepository.findAllByOrderByNameAsc()
                : pointOfInterestRepository.findByTypeIgnoreCaseOrderByNameAsc(type);

        return points.stream().map(this::toResponse).toList();
    }

    public PointOfInterestResponseDto getById(Long id) {
        PointOfInterest point = pointOfInterestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Point of interest not found with id: " + id));

        return toResponse(point);
    }

    private PointOfInterestResponseDto toResponse(PointOfInterest point) {
        return new PointOfInterestResponseDto(
                point.getId(),
                point.getName(),
                point.getType(),
                point.getDescription(),
                point.getLatitude(),
                point.getLongitude(),
                point.getZoneCode(),
                point.getOpeningHours()
        );
    }
}
