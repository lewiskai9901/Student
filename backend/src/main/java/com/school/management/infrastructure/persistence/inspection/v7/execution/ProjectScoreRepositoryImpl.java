package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.execution.ProjectScore;
import com.school.management.domain.inspection.repository.v7.ProjectScoreRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProjectScoreRepositoryImpl implements ProjectScoreRepository {

    private final ProjectScoreMapper mapper;

    public ProjectScoreRepositoryImpl(ProjectScoreMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ProjectScore save(ProjectScore score) {
        ProjectScorePO po = toPO(score);
        if (score.getId() == null) {
            mapper.insert(po);
            score.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return score;
    }

    @Override
    public Optional<ProjectScore> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<ProjectScore> findByProjectIdAndCycleDate(Long projectId, LocalDate cycleDate) {
        return Optional.ofNullable(mapper.findByProjectIdAndCycleDate(projectId, cycleDate)).map(this::toDomain);
    }

    @Override
    public List<ProjectScore> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        mapper.delete(new LambdaQueryWrapper<ProjectScorePO>()
                .eq(ProjectScorePO::getProjectId, projectId));
    }

    private ProjectScorePO toPO(ProjectScore d) {
        ProjectScorePO po = new ProjectScorePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setCycleDate(d.getCycleDate());
        po.setScore(d.getScore());
        po.setGrade(d.getGrade());
        po.setTargetCount(d.getTargetCount());
        po.setDetail(d.getDetail());
        po.setGradeSchemeDisplayName(d.getGradeSchemeDisplayName());
        po.setGradeName(d.getGradeName());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ProjectScore toDomain(ProjectScorePO po) {
        return ProjectScore.reconstruct(ProjectScore.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .cycleDate(po.getCycleDate())
                .score(po.getScore())
                .grade(po.getGrade())
                .targetCount(po.getTargetCount())
                .detail(po.getDetail())
                .gradeSchemeDisplayName(po.getGradeSchemeDisplayName())
                .gradeName(po.getGradeName())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
