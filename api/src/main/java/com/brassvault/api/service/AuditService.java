package com.brassvault.api.service;

import com.brassvault.api.domain.AuditAction;
import com.brassvault.api.domain.AuditLog;
import com.brassvault.api.repo.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String userEmail, AuditAction action, String itemTitle, String teamName, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setUserEmail(userEmail);
        log.setAction(action);
        log.setItemTitle(itemTitle);
        log.setTeamName(teamName);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }
}
