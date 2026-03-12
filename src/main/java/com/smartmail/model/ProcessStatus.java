package com.smartmail.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "process_status")
public class ProcessStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_config_id", nullable = false)
    private UserConfig userConfig;
    
    private String currentState;
    private LocalDateTime lastUpdated;
    private String lockOwner;
    private LocalDateTime lockTime;
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}