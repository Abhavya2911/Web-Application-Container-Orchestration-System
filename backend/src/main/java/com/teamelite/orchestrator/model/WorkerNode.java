package com.teamelite.orchestrator.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "worker_nodes")
@Getter @Setter @NoArgsConstructor
public class WorkerNode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, unique = true)
    private String name;
    @Positive @Column(nullable = false)
    private double cpuCores;
    @Positive @Column(nullable = false)
    private int memoryMb;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private NodeStatus status = NodeStatus.UP;
    private LocalDateTime lastHeartbeat;
}
