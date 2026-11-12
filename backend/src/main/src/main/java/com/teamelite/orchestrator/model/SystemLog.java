package com.teamelite.orchestrator.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "system_logs")
@Getter @Setter @NoArgsConstructor
public class SystemLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String level;
    @Column(length = 500)
    private String message;
    private LocalDateTime createdAt = LocalDateTime.now();
    public SystemLog(String level, String message) { this.level = level; this.message = message; }
}
