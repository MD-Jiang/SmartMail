package com.smartmail.repository;

import com.smartmail.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailRepository extends JpaRepository<Email, Long> {
    List<Email> findByUserConfigIdAndIsProcessedFalse(Long userConfigId);
    List<Email> findByUserConfigIdOrderBySentDateDesc(Long userConfigId);
}