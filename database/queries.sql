CREATE DATABASE IF NOT EXISTS container_orchestrator;
USE container_orchestrator;

-- 1. WORKER NODES
CREATE TABLE IF NOT EXISTS worker_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    cpu_cores DOUBLE NOT NULL CHECK (cpu_cores > 0),
    memory_mb INT NOT NULL CHECK (memory_mb > 0),
    status ENUM('UP', 'DOWN') NOT NULL DEFAULT 'UP',
    last_heartbeat DATETIME
);

-- 2. WORKLOADS
CREATE TABLE IF NOT EXISTS workloads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    arrival_time INT NOT NULL CHECK (arrival_time >= 0),
    burst_time INT NOT NULL CHECK (burst_time >= 1),
    priority INT NOT NULL DEFAULT 1,
    cpu_required DOUBLE NOT NULL CHECK (cpu_required > 0),
    memory_required INT NOT NULL CHECK (memory_required > 0),
    status ENUM(
        'PENDING', 'SCHEDULED', 'RUNNING',
        'STOPPED', 'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',
    assigned_node_id BIGINT NULL,
    container_id VARCHAR(100),
    host_port INT,
    created_at DATETIME,
    CONSTRAINT fk_workload_node
        FOREIGN KEY (assigned_node_id)
        REFERENCES worker_nodes(id)
        ON DELETE SET NULL,
    INDEX idx_workload_status (status)
);

-- 3. SCHEDULING RUNS
CREATE TABLE IF NOT EXISTS scheduling_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    algorithm VARCHAR(10) NOT NULL,
    quantum INT NOT NULL,
    avg_waiting DOUBLE,
    avg_turnaround DOUBLE,
    avg_response DOUBLE,
    rejected_count INT DEFAULT 0,
    timeline_json TEXT,
    created_at DATETIME
);

-- 4. SCHEDULING RESULTS
CREATE TABLE IF NOT EXISTS scheduling_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    workload_id BIGINT,
    workload_name VARCHAR(100),
    node_id BIGINT,
    node_name VARCHAR(100),
    arrival INT,
    burst INT,
    start_time INT,
    completion INT,
    waiting INT,
    turnaround INT,
    response INT,

    CONSTRAINT fk_result_run
        FOREIGN KEY (run_id)
        REFERENCES scheduling_runs(id)
        ON DELETE CASCADE
);

-- 5. SYSTEM LOGS
CREATE TABLE IF NOT EXISTS system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level VARCHAR(10),
    message VARCHAR(500),
    created_at DATETIME
);

-- Verify table creation
SHOW TABLES;

-- Display all table structures
USE container_orchestrator;
DESCRIBE worker_nodes;
DESCRIBE workloads;
DESCRIBE scheduling_runs;
DESCRIBE scheduling_results;
DESCRIBE system_logs;

-- Display all stored records
SELECT * FROM worker_nodes;
SELECT * FROM workloads;
SELECT * FROM scheduling_runs;
SELECT * FROM scheduling_results;
SELECT * FROM system_logs;

-- Count records in every table
USE container_orchestrator;
SELECT 'worker_nodes' AS table_name, COUNT(*) AS total
FROM worker_nodes
UNION ALL
SELECT 'workloads', COUNT(*) FROM workloads
UNION ALL
SELECT 'scheduling_runs', COUNT(*) FROM scheduling_runs
UNION ALL
SELECT 'scheduling_results', COUNT(*) FROM scheduling_results
UNION ALL
SELECT 'system_logs', COUNT(*) FROM system_logs;

-- Show workloads with their assigned worker nodes
SELECT
    w.id,
    w.name AS workload_name,
    w.status AS workload_status,
    n.name AS worker_node,
    n.status AS node_status,
    w.cpu_required,
    w.memory_required
FROM workloads w
LEFT JOIN worker_nodes n
    ON w.assigned_node_id = n.id
ORDER BY w.id;

-- Show scheduling results with their scheduling algorithm
SELECT
    r.id AS result_id,
    sr.id AS run_id,
    sr.algorithm,
    r.workload_name,
    r.node_name,
    r.burst,
    r.waiting,
    r.turnaround,
    r.response
FROM scheduling_results r
JOIN scheduling_runs sr
    ON r.run_id = sr.id
ORDER BY sr.id, r.id;

-- Compare FCFS and Round Robin
SELECT
    algorithm,
    COUNT(*) AS total_runs,
    ROUND(AVG(avg_waiting), 2) AS average_waiting_time,
    ROUND(AVG(avg_turnaround), 2) AS average_turnaround_time,
    ROUND(AVG(avg_response), 2) AS average_response_time,
    SUM(rejected_count) AS total_rejected
FROM scheduling_runs
GROUP BY algorithm;

-- Find workloads that were rejected
SELECT id, name, cpu_required, memory_required, status
FROM workloads
WHERE status = 'REJECTED';

-- calculate remaining worker-node capacity
SELECT
    n.name AS worker_node,
    n.cpu_cores,
    COALESCE(SUM(
        CASE WHEN w.status IN ('SCHEDULED', 'RUNNING')
             THEN w.cpu_required ELSE 0 END
    ), 0) AS allocated_cpu,
    n.cpu_cores - COALESCE(SUM(
        CASE WHEN w.status IN ('SCHEDULED', 'RUNNING')
             THEN w.cpu_required ELSE 0 END
    ), 0) AS free_cpu,
    n.memory_mb - COALESCE(SUM(
        CASE WHEN w.status IN ('SCHEDULED', 'RUNNING')
             THEN w.memory_required ELSE 0 END
    ), 0) AS free_memory_mb
FROM worker_nodes n
LEFT JOIN workloads w
    ON w.assigned_node_id = n.id
GROUP BY n.id, n.name, n.cpu_cores, n.memory_mb;

-- Count workloads by status
SELECT status, COUNT(*) AS total_workloads
FROM workloads
GROUP BY status
ORDER BY total_workloads DESC;

-- Show recent system logs
SELECT id, level, message, created_at
FROM system_logs
ORDER BY id DESC
LIMIT 10;

-- Find workloads without scheduling results
SELECT w.id, w.name, w.status
FROM workloads w
LEFT JOIN scheduling_results r
    ON r.workload_id = w.id
WHERE r.id IS NULL;

-- Display the latest scheduling run
SELECT *
FROM scheduling_runs
WHERE id = (SELECT MAX(id) FROM scheduling_runs);
SELECT *
FROM scheduling_results
WHERE run_id = (SELECT MAX(id) FROM scheduling_runs);

