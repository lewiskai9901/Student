package com.school.management.infrastructure.persistence.academic;

import com.school.management.domain.academic.model.CurriculumPlan;
import com.school.management.domain.academic.repository.CurriculumPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 培养方案仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CurriculumPlanRepositoryImpl implements CurriculumPlanRepository {

    private final CurriculumPlanPersistenceMapper planMapper;

    @Override
    public Optional<CurriculumPlan> findById(Long id) {
        CurriculumPlanPO po = planMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public CurriculumPlan save(CurriculumPlan plan) {
        CurriculumPlanPO po = toPO(plan);
        if (plan.getId() == null) {
            planMapper.insert(po);
            plan.setId(po.getId());
        } else {
            planMapper.updateById(po);
        }
        return plan;
    }

    @Override
    public void delete(CurriculumPlan aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            planMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        planMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return planMapper.selectById(id) != null;
    }

    @Override
    public Integer findMaxVersionByPlanCode(String planCode) {
        return planMapper.findMaxVersionByPlanCode(planCode);
    }

    // ======================== Mapping ========================

    private CurriculumPlanPO toPO(CurriculumPlan domain) {
        CurriculumPlanPO po = new CurriculumPlanPO();
        po.setId(domain.getId());
        po.setPlanCode(domain.getPlanCode());
        po.setPlanName(domain.getPlanName());
        po.setMajorId(domain.getMajorId());
        po.setMajorDirectionId(domain.getMajorDirectionId());
        po.setGradeYear(domain.getGradeYear());
        po.setTotalCredits(domain.getTotalCredits());
        po.setRequiredCredits(domain.getRequiredCredits());
        po.setElectiveCredits(domain.getElectiveCredits());
        po.setPracticeCredits(domain.getPracticeCredits());
        po.setTrainingObjective(domain.getTrainingObjective());
        po.setGraduationRequirement(domain.getGraduationRequirement());
        po.setVersion(domain.getPlanVersion());
        po.setStatus(domain.getStatus());
        po.setPublishedAt(domain.getPublishedAt());
        po.setPublishedBy(domain.getPublishedBy());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    CurriculumPlan toDomain(CurriculumPlanPO po) {
        return CurriculumPlan.builder()
            .id(po.getId())
            .planCode(po.getPlanCode())
            .planName(po.getPlanName())
            .majorId(po.getMajorId())
            .majorDirectionId(po.getMajorDirectionId())
            .gradeYear(po.getGradeYear())
            .totalCredits(po.getTotalCredits())
            .requiredCredits(po.getRequiredCredits())
            .electiveCredits(po.getElectiveCredits())
            .practiceCredits(po.getPracticeCredits())
            .trainingObjective(po.getTrainingObjective())
            .graduationRequirement(po.getGraduationRequirement())
            .planVersion(po.getVersion())
            .status(po.getStatus())
            .publishedAt(po.getPublishedAt())
            .publishedBy(po.getPublishedBy())
            .createdBy(po.getCreatedBy())
            .updatedBy(po.getUpdatedBy())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }
}
