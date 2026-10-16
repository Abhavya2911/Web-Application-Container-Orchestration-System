USE container_orchestrator;

-- 1. Free capacity per node (resources still available)
SELECT n.name, n.cpu_cores - COALESCE(SUM(w.cpu_required),0)       AS free_cpu,
               n.memory_mb - COALESCE(SUM(w.memory_required),0)    AS free_mem_mb
FROM worker_nodes n
LEFT JOIN workloads w ON w.assigned_node_id = n.id AND w.status IN ('SCHEDULED','RUNNING')
GROUP BY n.id;

-- 2. Compare algorithms by average metrics over all runs
SELECT algorithm, COUNT(*) AS runs, ROUND(AVG(avg_waiting),2) AS avg_wait,
       ROUND(AVG(avg_turnaround),2) AS avg_tat, ROUND(AVG(avg_response),2) AS avg_resp
FROM scheduling_runs GROUP BY algorithm;

-- 3. Per-workload results of the latest run
SELECT r.* FROM scheduling_results r
WHERE r.run_id = (SELECT MAX(id) FROM scheduling_runs);

-- 4. Workloads per node and status
SELECT n.name, w.status, COUNT(*) AS total
FROM workloads w JOIN worker_nodes n ON n.id = w.assigned_node_id
GROUP BY n.name, w.status;

-- 5. Recent system log
SELECT * FROM system_logs ORDER BY id DESC LIMIT 20;
