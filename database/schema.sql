-- Run in MySQL Workbench (File > Open SQL Script > Execute).
-- Spring Boot (ddl-auto=update) will also create/adjust these tables on startup.
CREATE DATABASE IF NOT EXISTS container_orchestrator;
USE container_orchestrator;

CREATE TABLE IF NOT EXISTS worker_nodes (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100) NOT NULL UNIQUE,
  cpu_cores      DOUBLE       NOT NULL CHECK (cpu_cores > 0),
  memory_mb      INT          NOT NULL CHECK (memory_mb > 0),
  status         ENUM('UP','DOWN') NOT NULL DEFAULT 'UP',
  last_heartbeat DATETIME
);

CREATE TABLE IF NOT EXISTS workloads (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  arrival_time    INT    NOT NULL CHECK (arrival_time >= 0),
  burst_time      INT    NOT NULL CHECK (burst_time >= 1),
  priority        INT    NOT NULL DEFAULT 1,
  cpu_required    DOUBLE NOT NULL CHECK (cpu_required > 0),
  memory_required INT    NOT NULL CHECK (memory_required > 0),
  status          ENUM('PENDING','SCHEDULED','RUNNING','STOPPED','REJECTED') NOT NULL DEFAULT 'PENDING',
  assigned_node_id BIGINT NULL,
  container_id    VARCHAR(100),
  host_port       INT,
  created_at      DATETIME,
  CONSTRAINT fk_workload_node FOREIGN KEY (assigned_node_id)
      REFERENCES worker_nodes(id) ON DELETE SET NULL,
  INDEX idx_workload_status (status)
);

CREATE TABLE IF NOT EXISTS scheduling_runs (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  algorithm      VARCHAR(10) NOT NULL,
  quantum        INT         NOT NULL,
  avg_waiting    DOUBLE,
  avg_turnaround DOUBLE,
  avg_response   DOUBLE,
  rejected_count INT DEFAULT 0,
  timeline_json  TEXT,
  created_at     DATETIME
);

CREATE TABLE IF NOT EXISTS scheduling_results (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  run_id        BIGINT NOT NULL,
  workload_id   BIGINT,
  workload_name VARCHAR(100),
  node_id       BIGINT,
  node_name     VARCHAR(100),
  arrival       INT, burst INT, start_time INT, completion INT,
  waiting       INT, turnaround INT, response INT,
  CONSTRAINT fk_result_run FOREIGN KEY (run_id) REFERENCES scheduling_runs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system_logs (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  level      VARCHAR(10),
  message    VARCHAR(500),
  created_at DATETIME
);
