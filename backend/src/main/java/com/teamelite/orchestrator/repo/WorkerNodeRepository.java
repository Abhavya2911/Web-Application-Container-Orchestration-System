package com.teamelite.orchestrator.repo;

import com.teamelite.orchestrator.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface WorkerNodeRepository extends JpaRepository<WorkerNode, Long> {
    /** Row-level lock (SELECT ... FOR UPDATE): two scheduling requests cannot allocate the same capacity. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<WorkerNode> findByStatus(NodeStatus status);
}
