
package com.teamelite.orchestrator.repo;

import com.teamelite.orchestrator.model.WorkloadFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkloadFailureRepository
        extends JpaRepository<WorkloadFailure, Long> {

    List<WorkloadFailure> findAllByOrderByFailedAtDesc();

    List<WorkloadFailure> findByWorkloadIdOrderByFailedAtDesc(Long workloadId);

    List<WorkloadFailure> findByStatusOrderByFailedAtDesc(String status);
}
