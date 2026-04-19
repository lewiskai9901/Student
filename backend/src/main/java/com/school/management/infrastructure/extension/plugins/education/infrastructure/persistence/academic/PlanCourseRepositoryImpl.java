package com.school.management.infrastructure.persistence.academic;

import com.school.management.domain.academic.model.PlanCourse;
import com.school.management.domain.academic.repository.PlanCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 方案课程仓储实现
 */
@Repository
@RequiredArgsConstructor
public class PlanCourseRepositoryImpl implements PlanCourseRepository {

    private final PlanCoursePersistenceMapper planCourseMapper;

    @Override
    public Optional<PlanCourse> findById(Long id) {
        PlanCoursePO po = planCourseMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public PlanCourse save(PlanCourse planCourse) {
        PlanCoursePO po = toPO(planCourse);
        if (planCourse.getId() == null) {
            planCourseMapper.insert(po);
            planCourse.setId(po.getId());
        } else {
            planCourseMapper.updateById(po);
        }
        return planCourse;
    }

    @Override
    public void deleteById(Long id) {
        planCourseMapper.deleteById(id);
    }

    @Override
    public List<PlanCourse> findByPlanId(Long planId) {
        return planCourseMapper.findByPlanIdWithCourse(planId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteByPlanId(Long planId) {
        planCourseMapper.deleteByPlanId(planId);
    }

    // ======================== Mapping ========================

    private PlanCoursePO toPO(PlanCourse domain) {
        PlanCoursePO po = new PlanCoursePO();
        po.setId(domain.getId());
        po.setPlanId(domain.getPlanId());
        po.setCourseId(domain.getCourseId());
        po.setSemesterNumber(domain.getSemesterNumber());
        po.setCourseCategory(domain.getCourseCategory());
        po.setCourseType(domain.getCourseType());
        po.setCredits(domain.getCredits());
        po.setTotalHours(domain.getTotalHours());
        po.setWeeklyHours(domain.getWeeklyHours());
        po.setTheoryHours(domain.getTheoryHours());
        po.setPracticeHours(domain.getPracticeHours());
        po.setAssessmentMethod(domain.getAssessmentMethod());
        po.setSortOrder(domain.getSortOrder());
        po.setRemark(domain.getRemark());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private PlanCourse toDomain(PlanCoursePO po) {
        return PlanCourse.builder()
            .id(po.getId())
            .planId(po.getPlanId())
            .courseId(po.getCourseId())
            .semesterNumber(po.getSemesterNumber())
            .courseCategory(po.getCourseCategory())
            .courseType(po.getCourseType())
            .credits(po.getCredits())
            .totalHours(po.getTotalHours())
            .weeklyHours(po.getWeeklyHours())
            .theoryHours(po.getTheoryHours())
            .practiceHours(po.getPracticeHours())
            .assessmentMethod(po.getAssessmentMethod())
            .sortOrder(po.getSortOrder())
            .remark(po.getRemark())
            .courseCode(po.getCourseCode())
            .courseName(po.getCourseName())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }
}
