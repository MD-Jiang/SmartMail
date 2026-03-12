package com.smartmail.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "email")
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_config_id", nullable = false)
    private UserConfig userConfig;
    
    private String messageId;
    private String fromAddress;
    private String subject;
    private LocalDateTime sentDate;
    private String content;
    private String cleanedContent;
    private String category;
    private String summary;
    private boolean isProcessed;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}