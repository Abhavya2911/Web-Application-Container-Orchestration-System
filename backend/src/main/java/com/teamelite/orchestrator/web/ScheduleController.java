package com.teamelite.orchestrator.web;

import com.teamelite.orchestrator.model.*;
import com.teamelite.orchestrator.repo.*;
import com.teamelite.orchestrator.service.OrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ScheduleController {
    public record ScheduleRequest(String algorithm, Integer quantum) {}

    private final OrchestrationService svc;
    private final SchedulingRunRepository runs;
    private final SystemLogRepository logs;

    @PostMapping("/schedule")
    public SchedulingRun run(@RequestBody ScheduleRequest req) {
        return svc.schedule(req.algorithm(), req.quantum() == null ? 2 : req.quantum());
    }
    @GetMapping("/schedule/runs") public List<SchedulingRun> history() { return runs.findAllByOrderByIdDesc(); }
    @GetMapping("/schedule/runs/{id}")
    public SchedulingRun one(@PathVariable Long id) {
        return runs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
    }
    @GetMapping("/logs") public List<SystemLog> logs() { return logs.findTop30ByOrderByIdDesc(); }
}
