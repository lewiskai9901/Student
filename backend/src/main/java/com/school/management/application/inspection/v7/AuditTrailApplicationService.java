package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.platform.AuditTrailEntry;
import com.school.management.domain.inspection.repository.v7.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuditTrailApplicationService {

    private final AuditTrailRepository auditTrailRepository;

    // ========== Record ==========

    @Transactional
    public AuditTrailEntry record(Long tenantId, Long userId, String userName,
                                  String action, String resourceType, Long resourceId,
                                  String resourceName, String details, String ipAddress) {
        AuditTrailEntry entry = new AuditTrailEntry(
                tenantId, userId, userName, action,
                resourceType, resourceId, resourceName,
                details, ipAddress, LocalDateTime.now());
        AuditTrailEntry saved = auditTrailRepository.save(entry);
        log.info("Audit trail recorded: user={}, action={}, resource={}({})",
                userName, action, resourceType, resourceId);
        return saved;
    }

    // ========== Queries ==========

    @Transactional(readOnly = true)
    public List<AuditTrailEntry> findByResource(String resourceType, Long resourceId) {
        return auditTrailRepository.findByResourceTypeAndId(resourceType, resourceId);
    }

    @Transactional(readOnly = true)
    public List<AuditTrailEntry> findByUser(Long userId) {
        return auditTrailRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<AuditTrailEntry> findRecent(int limit) {
        return auditTrailRepository.findRecent(limit);
    }

    @Transactional(readOnly = true)
    public List<AuditTrailEntry> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return auditTrailRepository.findByDateRange(from, to);
    }
}
