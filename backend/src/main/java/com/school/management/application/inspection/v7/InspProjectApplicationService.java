package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.template.InspTemplate;
import com.school.management.domain.inspection.model.v7.template.TemplateModuleRef;
import com.school.management.domain.inspection.repository.v7.InspProjectRepository;
import com.school.management.domain.inspection.repository.v7.InspTemplateRepository;
import com.school.management.domain.inspection.repository.v7.ProjectInspectorRepository;
import com.school.management.domain.inspection.repository.v7.ProjectScoreRepository;
import com.school.management.domain.inspection.repository.v7.TemplateModuleRefRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class InspProjectApplicationService {

    private final InspProjectRepository projectRepository;
    private final ProjectInspectorRepository inspectorRepository;
    private final InspTemplateRepository templateRepository;
    private final TemplateModuleRefRepository moduleRefRepository;
    private final ProjectScoreRepository scoreRepository;
    private final SpringDomainEventPublisher eventPublisher;

    // ========== Project CRUD ==========

    @Transactional
    public InspProject createProject(String projectName, Long templateId,
                                     LocalDate startDate, Long createdBy) {
        String projectCode = generateProjectCode();
        InspProject project = InspProject.create(projectCode, projectName, templateId, startDate, createdBy);
        InspProject saved = projectRepository.save(project);

        // 展开模块引用为子项目
        expandModuleRefsToChildProjects(saved, createdBy);

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<InspProject> getProject(Long id) {
        return projectRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<InspProject> listProjects() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<InspProject> listProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status);
    }

    @Transactional
    public InspProject updateProject(Long id, String projectName, Long templateId,
                                     Long scoringProfileId, ScopeType scopeType,
                                     String scopeConfig, TargetType targetType,
                                     LocalDate startDate, LocalDate endDate,
                                     CycleType cycleType, String cycleConfig,
                                     String timeSlots, Boolean skipHolidays,
                                     Long holidayCalendarId, String excludedDates,
                                     AssignmentMode assignmentMode, Boolean reviewRequired,
                                     Boolean autoPublish, Long updatedBy) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.updateInfo(projectName, templateId, scoringProfileId,
                scopeType, scopeConfig, targetType, startDate, endDate,
                cycleType, cycleConfig, timeSlots, skipHolidays,
                holidayCalendarId, excludedDates, assignmentMode,
                reviewRequired, autoPublish, updatedBy);
        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        // 递归删除子项目
        List<InspProject> children = projectRepository.findByParentProjectId(id);
        for (InspProject child : children) {
            deleteProject(child.getId());
        }
        scoreRepository.deleteByProjectId(id);
        inspectorRepository.deleteByProjectId(id);
        projectRepository.deleteById(id);
    }

    // ========== Project Lifecycle ==========

    @Transactional
    public InspProject publishProject(Long id, Long templateVersionId) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.publish(templateVersionId);
        InspProject saved = projectRepository.save(project);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspProject pauseProject(Long id) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.pause();
        InspProject saved = projectRepository.save(project);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspProject resumeProject(Long id) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.resume();
        InspProject saved = projectRepository.save(project);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspProject completeProject(Long id) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.complete();
        InspProject saved = projectRepository.save(project);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspProject archiveProject(Long id) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.archive();
        return projectRepository.save(project);
    }

    // ========== Inspector Pool ==========

    @Transactional
    public ProjectInspector addInspector(Long projectId, Long userId,
                                         String userName, InspectorRole role) {
        ProjectInspector inspector = ProjectInspector.create(projectId, userId, userName, role);
        return inspectorRepository.save(inspector);
    }

    @Transactional(readOnly = true)
    public List<ProjectInspector> listInspectors(Long projectId) {
        return inspectorRepository.findByProjectId(projectId);
    }

    @Transactional
    public void removeInspector(Long inspectorId) {
        inspectorRepository.deleteById(inspectorId);
    }

    // ========== Child Projects ==========

    @Transactional(readOnly = true)
    public List<InspProject> listChildProjects(Long parentProjectId) {
        return projectRepository.findByParentProjectId(parentProjectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectScore> listProjectScores(Long projectId) {
        return scoreRepository.findByProjectId(projectId);
    }

    // ========== Internal ==========

    /**
     * 展开组合模板的模块引用为子项目（递归）
     */
    private void expandModuleRefsToChildProjects(InspProject parentProject, Long createdBy) {
        List<TemplateModuleRef> moduleRefs = moduleRefRepository.findByCompositeTemplateId(parentProject.getTemplateId());
        if (moduleRefs.isEmpty()) {
            return;
        }

        for (TemplateModuleRef ref : moduleRefs) {
            InspTemplate moduleTemplate = templateRepository.findById(ref.getModuleTemplateId())
                    .orElse(null);
            if (moduleTemplate == null) {
                log.warn("模块模板不存在: {}, 跳过", ref.getModuleTemplateId());
                continue;
            }

            String childCode = generateProjectCode();
            String childName = parentProject.getProjectName() + " - " + moduleTemplate.getTemplateName();

            InspProject childProject = InspProject.reconstruct(InspProject.builder()
                    .projectCode(childCode)
                    .projectName(childName)
                    .parentProjectId(parentProject.getId())
                    .templateId(ref.getModuleTemplateId())
                    .startDate(parentProject.getStartDate())
                    .endDate(parentProject.getEndDate())
                    .scopeType(parentProject.getScopeType())
                    .scopeConfig(parentProject.getScopeConfig())
                    .targetType(moduleTemplate.getTargetType())
                    .cycleType(parentProject.getCycleType())
                    .cycleConfig(parentProject.getCycleConfig())
                    .timeSlots(parentProject.getTimeSlots())
                    .skipHolidays(parentProject.getSkipHolidays())
                    .holidayCalendarId(parentProject.getHolidayCalendarId())
                    .excludedDates(parentProject.getExcludedDates())
                    .assignmentMode(parentProject.getAssignmentMode())
                    .reviewRequired(parentProject.getReviewRequired())
                    .autoPublish(parentProject.getAutoPublish())
                    .status(ProjectStatus.DRAFT)
                    .createdBy(createdBy));

            InspProject savedChild = projectRepository.save(childProject);

            // 递归展开子模板的模块引用
            expandModuleRefsToChildProjects(savedChild, createdBy);
        }
    }

    private String generateProjectCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PRJ-" + dateStr + "-" + random;
    }
}
