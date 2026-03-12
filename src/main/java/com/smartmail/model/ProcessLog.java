package com.smartmail.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "process_log")
public class ProcessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_config_id", nullable = false)
    private UserConfig userConfig;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String errorMessage;
    private int processedCount;
}