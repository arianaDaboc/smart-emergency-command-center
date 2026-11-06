package com.smartemergency.service;

import org.springframework.stereotype.Service;
import com.smartemergency.domain.AuditLog;
import com.smartemergency.repo.AuditLogRepository;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    public void log(String username, String ip, String action, String method, String path, Integer status, Long durationMs) {
        AuditLog a = new AuditLog();
        a.setUsername(username);
        a.setIp(ip);
        a.setAction(action);
        a.setMethod(method);
        a.setPath(path != null && path.length() > 255 ? path.substring(0, 255) : path);
        a.setStatus(status);
        a.setDurationMs(durationMs);
        repo.save(a);
    }
}
