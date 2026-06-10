package com.digitaltwin.central.repository;

import com.digitaltwin.central.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {
    boolean existsByStageIdAndTypeAndResolvedFalse(Long stageId, String type);
}
