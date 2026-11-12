
package com.teamelite.orchestrator.repo;

import com.teamelite.orchestrator.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;

public interface WorkloadRepository extends JpaRepository<Workload, Long> {

    List<Workload> findByStatusIn(Collection<WorkloadStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Workload w WHERE w.status IN :statuses ORDER BY w.id ASC")
    List<Workload> findByStatusInForUpdate(
            @Param("statuses") Collection<WorkloadStatus> statuses
    );

    List<Workload> findByAssignedNode(WorkerNode node);

    List<Workload> findAllByOrderByIdDesc();
}
