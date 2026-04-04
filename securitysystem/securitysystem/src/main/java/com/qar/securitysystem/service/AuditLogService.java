package com.qar.securitysystem.service;

import com.qar.securitysystem.model.AuditEventType;
import com.qar.securitysystem.model.AuditLogEntity;
import com.qar.securitysystem.repo.AuditLogRepository;
import com.qar.securitysystem.security.AppPrincipal;
import com.qar.securitysystem.util.IdUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logApi(AppPrincipal p, String method, String path, int statusCode, long durationMs, String ip, String userAgent) {
        if (p == null) {
            return;
        }
        if (p.getRole() != null && p.getRole().name().equals("ADMIN")) {
            return;
        }
        AuditLogEntity e = new AuditLogEntity();
        e.setId(IdUtil.newId());
        e.setUserId(p.getUserId());
        e.setPersonId(p.getPersonId());
        e.setPersonNo(p.getEmailOrUsername());
        e.setEventType(AuditEventType.API_CALL);
        e.setMethod(safe(method, 10));
        e.setPath(safe(path, 255));
        e.setStatusCode(statusCode);
        e.setDurationMs(durationMs);
        e.setIp(safe(ip, 64));
        e.setUserAgent(safe(userAgent, 255));
        e.setCreatedAt(Instant.now());
        auditLogRepository.save(e);
    }

    private static String safe(String v, int max) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        if (s.isBlank()) {
            return null;
        }
        if (s.length() > max) {
            s = s.substring(0, max);
        }
        return s;
    }
}

