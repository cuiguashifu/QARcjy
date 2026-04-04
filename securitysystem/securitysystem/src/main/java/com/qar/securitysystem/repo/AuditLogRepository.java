package com.qar.securitysystem.repo;

import com.qar.securitysystem.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
    List<AuditLogEntity> findTop200ByOrderByCreatedAtDesc();
}

