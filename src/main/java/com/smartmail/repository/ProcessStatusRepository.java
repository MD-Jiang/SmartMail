package com.smartmail.repository;

import com.smartmail.model.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProcessStatusRepository extends JpaRepository<ProcessStatus, Long> {
    Optional<ProcessStatus> findByUserConfigId(Long userConfigId);
}