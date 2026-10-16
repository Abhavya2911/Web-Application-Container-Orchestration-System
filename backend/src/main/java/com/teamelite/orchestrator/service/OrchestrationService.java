package com.teamelite.orchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.teamelite.orchestrator.model.*;
import com.teamelite.orchestrator.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrchestrationService {
    private static final List<WorkloadStatus> ACTIVE = List.of(WorkloadStatus.SCHEDULED, WorkloadStatus.RUNNING);

    private final WorkerNodeRepository nodeRepo;
    private final WorkloadRepository workloadRepo;
    private final SchedulingRunRepository runRepo;
    private final SystemLogRepository logRepo;
    private final SchedulerClient scheduler;
    private final DockerService docker;
    private final ObjectMapper objectMapper;
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private static ResponseStatusException notFound(String m) { return new ResponseStatusException(HttpStatus.NOT_FOUND, m); }

    private void log(String level, String msg) { logRepo.save(new SystemLog(level, msg)); }

    private static boolean onNode(Workload w, Long nodeId) {
        return w.getAssignedNode() != null && w.getAssignedNode().getId().equals(nodeId);
    }

    // ---------- nodes ----------
    @Transactional(readOnly = true)
    public List<NodeView> nodeViews() {
        List<Workload> active = workloadRepo.findByStatusIn(ACTIVE);
        return nodeRepo.findAll().stream().map(n -> {
            List<Workload> mine = active.stream().filter(w -> onNode(w, n.getId())).toList();
            return new NodeView(n.getId(), n.getName(), n.getCpuCores(), n.getMemoryMb(), n.getStatus(),
                    mine.stream().mapToDouble(Workload::getCpuRequired).sum(),
                    mine.stream().mapToInt(Workload::getMemoryRequired).sum(), mine.size());
        }).toList();
    }

    @Transactional
    public WorkerNode addNode(WorkerNode n) {
        n.setId(null); n.setStatus(NodeStatus.UP); n.setLastHeartbeat(LocalDateTime.now());
        WorkerNode saved = nodeRepo.save(n);
        log("INFO", "Node " + saved.getName() + " registered");
        return saved;
    }

    /** Failure detection + recovery: stop containers on the node, requeue them, reschedule on remaining nodes. */
    @Transactional
    public void failNode(Long id) {
        WorkerNode node = nodeRepo.findById(id).orElseThrow(() -> notFound("Node not found"));
        node.setStatus(NodeStatus.DOWN);
        nodeRepo.save(node);

        List<Workload> affected = workloadRepo.findByAssignedNode(node).stream()
                .filter(w -> ACTIVE.contains(w.getStatus())).toList();
        Set<Long> wasRunning = new HashSet<>();
        for (Workload w : affected) {
            if (w.getStatus() == WorkloadStatus.RUNNING) wasRunning.add(w.getId());
            docker.stop(w);
            w.setStatus(WorkloadStatus.PENDING);
            w.setAssignedNode(null);
        }
        workloadRepo.saveAll(affected);
        log("WARN", "Node " + node.getName() + " is DOWN; " + affected.size() + " workload(s) affected");
        if (affected.isEmpty()) return;

        Optional<SchedulingRun> last = runRepo.findFirstByOrderByIdDesc();
        String algo = last.map(SchedulingRun::getAlgorithm).orElse("FCFS");
        int quantum = last.map(SchedulingRun::getQuantum).orElse(2);
        try {
            schedule(algo, quantum);
            for (Workload w : workloadRepo.findAllById(wasRunning)) {
                if (w.getStatus() == WorkloadStatus.SCHEDULED) { docker.start(w); w.setStatus(WorkloadStatus.RUNNING); }
            }
            log("INFO", "Rescheduled workloads from " + node.getName() + " using " + algo);
        } catch (ResponseStatusException e) {
            log("WARN", "Could not reschedule after failure: " + e.getReason());
        }
    }

    @Transactional
    public void recoverNode(Long id) {
        WorkerNode node = nodeRepo.findById(id).orElseThrow(() -> notFound("Node not found"));
        node.setStatus(NodeStatus.UP);
        node.setLastHeartbeat(LocalDateTime.now());
        log("INFO", "Node " + node.getName() + " is back UP");
    }

    // ---------- scheduling ----------
    @Transactional
    public SchedulingRun schedule(String algorithm, int quantum) {
        String algo = algorithm == null ? "" : algorithm.toUpperCase();
        if (!algo.equals("FCFS") && !algo.equals("RR")) throw bad("Algorithm must be FCFS or RR");
        if (quantum < 1) throw bad("Time quantum must be at least 1");

        List<WorkerNode> up = nodeRepo.findByStatus(NodeStatus.UP);     // locks node rows
        if (up.isEmpty()) throw bad("No worker node is UP");
        List<Workload> pending = workloadRepo.findByStatusIn(List.of(WorkloadStatus.PENDING, WorkloadStatus.REJECTED));
        if (pending.isEmpty()) throw bad("There are no pending workloads to schedule");

        List<Workload> active = workloadRepo.findByStatusIn(ACTIVE);
        List<SchedulerClient.FreeNode> free = up.stream().map(n -> {
            List<Workload> mine = active.stream().filter(w -> onNode(w, n.getId())).toList();
            return new SchedulerClient.FreeNode(n.getId(),
                    n.getCpuCores() - mine.stream().mapToDouble(Workload::getCpuRequired).sum(),
                    n.getMemoryMb() - mine.stream().mapToInt(Workload::getMemoryRequired).sum());
        }).toList();

        JsonNode out = scheduler.run(algo, quantum, free, pending);

        Map<Long, Workload> byId = pending.stream().collect(Collectors.toMap(Workload::getId, Function.identity()));
        Map<Long, WorkerNode> nodeById = up.stream().collect(Collectors.toMap(WorkerNode::getId, Function.identity()));

        SchedulingRun run = new SchedulingRun();
        run.setAlgorithm(algo);
        run.setQuantum(quantum);
        run.setAvgWaiting(out.path("averages").path("waiting").asDouble());
        run.setAvgTurnaround(out.path("averages").path("turnaround").asDouble());
        run.setAvgResponse(out.path("averages").path("response").asDouble());
        run.setTimelineJson(out.path("timeline").toString());

        for (JsonNode r : out.path("results")) {
            Workload w = byId.get(r.path("id").asLong());
            WorkerNode n = nodeById.get(r.path("node").asLong());
            w.setAssignedNode(n);
            w.setStatus(WorkloadStatus.SCHEDULED);
            SchedulingResult res = new SchedulingResult();
            res.setRun(run);
            res.setWorkloadId(w.getId()); res.setWorkloadName(w.getName());
            res.setNodeId(n.getId()); res.setNodeName(n.getName());
            res.setArrival(r.path("arrival").asInt()); res.setBurst(r.path("burst").asInt());
            res.setStart(r.path("start").asInt()); res.setCompletion(r.path("completion").asInt());
            res.setWaiting(r.path("waiting").asInt()); res.setTurnaround(r.path("turnaround").asInt());
            res.setResponse(r.path("response").asInt());
            run.getResults().add(res);
        }
        int rejected = 0;
        for (JsonNode id : out.path("rejected")) {
            Workload w = byId.get(id.asLong());
            w.setStatus(WorkloadStatus.REJECTED);
            w.setAssignedNode(null);
            rejected++;
        }
        run.setRejectedCount(rejected);
        workloadRepo.saveAll(pending);
        SchedulingRun saved = runRepo.save(run);
        log("INFO", "Scheduling run #" + saved.getId() + " (" + algo + "): " + run.getResults().size()
                + " scheduled, " + rejected + " rejected");
        return saved;
    }

    // ---------- workloads / containers ----------
    @Transactional
    public Workload addWorkload(Workload w) {
        w.setId(null); w.setStatus(WorkloadStatus.PENDING); w.setAssignedNode(null);
        w.setContainerId(null); w.setHostPort(null); w.setCreatedAt(LocalDateTime.now());
        return workloadRepo.save(w);
    }

    @Transactional
    public Workload start(Long id) {
        Workload w = workloadRepo.findById(id).orElseThrow(() -> notFound("Workload not found"));
        if (w.getStatus() != WorkloadStatus.SCHEDULED) throw bad("Only SCHEDULED workloads can be started");
        docker.start(w);
        w.setStatus(WorkloadStatus.RUNNING);
        log("INFO", "Container started for " + w.getName() + " on " + w.getAssignedNode().getName());
        return w;
    }


    @Transactional
    public Workload stop(Long id) {
        Workload w = workloadRepo.findById(id)
                .orElseThrow(() -> notFound("Workload not found"));

        // Do not repeat the stop operation or log
        // if the workload is already stopped.
        if (w.getStatus() == WorkloadStatus.STOPPED) {
            return w;
        }

        docker.stop(w);

        w.setStatus(WorkloadStatus.STOPPED);
        w.setAssignedNode(null);

        log("INFO", "Workload " + w.getName() + " stopped");

        return w;
    }


    @Transactional
    public void delete(Long id) {
        Workload w = workloadRepo.findById(id)
                .orElseThrow(() -> notFound("Workload not found"));

        docker.stop(w);

        List<SchedulingRun> runs =
                runRepo.findDistinctByResults_WorkloadId(id);

        for (SchedulingRun run : runs) {
            // Remove only this workload's results.
            run.getResults().removeIf(
                    result -> Objects.equals(result.getWorkloadId(), id)
            );

            // If no results remain, delete this run entirely.
            if (run.getResults().isEmpty()) {
                runRepo.delete(run);
                continue;
            }

            // Recalculate averages from the remaining results.
            List<SchedulingResult> results = run.getResults();
            run.setAvgWaiting(
                    results.stream().mapToInt(SchedulingResult::getWaiting)
                            .average().orElse(0)
            );
            run.setAvgTurnaround(
                    results.stream().mapToInt(SchedulingResult::getTurnaround)
                            .average().orElse(0)
            );
            run.setAvgResponse(
                    results.stream().mapToInt(SchedulingResult::getResponse)
                            .average().orElse(0)
            );

            // Remove this workload from the stored timeline.
            try {
                JsonNode timeline =
                        objectMapper.readTree(run.getTimelineJson());

                if (timeline.isArray()) {
                    ArrayNode filtered = objectMapper.createArrayNode();

                    for (JsonNode segment : timeline) {
                        if (segment.path("id").asLong(-1) != id) {
                            filtered.add(segment);
                        }
                    }

                    run.setTimelineJson(filtered.toString());
                }
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Could not update scheduling timeline for run "
                                + run.getId(), e
                );
            }

            runRepo.save(run);
        }

        workloadRepo.delete(w);
    }

}
