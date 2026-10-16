package com.teamelite.orchestrator.web;

import com.teamelite.orchestrator.model.Workload;
import com.teamelite.orchestrator.repo.WorkloadRepository;
import com.teamelite.orchestrator.service.OrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/workloads") @RequiredArgsConstructor
public class WorkloadController {
    private final OrchestrationService svc;
    private final WorkloadRepository repo;

    @GetMapping public List<Workload> list() { return repo.findAllByOrderByIdDesc(); }
    @PostMapping public Workload add(@Valid @RequestBody Workload w) { return svc.addWorkload(w); }
    @PostMapping("/{id}/start") public Workload start(@PathVariable Long id) { return svc.start(id); }
    @PostMapping("/{id}/stop") public Workload stop(@PathVariable Long id) { return svc.stop(id); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { svc.delete(id); }
}
