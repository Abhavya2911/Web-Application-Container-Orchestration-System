package com.teamelite.orchestrator;

import com.teamelite.orchestrator.model.*;
import com.teamelite.orchestrator.repo.WorkerNodeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

@SpringBootApplication
public class OrchestratorApplication {
    public static void main(String[] args) { SpringApplication.run(OrchestratorApplication.class, args); }

    /** Seed three simulated worker nodes on first start. */

    @Bean
    CommandLineRunner seed(WorkerNodeRepository nodes) {
        return args -> {
            // Detect resources available to this Java process.
            int availableCpu = Runtime.getRuntime().availableProcessors();

            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean)
                            ManagementFactory.getOperatingSystemMXBean();

            long totalMemoryBytes = os.getTotalMemorySize();
            int memoryMb = (int) Math.max(1, totalMemoryBytes / (1024 * 1024));

            // Register or update the local PC as worker-1.
            WorkerNode node = nodes.findAll().stream()
                    .filter(n -> "worker-1".equalsIgnoreCase(n.getName()))
                    .findFirst()
                    .orElseGet(WorkerNode::new);

            node.setName("worker-1");
            node.setCpuCores(availableCpu);
            node.setMemoryMb(memoryMb);
            node.setStatus(NodeStatus.UP);
            node.setLastHeartbeat(LocalDateTime.now());

            nodes.save(node);
        };
    }


    @Bean
    WebMvcConfigurer cors() {
        return new WebMvcConfigurer() {
            @Override public void addCorsMappings(CorsRegistry r) {
                r.addMapping("/api/**").allowedOrigins("http://localhost:5173").allowedMethods("*");
            }
        };
    }
}
