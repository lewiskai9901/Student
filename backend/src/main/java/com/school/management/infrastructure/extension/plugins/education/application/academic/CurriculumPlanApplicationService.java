package com.school.management.application.academic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.academic.command.CreateCurriculumPlanCommand;
import com.school.management.application.academic.command.CreatePlanCourseCommand;
import com.school.management.application.academic.command.UpdateCurriculumPlanCommand;
import com.school.management.application.academic.command.UpdatePlanCourseCommand;
import com.school.management.application.academic.query.CurriculumPlanDTO;
import com.school.management.application.academic.query.PlanCourseDTO;
import com.school.management.domain.academic.model.CurriculumPlan;
import com.school.management.domain.academic.model.PlanCourse;
import com.school.management.domain.academic.repository.CurriculumPlanRepository;
import com.school.management.domain.academic.repository.PlanCourseRepository;
import com.school.management.infrastructure.persistence.academic.CurriculumPlanPO;
import com.school.management.infrastructure.persistence.academic.CurriculumPlanPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 培养方案管理应用服务
 */
@RequiredArgsConstructor
@Service
public class CurriculumPlanApplicationService {

    private final CurriculumPlanRepository planRepository;
    private final PlanCourseRepository planCourseRepository;
    private final CurriculumPlanPersistenceMapper planMapper;

    // ======================== 方案查询 ========================

    @Transactional(readOnly = true)
    public Page<CurriculumPlanDTO> getPlanList(Integer gradeYear, Integer status,
                                                Long majorId,
                                                int pageNum, int pageSize) {
        Page<CurriculumPlanPO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CurriculumPlanPO> wrapper = new LambdaQueryWrapper<>();

        if (gradeYear != null) {
            wrapper.eq(CurriculumPlanPO::getGradeYear, gradeYear);
        }
        if (status != null) {
            wrapper.eq(CurriculumPlanPO::getStatus, status);
        }
        if (majorId != null) {
            wrapper.eq(CurriculumPlanPO::getMajorId, majorId);
        }
        wrapper.orderByDesc(CurriculumPlanPO::getCreatedAt);

        Page<CurriculumPlanPO> result = planMapper.selectPage(page, wrapper);

        Page<CurriculumPlanDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::poToDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Transactional(readOnly = true)
    public CurriculumPlanDTO getPlan(Long id) {
        CurriculumPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("培养方案不存在: " + id));
        return toDTO(plan);
    }

    // ======================== 方案命令 ========================

    @Transactional
    public CurriculumPlanDTO createPlan(CreateCurriculumPlanCommand command) {
        CurriculumPlan plan = CurriculumPlan.builder()
            .planCode(command.getPlanCode())
            .planName(command.getPlanName())
            .majorId(command.getMajorId())
            .majorDirectionId(command.getMajorDirectionId())
            .gradeYear(command.getGradeYear())
            .totalCredits(command.getTotalCredits())
            .requiredCredits(command.getRequiredCredits())
            .electiveCredits(command.getElectiveCredits())
            .practiceCredits(command.getPracticeCredits())
            .trainingObjective(command.getTrainingObjective())
            .graduationRequirement(command.getGraduationRequirement())
            .status(command.getStatus())
            .createdBy(command.getCreatedBy())
            .build();

        plan = planRepository.save(plan);
        return toDTO(plan);
    }

    @Transactional
    public CurriculumPlanDTO updatePlan(Long id, UpdateCurriculumPlanCommand command) {
        CurriculumPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("培养方案不存在: " + id));

        plan.update(
            command.getPlanName(),
            command.getMajorId(),
            command.getMajorDirectionId(),
            command.getGradeYear(),
            command.getTotalCredits(),
            command.getRequiredCredits(),
            command.getElectiveCredits(),
            command.getPracticeCredits(),
            command.getTrainingObjective(),
            command.getGraduationRequirement(),
            command.getUpdatedBy()
        );

        plan = planRepository.save(plan);
        return toDTO(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        // 先删除关联的方案课程
        planCourseRepository.deleteByPlanId(id);
        planRepository.deleteById(id);
    }

