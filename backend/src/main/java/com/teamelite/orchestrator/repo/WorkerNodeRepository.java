
package com.teamelite.orchestrator.repo;

import com.teamelite.orchestrator.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkerNodeRepository extends JpaRepository<WorkerNode, Long> {

    /**
     * Row-level lock (SELECT ... FOR UPDATE).
     * Prevents concurrent scheduling transactions from
     * simultaneously allocating the same worker capacity.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM WorkerNode n WHERE n.status = :status")
    List<WorkerNode> findByStatus(@Param("status") NodeStatus status);
}
