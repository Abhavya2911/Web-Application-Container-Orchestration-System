
package com.teamelite.orchestrator.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workload_failures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workloadId;

    @Column(nullable = false)
    private String workloadName;

    private Long workerNodeId;

    private String containerId;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private Integer exitCode;

    @Column(nullable = false)
    private LocalDateTime failedAt;

    private LocalDateTime recoveredAt;

    @Column(nullable = false)
    private String status = "FAILED";
}
