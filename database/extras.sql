-- Optional DBMS extras: a VIEW and a TRIGGER.
-- Run in MySQL Workbench AFTER the backend has started once (so the tables exist).
-- Nothing in the Java/React code depends on these; remove them any time with the DROP lines at the bottom.
USE container_orchestrator;

-- ============================================
-- VIEW: free capacity per worker node
-- ============================================
CREATE OR REPLACE VIEW node_free_capacity AS
SELECT n.id,
       n.name,
       n.status,
       n.cpu_cores,
       n.memory_mb,
       COALESCE(SUM(w.cpu_required), 0)                    AS used_cpu,
       COALESCE(SUM(w.memory_required), 0)                 AS used_memory_mb,
       n.cpu_cores - COALESCE(SUM(w.cpu_required), 0)      AS free_cpu,
       n.memory_mb - COALESCE(SUM(w.memory_required), 0)   AS free_memory_mb,
       COUNT(w.id)                                         AS active_workloads
FROM worker_nodes n
LEFT JOIN workloads w
       ON w.assigned_node_id = n.id
      AND w.status IN ('SCHEDULED', 'RUNNING')
GROUP BY n.id, n.name, n.status, n.cpu_cores, n.memory_mb;

-- ============================================
-- TRIGGER: audit every node status change
-- ============================================
DROP TRIGGER IF EXISTS trg_node_status_audit;

DELIMITER $$
CREATE TRIGGER trg_node_status_audit
AFTER UPDATE ON worker_nodes
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO system_logs (level, message, created_at)
        VALUES ('AUDIT',
                CONCAT('[DB trigger] Node ', NEW.name, ' changed ', OLD.status, ' -> ', NEW.status),
                NOW());
    END IF;
END$$
DELIMITER ;

-- ============================================
-- To remove both:
-- DROP TRIGGER IF EXISTS trg_node_status_audit;
-- DROP VIEW IF EXISTS node_free_capacity;
-- ============================================