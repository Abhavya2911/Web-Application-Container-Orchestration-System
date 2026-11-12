package com.teamelite.orchestrator.repo;

import com.teamelite.orchestrator.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    List<SystemLog> findTop30ByOrderByIdDesc();
}
