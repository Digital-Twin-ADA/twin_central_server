package com.digitaltwin.central.repository;

import com.digitaltwin.central.model.ParticipantLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantLocationRepository extends JpaRepository<ParticipantLocation, Long> {
    Optional<ParticipantLocation> findByParticipantId(String participantId);
    List<ParticipantLocation> findByRecordedAtGreaterThanEqualOrderByRecordedAtDesc(OffsetDateTime recordedAt);
    List<ParticipantLocation> findAllByOrderByRecordedAtDesc();
}
