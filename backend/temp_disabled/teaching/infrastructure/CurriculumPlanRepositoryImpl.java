package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.aggregate.CurriculumPlan;
import com.school.management.domain.teaching.model.entity.PlanCourse;
import com.school.management.domain.teaching.repository.CurriculumPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 培养方案仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CurriculumPlanRepositoryImpl implements CurriculumPlanRepository {

    private final CurriculumPlanMapper planMapper;
    private final PlanCourseMapper planCourseMapper;

    @Override
    public CurriculumPlan save(CurriculumPlan plan) {
        CurriculumPlanPO po = toPO(plan);
        if (po.getId() == null) {
            planMapper.insert(po);
        } else {
            planMapper.updateById(po);
        }
        plan.setId(po.getId());
        return plan;
    }

    @Override
    public Optional<CurriculumPlan> findById(Long id) {
        CurriculumPlanPO po = planMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<CurriculumPlan> findByPlanCode(String planCode) {
        LambdaQueryWrapper<CurriculumPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CurriculumPlanPO::getPlanCode, planCode);
        CurriculumPlanPO po = planMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<CurriculumPlan> findByMajorIdAndGradeYear(Long majorId, Integer gradeYear) {
        LambdaQueryWrapper<CurriculumPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CurriculumPlanPO::getMajorId, majorId)
                .eq(CurriculumPlanPO::getEnrollYear, gradeYear)
                .ne(CurriculumPlanPO::getStatus, 2) // 排除已归档
                .orderByDesc(CurriculumPlanPO::getVersion)
                .last("LIMIT 1");
        CurriculumPlanPO po = planMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<CurriculumPlan> findAll() {
        LambdaQueryWrapper<CurriculumPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CurriculumPlanPO::getEnrollYear)
                .orderByAsc(CurriculumPlanPO::getPlanCode);
        return planMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CurriculumPlan> findByMajorId(Long majorId) {
        LambdaQueryWrapper<CurriculumPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CurriculumPlanPO::getMajorId, majorId)
                .orderByDesc(CurriculumPlanPO::getEnrollYear)
                .orderByDesc(CurriculumPlanPO::getVersion);
        return planMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        planCourseMapper.deleteByPlanId(id);
        planMapper.deleteById(id);
    }

    @Override
    public boolean existsByPlanCode(String planCode) {
        LambdaQueryWrapper<CurriculumPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CurriculumPlanPO::getPlanCode, planCode);
        return planMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Optional<CurriculumPlan> findByIdWithCourses(Long id) {
        return findById(id).map(plan -> {
            List<PlanCourse> courses = findPlanCourses(id);
            plan.setCourses(courses);
            return plan;
        });
    }

    @Override
    public List<PlanCourse> findPlanCourses(Long planId) {
        LambdaQueryWrapper<PlanCoursePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanCoursePO::getPlanId, planId)
                .orderByAsc(PlanCoursePO::getSemester);
        return planCourseMapper.selectList(wrapper).stream()
                .map(this::toPlanCourseDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void savePlanCourses(Long planId, List<PlanCourse> courses) {
        // 先删除原有课程
        planCourseMapper.deleteByPlanId(planId);

        // 保存新的课程
        for (PlanCourse course : courses) {
            PlanCoursePO po = toPlanCoursePO(course);
            po.setPlanId(planId);
            po.setCreatedAt(LocalDateTime.now());
            planCourseMapper.insert(po);
            course.setId(po.getId());
        }
    }

    @Override
    public List<CurriculumPlan> findPage(int page, int size, Long majorId, Integer status) {
        return findPage(page, size, majorId, null, status);
    }

    @Override
    public long count(Long majorId, Integer status) {
        return count(majorId, null, status);
    }

    @Override
    public void deletePlanCourse(Long planId, Long courseId) {
        LambdaQueryWrapper<PlanCoursePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanCoursePO::getPlanId, planId)
                .eq(PlanCoursePO::getCourseId, courseId);
        planCourseMapper.delete(wrapper);
    }

    @Override
    public List<CurriculumPlan> findPage(int page, int size, Long majorId, Integer enrollYear, Integer status) {
        LambdaQueryWrapper<CurriculumPlanPO> wrapper = new LambdaQueryWrapper<>();
        if (majorId != null) {
            wrapper.eq(CurriculumPlanPO::getMajorId, majorId);
        }
        if (enrollYear != null) {
            wrapper.eq(CurriculumPlanPO::getEnrollYear, enrollYear);
        }
        if (status != null) {
            wrapper.eq(CurriculumPlanPO::getStatus, status);
        }
        wrapper.orderByDesc(CurriculumPlanPO::getEnrollYear)
                .orderByDesc(CurriculumPlanPO::getVersion);

        Page<CurriculumPlanPO> pageResult = planMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(Long majorId, Integer enrollYear, Integer status) {
        LambdaQueryWrapper<CurriculumPlanPO> wrapper = new LambdaQueryWrapper<>();
        if (majorId != null) {
            wrapper.eq(CurriculumPlanPO::getMajorId, majorId);
        }
        if (enrollYear != null) {
            wrapper.eq(CurriculumPlanPO::getEnrollYear, enrollYear);
        }
        if (status != null) {
            wrapper.eq(CurriculumPlanPO::getStatus, status);
        }
        return planMapper.selectCount(wrapper);
    }

    private CurriculumPlanPO toPO(CurriculumPlan domain) {
        CurriculumPlanPO po = new CurriculumPlanPO();
        po.setId(domain.getId());
        po.setPlanCode(domain.getPlanCode());
        po.setPlanName(domain.getPlanName());
        po.setMajorId(domain.getMajorId());
        po.setEnrollYear(domain.getEnrollYear());
        po.setDuration(domain.getDuration());
        po.setVersion(domain.getVersion());
        po.setTotalCredits(domain.getTotalCredits());
        po.setRequiredCredits(domain.getRequiredCredits());
        po.setElectiveCredits(domain.getElectiveCredits());
        po.setPracticeCredits(domain.getPracticeCredits());
        po.setObjectives(domain.getObjectives());
        po.setRequirements(domain.getRequirements());
        po.setRemark(domain.getRemark());
        po.setStatus(domain.getStatus());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private CurriculumPlan toDomain(CurriculumPlanPO po) {
        return CurriculumPlan.builder()
                .id(po.getId())
                .planCode(po.getPlanCode())
                .planName(po.getPlanName())
                .majorId(po.getMajorId())
                .enrollYear(po.getEnrollYear())
                .duration(po.getDuration())
                .version(po.getVersion())
                .totalCredits(po.getTotalCredits())
                .requiredCredits(po.getRequiredCredits())
                .electiveCredits(po.getElectiveCredits())
                .practiceCredits(po.getPracticeCredits())
                .objectives(po.getObjectives())
                .requirements(po.getRequirements())
                .remark(po.getRemark())
                .status(po.getStatus())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private PlanCoursePO toPlanCoursePO(PlanCourse domain) {
        PlanCoursePO po = new PlanCoursePO();
        po.setId(domain.getId());
        po.setPlanId(domain.getPlanId());
        po.setCourseId(domain.getCourseId());
        po.setSemester(domain.getSemester());
        po.setWeeklyHours(domain.getWeeklyHours());
        po.setExamType(domain.getExamType());
        po.setIsRequired(domain.getIsRequired());
        return po;
    }

    private PlanCourse toPlanCourseDomain(PlanCoursePO po) {
        return PlanCourse.builder()
                .id(po.getId())
                .planId(po.getPlanId())
                .courseId(po.getCourseId())
                .semester(po.getSemester())
                .weeklyHours(po.getWeeklyHours())
                .examType(po.getExamType())
                .isRequired(po.getIsRequired())
                .build();
    }
}
