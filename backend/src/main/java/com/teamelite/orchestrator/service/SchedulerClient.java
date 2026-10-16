package com.teamelite.orchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamelite.orchestrator.model.Workload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Bridge to the C scheduling engine: sends text on stdin, reads JSON from stdout. */
@Service
public class SchedulerClient {
    public record FreeNode(long id, double cpu, int memMb) {}

    @Value("${scheduler.path}") private String schedulerPath;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode run(String algorithm, int quantum, List<FreeNode> nodes, List<Workload> workloads) {
        StringBuilder in = new StringBuilder();
        in.append(algorithm).append(' ').append(quantum).append('\n');
        in.append(nodes.size()).append('\n');
        for (FreeNode n : nodes)
            in.append(String.format(Locale.ROOT, "%d %.3f %d%n", n.id(), n.cpu(), n.memMb()));
        in.append(workloads.size()).append('\n');
        for (Workload w : workloads)
            in.append(String.format(Locale.ROOT, "%d %d %d %d %.3f %d%n", w.getId(), w.getArrivalTime(),
                    w.getBurstTime(), w.getPriority(), w.getCpuRequired(), w.getMemoryRequired()));

        File exe = new File(schedulerPath).getAbsoluteFile();
        if (!exe.exists())
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "C scheduler not found at " + exe + " - compile it first (cd scheduler && make)");
        try {
            Process p = new ProcessBuilder(exe.getPath()).start();
            p.getOutputStream().write(in.toString().getBytes(StandardCharsets.UTF_8));
            p.getOutputStream().close();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(10, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new IllegalStateException("scheduler timed out"); }
            if (p.exitValue() != 0) throw new IllegalStateException("scheduler failed: " + err.trim());
            return mapper.readTree(out);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Scheduler error: " + e.getMessage());
        }
    }
}
