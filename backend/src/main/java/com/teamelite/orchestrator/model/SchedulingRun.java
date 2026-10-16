package com.teamelite.orchestrator.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "scheduling_runs")
@Getter @Setter @NoArgsConstructor
public class SchedulingRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String algorithm;
    private int quantum;
    private double avgWaiting;
    private double avgTurnaround;
    private double avgResponse;
    private int rejectedCount;
    @Column(columnDefinition = "TEXT")
    private String timelineJson;
    private LocalDateTime createdAt = LocalDateTime.now();
    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchedulingResult> results = new ArrayList<>();
}
