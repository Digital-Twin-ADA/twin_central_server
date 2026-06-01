package com.digitaltwin.central.repository;

import com.digitaltwin.central.model.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findAllByOrderByIdAsc();
}
