package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.execution.OrgUnitScore;
import com.school.management.domain.inspection.repository.OrgUnitScoreRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class OrgUnitScoreRepositoryImpl implements OrgUnitScoreRepository {

    private final OrgUnitScoreMapper mapper;

    public OrgUnitScoreRepositoryImpl(OrgUnitScoreMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OrgUnitScore save(OrgUnitScore score) {
        OrgUnitScorePO po = toPO(score);
        if (score.getId() == null) {
            mapper.insert(po);
            score.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return score;
    }

    @Override
    public Optional<OrgUnitScore> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<OrgUnitScore> findByProjectIdAndOrgUnitIdAndCycleDate(Long projectId, Long orgUnitId, LocalDate cycleDate) {
        return Optional.ofNullable(
                mapper.findByProjectIdAndOrgUnitIdAndCycleDate(projectId, orgUnitId, cycleDate))
                .map(this::toDomain);
    }

    @Override
    public List<OrgUnitScore> findByProjectIdAndCycleDate(Long projectId, LocalDate cycleDate) {
        return mapper.findByProjectIdAndCycleDate(projectId, cycleDate).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<OrgUnitScore> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        mapper.delete(new LambdaQueryWrapper<OrgUnitScorePO>()
                .eq(OrgUnitScorePO::getProjectId, projectId));
    }

    private OrgUnitScorePO toPO(OrgUnitScore d) {
        OrgUnitScorePO po = new OrgUnitScorePO();
        po.setId(d.getId());
        // org_unit_scores.tenant_id 默认 1 (单租户); org_unit_id 是业务主键, 显式 set.
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 1L);
        po.setProjectId(d.getProjectId());
        po.setOrgUnitId(d.getOrgUnitId());
        po.setCycleDate(d.getCycleDate());
        po.setScore(d.getScore());
        po.setGrade(d.getGrade());
        po.setChildCount(d.getChildCount());
        po.setSourceCount(d.getSourceCount());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private OrgUnitScore toDomain(OrgUnitScorePO po) {
        return OrgUnitScore.reconstruct(OrgUnitScore.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .orgUnitId(po.getOrgUnitId())
                .cycleDate(po.getCycleDate())
                .score(po.getScore())
                .grade(po.getGrade())
                .childCount(po.getChildCount())
                .sourceCount(po.getSourceCount())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
