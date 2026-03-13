package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.AuditTrailEntry;
import com.school.management.domain.inspection.repository.v7.AuditTrailRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AuditTrailRepositoryImpl implements AuditTrailRepository {

    private final AuditTrailMapper mapper;

    public AuditTrailRepositoryImpl(AuditTrailMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AuditTrailEntry save(AuditTrailEntry entry) {
        AuditTrailPO po = toPO(entry);
        mapper.insert(po);
        entry.setId(po.getId());
        return entry;
    }

    @Override
    public List<AuditTrailEntry> findByResourceTypeAndId(String resourceType, Long resourceId) {
        return mapper.findByResourceTypeAndId(resourceType, resourceId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AuditTrailEntry> findByUserId(Long userId) {
        return mapper.findByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AuditTrailEntry> findRecent(int limit) {
        return mapper.findRecent(limit).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AuditTrailEntry> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return mapper.findByDateRange(from, to).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private AuditTrailPO toPO(AuditTrailEntry d) {
        AuditTrailPO po = new AuditTrailPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setUserId(d.getUserId());
        po.setUserName(d.getUserName());
        po.setAction(d.getAction());
        po.setResourceType(d.getResourceType());
        po.setResourceId(d.getResourceId());
        po.setResourceName(d.getResourceName());
        po.setDetails(d.getDetails());
        po.setIpAddress(d.getIpAddress());
        po.setOccurredAt(d.getOccurredAt());
        return po;
    }

    private AuditTrailEntry toDomain(AuditTrailPO po) {
        AuditTrailEntry entry = new AuditTrailEntry(
                po.getTenantId(),
                po.getUserId(),
                po.getUserName(),
                po.getAction(),
                po.getResourceType(),
                po.getResourceId(),
                po.getResourceName(),
                po.getDetails(),
                po.getIpAddress(),
                po.getOccurredAt());
        entry.setId(po.getId());
        return entry;
    }
}
