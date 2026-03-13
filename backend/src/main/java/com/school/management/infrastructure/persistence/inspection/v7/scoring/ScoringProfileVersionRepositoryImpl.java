package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.scoring.ScoringProfileVersion;
import com.school.management.domain.inspection.repository.v7.ScoringProfileVersionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ScoringProfileVersionRepositoryImpl implements ScoringProfileVersionRepository {

    private final ScoringProfileVersionMapper mapper;

    public ScoringProfileVersionRepositoryImpl(ScoringProfileVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ScoringProfileVersion save(ScoringProfileVersion version) {
        ScoringProfileVersionPO po = toPO(version);
        if (version.getId() == null) {
            mapper.insert(po);
            version.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return version;
    }

    @Override
    public Optional<ScoringProfileVersion> findById(Long id) {
        ScoringProfileVersionPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ScoringProfileVersion> findByProfileId(Long profileId) {
        return mapper.findByProfileId(profileId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ScoringProfileVersion> findByProfileIdAndVersion(Long profileId, Integer version) {
        ScoringProfileVersionPO po = mapper.findByProfileIdAndVersion(profileId, version);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void deleteByProfileId(Long profileId) {
        mapper.delete(new LambdaQueryWrapper<ScoringProfileVersionPO>()
                .eq(ScoringProfileVersionPO::getProfileId, profileId));
    }

    private ScoringProfileVersionPO toPO(ScoringProfileVersion domain) {
        ScoringProfileVersionPO po = new ScoringProfileVersionPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setProfileId(domain.getProfileId());
        po.setVersion(domain.getVersion());
        po.setSnapshot(domain.getSnapshot());
        po.setPublishedAt(domain.getPublishedAt());
        po.setPublishedBy(domain.getPublishedBy());
        po.setChangeSummary(domain.getChangeSummary());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private ScoringProfileVersion toDomain(ScoringProfileVersionPO po) {
        return ScoringProfileVersion.reconstruct(ScoringProfileVersion.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .profileId(po.getProfileId())
                .version(po.getVersion())
                .snapshot(po.getSnapshot())
                .publishedAt(po.getPublishedAt())
                .publishedBy(po.getPublishedBy())
                .changeSummary(po.getChangeSummary())
                .createdAt(po.getCreatedAt()));
    }
}
