
package com.teamelite.orchestrator.repo;

import com.teamelite.orchestrator.model.SchedulingRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchedulingRunRepository
        extends JpaRepository<SchedulingRun, Long> {

    List<SchedulingRun> findAllByOrderByIdDesc();

    Optional<SchedulingRun> findFirstByOrderByIdDesc();

    List<SchedulingRun> findDistinctByResults_WorkloadId(Long workloadId);
}
