package com.teamelite.orchestrator.repo;

import com.teamelite.orchestrator.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface WorkloadRepository extends JpaRepository<Workload, Long> {
    List<Workload> findByStatusIn(Collection<WorkloadStatus> statuses);
    List<Workload> findByAssignedNode(WorkerNode node);
    List<Workload> findAllByOrderByIdDesc();
}
