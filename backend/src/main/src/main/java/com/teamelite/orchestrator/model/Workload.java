package com.teamelite.orchestrator.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "workloads")
@Getter @Setter @NoArgsConstructor
public class Workload {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false)
    private String name;
    @Min(0) @Column(nullable = false)
    private int arrivalTime;
    @Min(1) @Column(nullable = false)
    private int burstTime;
    private int priority = 1;
    @Positive @Column(nullable = false)
    private double cpuRequired;
    @Positive @Column(nullable = false)
    private int memoryRequired;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private WorkloadStatus status = WorkloadStatus.PENDING;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "assigned_node_id")
    private WorkerNode assignedNode;
    private String containerId;
    private Integer hostPort;
    private LocalDateTime createdAt = LocalDateTime.now();
}
