package com.digitaltwin.central.repository;

import com.digitaltwin.central.model.SpontaneousEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface SpontaneousEventRepository extends JpaRepository<SpontaneousEvent, Long> {
    List<SpontaneousEvent> findAllByOrderByStartsAtAsc();
    List<SpontaneousEvent> findByStartsAtLessThanEqualAndEndsAtGreaterThan(OffsetDateTime starts, OffsetDateTime ends);
}