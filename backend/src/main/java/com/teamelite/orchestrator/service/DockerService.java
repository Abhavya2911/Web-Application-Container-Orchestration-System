package com.teamelite.orchestrator.service;

import com.teamelite.orchestrator.model.Workload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Runs sample-app containers through the Docker CLI. docker.enabled=false simulates them. */
@Service
public class DockerService {
    @Value("${docker.enabled:true}") private boolean enabled;
    @Value("${docker.image}") private String image;

    public void start(Workload w) {
        String name = "workload-" + w.getId();
        int port = 20000 + (int) (w.getId() % 10000);
        if (!enabled) { w.setContainerId("sim-" + name); w.setHostPort(port); return; }
        try { exec("docker", "rm", "-f", name); } catch (ResponseStatusException ignored) { }
        String id = exec("docker", "run", "-d", "--name", name,
                "--cpus", String.format(Locale.ROOT, "%.2f", w.getCpuRequired()),
                "--memory", w.getMemoryRequired() + "m",
                "-p", port + ":80", image).trim();
        w.setContainerId(id.substring(0, Math.min(12, id.length())));
        w.setHostPort(port);
    }

    public void stop(Workload w) {
        if (w.getContainerId() == null) return;
        if (enabled) {
            try { exec("docker", "rm", "-f", "workload-" + w.getId()); } catch (ResponseStatusException ignored) { }
        }
        w.setContainerId(null);
        w.setHostPort(null);
    }

    private String exec(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (p.waitFor() != 0)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker: " + out.trim());
            return out;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Docker is not available: " + e.getMessage() + " (start Docker Desktop or set docker.enabled=false)");
        }
    }
}
