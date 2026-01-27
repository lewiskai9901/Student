package com.school.management.infrastructure.persistence.analytics;

import com.school.management.domain.analytics.AnalyticsSnapshotRepository;
import com.school.management.domain.analytics.model.AnalyticsSnapshot;
import com.school.management.domain.analytics.model.SnapshotType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of AnalyticsSnapshotRepository
 */
@Repository
public class AnalyticsSnapshotRepositoryImpl implements AnalyticsSnapshotRepository {

    private final AnalyticsSnapshotMapper snapshotMapper;

    public AnalyticsSnapshotRepositoryImpl(AnalyticsSnapshotMapper snapshotMapper) {
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    public AnalyticsSnapshot save(AnalyticsSnapshot snapshot) {
        AnalyticsSnapshotPO po = toPO(snapshot);
        snapshotMapper.insert(po);
        snapshot.setId(po.getId());
        return snapshot;
    }

    @Override
    public Optional<AnalyticsSnapshot> findById(Long id) {
        AnalyticsSnapshotPO po = snapshotMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<AnalyticsSnapshot> findByTypeAndDate(SnapshotType type, LocalDate date) {
        return snapshotMapper.findByTypeAndDate(type.name(), date).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AnalyticsSnapshot> findLatestByTypeAndScope(SnapshotType type, String scope, Long scopeId) {
        AnalyticsSnapshotPO po = snapshotMapper.findLatestByTypeAndScope(type.name(), scope, scopeId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AnalyticsSnapshot> findByTypeAndDateRange(SnapshotType type, LocalDate startDate, LocalDate endDate) {
        return snapshotMapper.findByTypeAndDateRange(type.name(), startDate, endDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private AnalyticsSnapshotPO toPO(AnalyticsSnapshot domain) {
        AnalyticsSnapshotPO po = new AnalyticsSnapshotPO();
        po.setId(domain.getId());
        po.setSnapshotType(domain.getSnapshotType() != null ? domain.getSnapshotType().name() : null);
        po.setSnapshotScope(domain.getSnapshotScope());
        po.setScopeId(domain.getScopeId());
        po.setSnapshotDate(domain.getSnapshotDate());
        po.setDataJson(domain.getDataJson());
        po.setGeneratedAt(domain.getGeneratedAt());
        return po;
    }

    private AnalyticsSnapshot toDomain(AnalyticsSnapshotPO po) {
        return AnalyticsSnapshot.reconstruct(
                po.getId(),
                po.getSnapshotType() != null ? SnapshotType.valueOf(po.getSnapshotType()) : null,
                po.getSnapshotScope(),
                po.getScopeId(),
                po.getSnapshotDate(),
                po.getDataJson(),
                po.getGeneratedAt()
        );
    }
}