    @Transactional
    public void publishPlan(Long id, Long publishedBy) {
        CurriculumPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("培养方案不存在: " + id));
        plan.publish(publishedBy);
        planRepository.save(plan);
    }

    @Transactional
    public void deprecatePlan(Long id) {
        CurriculumPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("培养方案不存在: " + id));
        plan.deprecate();
        planRepository.save(plan);
    }

    @Transactional
    public Map<String, Object> copyPlan(Long id, Long createdBy) {
        CurriculumPlan original = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("培养方案不存在: " + id));

        // 确定新版本号
        Integer maxVersion = planRepository.findMaxVersionByPlanCode(original.getPlanCode());
        int newVersion = (maxVersion != null ? maxVersion : 1) + 1;

        // 创建新方案
        CurriculumPlan copy = original.copyWithNewVersion(newVersion, createdBy);
        copy = planRepository.save(copy);

        // 复制方案课程
        List<PlanCourse> courses = planCourseRepository.findByPlanId(id);
        for (PlanCourse pc : courses) {
            PlanCourse newPc = pc.copyForPlan(copy.getId());
            planCourseRepository.save(newPc);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", copy.getId());
        result.put("version", newVersion);
        result.put("copiedCourses", courses.size());
        return result;
    }

    // ======================== 方案课程 ========================

    @Transactional(readOnly = true)
    public List<PlanCourseDTO> getPlanCourses(Long planId) {
        return planCourseRepository.findByPlanId(planId).stream()
            .map(this::toPlanCourseDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public PlanCourseDTO addPlanCourse(Long planId, CreatePlanCourseCommand command) {
        PlanCourse pc = PlanCourse.builder()
            .planId(planId)
            .courseId(command.getCourseId())
            .semesterNumber(command.getSemesterNumber())
            .courseCategory(command.getCourseCategory())
            .courseType(command.getCourseType())
            .credits(command.getCredits())
            .totalHours(command.getTotalHours())
            .weeklyHours(command.getWeeklyHours())
            .theoryHours(command.getTheoryHours())
            .practiceHours(command.getPracticeHours())
            .assessmentMethod(command.getAssessmentMethod())
            .sortOrder(command.getSortOrder())
            .remark(command.getRemark())
            .build();

        pc = planCourseRepository.save(pc);
        return toPlanCourseDTO(pc);
    }

    @Transactional
    public void updatePlanCourse(Long planId, Long courseRecordId, UpdatePlanCourseCommand command) {
        PlanCourse pc = planCourseRepository.findById(courseRecordId)
            .orElseThrow(() -> new IllegalArgumentException("方案课程不存在: " + courseRecordId));

        pc.update(
            command.getSemesterNumber(),
            command.getCourseCategory(),
            command.getCourseType(),
            command.getCredits(),
            command.getTotalHours(),
            command.getWeeklyHours(),
            command.getTheoryHours(),
            command.getPracticeHours(),
            command.getAssessmentMethod(),
            command.getSortOrder(),
            command.getRemark()
        );

        planCourseRepository.save(pc);
    }

    @Transactional
    public void removePlanCourse(Long planId, Long courseRecordId) {
        planCourseRepository.deleteById(courseRecordId);
    }

    // ======================== Mapping ========================

    private CurriculumPlanDTO toDTO(CurriculumPlan plan) {
        CurriculumPlanDTO dto = new CurriculumPlanDTO();
        dto.setId(plan.getId());
        dto.setPlanCode(plan.getPlanCode());
        dto.setPlanName(plan.getPlanName());
        dto.setMajorId(plan.getMajorId());
        dto.setMajorDirectionId(plan.getMajorDirectionId());
        dto.setGradeYear(plan.getGradeYear());
        dto.setTotalCredits(plan.getTotalCredits());
        dto.setRequiredCredits(plan.getRequiredCredits());
        dto.setElectiveCredits(plan.getElectiveCredits());
        dto.setPracticeCredits(plan.getPracticeCredits());
        dto.setTrainingObjective(plan.getTrainingObjective());
        dto.setGraduationRequirement(plan.getGraduationRequirement());
        dto.setVersion(plan.getPlanVersion());
        dto.setStatus(plan.getStatus());
        dto.setPublishedAt(plan.getPublishedAt());
        dto.setPublishedBy(plan.getPublishedBy());
        dto.setCreatedBy(plan.getCreatedBy());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        return dto;
    }

    private CurriculumPlanDTO poToDTO(CurriculumPlanPO po) {
        CurriculumPlanDTO dto = new CurriculumPlanDTO();
        dto.setId(po.getId());
        dto.setPlanCode(po.getPlanCode());
        dto.setPlanName(po.getPlanName());
        dto.setMajorId(po.getMajorId());
        dto.setMajorDirectionId(po.getMajorDirectionId());
        dto.setGradeYear(po.getGradeYear());
        dto.setTotalCredits(po.getTotalCredits());
        dto.setRequiredCredits(po.getRequiredCredits());
        dto.setElectiveCredits(po.getElectiveCredits());
        dto.setPracticeCredits(po.getPracticeCredits());
        dto.setTrainingObjective(po.getTrainingObjective());
        dto.setGraduationRequirement(po.getGraduationRequirement());
        dto.setVersion(po.getVersion());
        dto.setStatus(po.getStatus());
        dto.setPublishedAt(po.getPublishedAt());
        dto.setPublishedBy(po.getPublishedBy());
        dto.setCreatedBy(po.getCreatedBy());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        return dto;
    }

    private PlanCourseDTO toPlanCourseDTO(PlanCourse pc) {
        PlanCourseDTO dto = new PlanCourseDTO();
        dto.setId(pc.getId());
        dto.setPlanId(pc.getPlanId());
        dto.setCourseId(pc.getCourseId());
        dto.setSemesterNumber(pc.getSemesterNumber());
        dto.setCourseCategory(pc.getCourseCategory());
        dto.setCourseType(pc.getCourseType());
        dto.setCredits(pc.getCredits());
        dto.setTotalHours(pc.getTotalHours());
        dto.setWeeklyHours(pc.getWeeklyHours());
        dto.setTheoryHours(pc.getTheoryHours());
        dto.setPracticeHours(pc.getPracticeHours());
        dto.setAssessmentMethod(pc.getAssessmentMethod());
        dto.setSortOrder(pc.getSortOrder());
        dto.setRemark(pc.getRemark());
        dto.setCourseCode(pc.getCourseCode());
        dto.setCourseName(pc.getCourseName());
        dto.setCreatedAt(pc.getCreatedAt());
        dto.setUpdatedAt(pc.getUpdatedAt());
        return dto;
    }
}
