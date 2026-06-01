package com.digitaltwin.central.repository;

import com.digitaltwin.central.model.PointOfInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, Long> {
    List<PointOfInterest> findByTypeIgnoreCaseOrderByNameAsc(String type);
    List<PointOfInterest> findAllByOrderByNameAsc();
}
