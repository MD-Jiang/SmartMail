package com.smartmail.repository;

import com.smartmail.model.ProcessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessLogRepository extends JpaRepository<ProcessLog, Long> {
}