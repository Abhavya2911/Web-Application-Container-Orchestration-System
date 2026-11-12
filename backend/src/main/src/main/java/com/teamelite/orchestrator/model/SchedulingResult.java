package com.teamelite.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "scheduling_results")
@Getter @Setter @NoArgsConstructor
public class SchedulingResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(optional = false) @JoinColumn(name = "run_id")
    private SchedulingRun run;
    private Long workloadId;
    private String workloadName;
    private Long nodeId;
    private String nodeName;
    private int arrival;
    private int burst;
    @Column(name = "start_time")
    private int start;
    private int completion;
    private int waiting;
    private int turnaround;
    private int response;
}
