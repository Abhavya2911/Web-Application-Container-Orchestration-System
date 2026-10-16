package com.teamelite.orchestrator.web;

import com.teamelite.orchestrator.model.*;
import com.teamelite.orchestrator.service.OrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/nodes") @RequiredArgsConstructor
public class NodeController {
    private final OrchestrationService svc;

    @GetMapping public List<NodeView> list() { return svc.nodeViews(); }
    @PostMapping public WorkerNode add(@Valid @RequestBody WorkerNode n) { return svc.addNode(n); }
    @PostMapping("/{id}/fail") public void fail(@PathVariable Long id) { svc.failNode(id); }
    @PostMapping("/{id}/recover") public void recover(@PathVariable Long id) { svc.recoverNode(id); }
}
