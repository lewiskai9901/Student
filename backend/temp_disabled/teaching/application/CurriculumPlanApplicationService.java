package com.school.management.application.teaching;

import com.school.management.application.teaching.command.CreateCurriculumPlanCommand;
import com.school.management.application.teaching.command.UpdateCurriculumPlanCommand;
import com.school.management.application.teaching.query.CourseDTO;
import com.school.management.application.teaching.query.CurriculumPlanDTO;
import com.school.management.application.teaching.query.PlanCourseDTO;
import com.school.management.domain.teaching.model.aggregate.CurriculumPlan;
import com.school.management.domain.teaching.model.entity.PlanCourse;
import com.school.management.domain.teaching.repository.CourseRepository;
import com.school.management.domain.teaching.repository.CurriculumPlanRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 培养方案应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculumPlanApplicationService {

    private final CurriculumPlanRepository curriculumPlanRepository;
    private final CourseRepository courseRepository;

    /**
     * 创建培养方案
     */
    @Transactional
    public Long createPlan(CreateCurriculumPlanCommand command) {
        // 检查方案代码是否已存在
        if (curriculumPlanRepository.existsByPlanCode(command.getPlanCode())) {
            throw new BusinessException("方案代码已存在: " + command.getPlanCode());
        }

        // 检查同一专业同一年份是否已有方案
        if (curriculumPlanRepository.findByMajorIdAndEnrollYear(command.getMajorId(), command.getEnrollYear()).isPresent()) {
            throw new BusinessException("该专业该年份已存在培养方案");
        }

        CurriculumPlan plan = CurriculumPlan.builder()
                .planCode(command.getPlanCode())
                .planName(command.getPlanName())
                .majorId(command.getMajorId())
                .enrollYear(command.getEnrollYear())
                .duration(command.getDuration())
                .version(1) // 初始版本
                .totalCredits(command.getTotalCredits())
                .requiredCredits(command.getRequiredCredits())
                .electiveCredits(command.getElectiveCredits())
                .practiceCredits(command.getPracticeCredits())
                .objectives(command.getObjectives())
                .requirements(command.getRequirements())
                .remark(command.getRemark())
                .status(0) // 草稿状态
                .createdBy(command.getOperatorId())
                .createdAt(LocalDateTime.now())
                .build();

        plan = curriculumPlanRepository.save(plan);

        // 保存课程列表
        if (command.getCourses() != null && !command.getCourses().isEmpty()) {
            List<PlanCourse> planCourses = new ArrayList<>();
            for (CreateCurriculumPlanCommand.PlanCourseItem item : command.getCourses()) {
                PlanCourse pc = PlanCourse.builder()
                        .planId(plan.getId())
                        .courseId(item.getCourseId())
                        .semester(item.getSemester())
                        .weeklyHours(item.getWeeklyHours())
                        .examType(item.getExamType())
                        .isRequired(item.getIsRequired())
                        .build();
                planCourses.add(pc);
            }
            curriculumPlanRepository.savePlanCourses(plan.getId(), planCourses);
        }

        log.info("创建培养方案成功: id={}, code={}", plan.getId(), plan.getPlanCode());
        return plan.getId();
    }

    /**
     * 更新培养方案
     */
    @Transactional
    public void updatePlan(UpdateCurriculumPlanCommand command) {
        CurriculumPlan plan = curriculumPlanRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException("培养方案不存在: " + command.getId()));

        // 已发布的方案需要创建新版本
        if (plan.getStatus() == 1 && command.getCourses() != null) {
            throw new BusinessException("已发布的方案不能直接修改课程,请创建新版本");
        }

        if (command.getPlanName() != null) {
            plan.setPlanName(command.getPlanName());
        }
        if (command.getDuration() != null) {
            plan.setDuration(command.getDuration());
        }
        if (command.getTotalCredits() != null) {
            plan.setTotalCredits(command.getTotalCredits());
        }
        if (command.getRequiredCredits() != null) {
            plan.setRequiredCredits(command.getRequiredCredits());
        }
        if (command.getElectiveCredits() != null) {
            plan.setElectiveCredits(command.getElectiveCredits());
        }
        if (command.getPracticeCredits() != null) {
            plan.setPracticeCredits(command.getPracticeCredits());
        }
        if (command.getObjectives() != null) {
            plan.setObjectives(command.getObjectives());
        }
        if (command.getRequirements() != null) {
            plan.setRequirements(command.getRequirements());
        }
        if (command.getRemark() != null) {
            plan.setRemark(command.getRemark());
        }
        if (command.getStatus() != null) {
            plan.setStatus(command.getStatus());
        }

        plan.setUpdatedBy(command.getOperatorId());
        plan.setUpdatedAt(LocalDateTime.now());

        curriculumPlanRepository.save(plan);

        // 更新课程列表
        if (command.getCourses() != null) {
            List<PlanCourse> planCourses = new ArrayList<>();
            for (UpdateCurriculumPlanCommand.PlanCourseItem item : command.getCourses()) {
                PlanCourse pc = PlanCourse.builder()
                        .planId(plan.getId())
                        .courseId(item.getCourseId())
                        .semester(item.getSemester())
                        .weeklyHours(item.getWeeklyHours())
                        .examType(item.getExamType())
                        .isRequired(item.getIsRequired())
                        .build();
                planCourses.add(pc);
            }
            curriculumPlanRepository.savePlanCourses(plan.getId(), planCourses);
        }

        log.info("更新培养方案成功: id={}", plan.getId());
    }

    /**
     * 发布培养方案
     */
    @Transactional
    public void publishPlan(Long id, Long operatorId) {
        CurriculumPlan plan = curriculumPlanRepository.findById(id)
                .orElseThrow(() -> new BusinessException("培养方案不存在: " + id));

        if (plan.getStatus() == 1) {
            throw new BusinessException("方案已发布");
        }

        // 检查课程配置是否完整
        List<PlanCourse> courses = curriculumPlanRepository.findPlanCourses(id);
        if (courses.isEmpty()) {
            throw new BusinessException("请先配置课程后再发布");
        }

        plan.setStatus(1);
        plan.setUpdatedBy(operatorId);
        plan.setUpdatedAt(LocalDateTime.now());
        curriculumPlanRepository.save(plan);

        log.info("发布培养方案成功: id={}", id);
    }

    /**
     * 归档培养方案
     */
    @Transactional
    public void archivePlan(Long id, Long operatorId) {
        CurriculumPlan plan = curriculumPlanRepository.findById(id)
                .orElseThrow(() -> new BusinessException("培养方案不存在: " + id));

        plan.setStatus(2);
        plan.setUpdatedBy(operatorId);
        plan.setUpdatedAt(LocalDateTime.now());
        curriculumPlanRepository.save(plan);

        log.info("归档培养方案成功: id={}", id);
    }

    /**
     * 创建新版本
     */
    @Transactional
    public Long createNewVersion(Long id, Long operatorId) {
        CurriculumPlan oldPlan = curriculumPlanRepository.findByIdWithCourses(id)
                .orElseThrow(() -> new BusinessException("培养方案不存在: " + id));

        int newVersion = oldPlan.getVersion() + 1;
        String newCode = oldPlan.getPlanCode() + "-v" + newVersion;

        CurriculumPlan newPlan = CurriculumPlan.builder()
                .planCode(newCode)
                .planName(oldPlan.getPlanName() + " v" + newVersion)
                .majorId(oldPlan.getMajorId())
                .enrollYear(oldPlan.getEnrollYear())
                .duration(oldPlan.getDuration())
                .version(newVersion)
                .totalCredits(oldPlan.getTotalCredits())
                .requiredCredits(oldPlan.getRequiredCredits())
                .electiveCredits(oldPlan.getElectiveCredits())
                .practiceCredits(oldPlan.getPracticeCredits())
                .objectives(oldPlan.getObjectives())
                .requirements(oldPlan.getRequirements())
                .remark("基于版本" + oldPlan.getVersion() + "创建")
                .status(0) // 草稿
                .createdBy(operatorId)
                .createdAt(LocalDateTime.now())
                .build();

        newPlan = curriculumPlanRepository.save(newPlan);

        // 复制课程
        List<PlanCourse> oldCourses = curriculumPlanRepository.findPlanCourses(id);
        if (!oldCourses.isEmpty()) {
            List<PlanCourse> newCourses = oldCourses.stream()
                    .map(c -> PlanCourse.builder()
                            .planId(newPlan.getId())
                            .courseId(c.getCourseId())
                            .semester(c.getSemester())
                            .weeklyHours(c.getWeeklyHours())
                            .examType(c.getExamType())
                            .isRequired(c.getIsRequired())
                            .build())
                    .collect(Collectors.toList());
            curriculumPlanRepository.savePlanCourses(newPlan.getId(), newCourses);
        }

        // 归档旧版本
        oldPlan.setStatus(2);
        oldPlan.setUpdatedBy(operatorId);
        oldPlan.setUpdatedAt(LocalDateTime.now());
        curriculumPlanRepository.save(oldPlan);

        log.info("创建培养方案新版本: newId={}, oldId={}, version={}", newPlan.getId(), id, newVersion);
        return newPlan.getId();
    }

    /**
     * 删除培养方案
     */
    @Transactional
    public void deletePlan(Long id) {
        CurriculumPlan plan = curriculumPlanRepository.findById(id)
                .orElseThrow(() -> new BusinessException("培养方案不存在: " + id));

        if (plan.getStatus() == 1) {
            throw new BusinessException("已发布的方案不能删除,请先归档");
        }

        curriculumPlanRepository.deleteById(id);
        log.info("删除培养方案成功: id={}", id);
    }

    /**
     * 获取培养方案详情
     */
    public CurriculumPlanDTO getPlan(Long id) {
        return curriculumPlanRepository.findByIdWithCourses(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("培养方案不存在: " + id));
    }

    /**
     * 根据专业和年份获取方案
     */
    public CurriculumPlanDTO getPlanByMajorAndYear(Long majorId, Integer enrollYear) {
        return curriculumPlanRepository.findByMajorIdAndEnrollYear(majorId, enrollYear)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * 获取所有方案
     */
    public List<CurriculumPlanDTO> getAllPlans() {
        return curriculumPlanRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据专业获取方案列表
     */
    public List<CurriculumPlanDTO> getPlansByMajor(Long majorId) {
        return curriculumPlanRepository.findByMajorId(majorId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询
     */
    public List<CurriculumPlanDTO> getPlansPage(int page, int size, Long majorId, Integer enrollYear, Integer status) {
        return curriculumPlanRepository.findPage(page, size, majorId, enrollYear, status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计数量
     */
    public long countPlans(Long majorId, Integer enrollYear, Integer status) {
        return curriculumPlanRepository.count(majorId, enrollYear, status);
    }

    private CurriculumPlanDTO toDTO(CurriculumPlan plan) {
        CurriculumPlanDTO dto = CurriculumPlanDTO.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .majorId(plan.getMajorId())
                .enrollYear(plan.getEnrollYear())
                .duration(plan.getDuration())
                .version(plan.getVersion())
                .totalCredits(plan.getTotalCredits())
                .requiredCredits(plan.getRequiredCredits())
                .electiveCredits(plan.getElectiveCredits())
                .practiceCredits(plan.getPracticeCredits())
                .objectives(plan.getObjectives())
                .requirements(plan.getRequirements())
                .remark(plan.getRemark())
                .status(plan.getStatus())
                .statusName(CurriculumPlanDTO.getStatusName(plan.getStatus()))
                .createdBy(plan.getCreatedBy())
                .createdAt(plan.getCreatedAt())
                .updatedBy(plan.getUpdatedBy())
                .updatedAt(plan.getUpdatedAt())
                .build();

        // 转换课程列表
        if (plan.getCourses() != null && !plan.getCourses().isEmpty()) {
            List<PlanCourseDTO> courseDTOs = plan.getCourses().stream()
                    .map(this::toPlanCourseDTO)
                    .collect(Collectors.toList());
            dto.setCourses(courseDTOs);
        }

        return dto;
    }

    private PlanCourseDTO toPlanCourseDTO(PlanCourse pc) {
        PlanCourseDTO dto = PlanCourseDTO.builder()
                .id(pc.getId())
                .planId(pc.getPlanId())
                .courseId(pc.getCourseId())
                .semester(pc.getSemester())
                .weeklyHours(pc.getWeeklyHours())
                .examType(pc.getExamType())
                .isRequired(pc.getIsRequired())
                .build();

        // 获取课程信息
        courseRepository.findById(pc.getCourseId()).ifPresent(course -> {
            dto.setCourseCode(course.getCourseCode());
            dto.setCourseName(course.getCourseName());
            dto.setCredits(course.getCredits());
            dto.setTotalHours(course.getTotalHours());
            dto.setCourseType(course.getCourseType());
            dto.setCourseTypeName(CourseDTO.getCourseTypeName(course.getCourseType()));
            dto.setCourseNature(course.getCourseNature());
            dto.setCourseNatureName(CourseDTO.getCourseNatureName(course.getCourseNature()));
        });

        return dto;
    }
}
