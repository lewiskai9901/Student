package com.school.management.application.inspection.v6;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v6.*;
import com.school.management.domain.inspection.repository.v6.InspectionProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * V6检查项目应用服务
 */
@Service
public class InspectionProjectApplicationService {

    private static final Logger log = LoggerFactory.getLogger(InspectionProjectApplicationService.class);

    private final InspectionProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    public InspectionProjectApplicationService(InspectionProjectRepository projectRepository,
                                                ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建项目（草稿）
     */
    @Transactional
    public InspectionProject createProject(CreateProjectCommand command) {
        // 生成项目编码
        String projectCode = generateProjectCode();

        InspectionProject project = InspectionProject.create(
                projectCode,
                command.getProjectName(),
                command.getTemplateId(),
                command.getCreatedBy()
        );

        project.setDescription(command.getDescription());

        return projectRepository.save(project);
    }

    /**
     * 更新项目配置（仅草稿状态）
     */
    @Transactional
    public InspectionProject updateProjectConfig(Long projectId, UpdateProjectConfigCommand command) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        project.updateConfig(
                command.getScopeType(),
                command.getScopeConfig(),
                command.getStartDate(),
                command.getEndDate(),
                command.getCycleType(),
                command.getCycleConfig(),
                command.getTimeSlots(),
                command.isSkipHolidays(),
                command.getSharedSpaceStrategy(),
                command.getInspectorAssignmentMode()
        );

        return projectRepository.save(project);
    }

    /**
     * 发布项目
     */
    @Transactional
    public InspectionProject publishProject(Long projectId, String templateSnapshot) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        project.publish(templateSnapshot);

        return projectRepository.save(project);
    }

    /**
     * 暂停项目
     */
    @Transactional
    public InspectionProject pauseProject(Long projectId) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        project.pause();

        return projectRepository.save(project);
    }

    /**
     * 恢复项目
     */
    @Transactional
    public InspectionProject resumeProject(Long projectId) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        project.resume();

        return projectRepository.save(project);
    }

    /**
     * 完成项目
     */
    @Transactional
    public InspectionProject completeProject(Long projectId) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        project.complete();

        return projectRepository.save(project);
    }

    /**
     * 归档项目
     */
    @Transactional
    public InspectionProject archiveProject(Long projectId) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        project.archive();

        return projectRepository.save(project);
    }

    /**
     * 获取项目详情
     */
    public Optional<InspectionProject> getProject(Long projectId) {
        return projectRepository.findById(projectId);
    }

    /**
     * 根据编码获取项目
     */
    public Optional<InspectionProject> getProjectByCode(String projectCode) {
        return projectRepository.findByProjectCode(projectCode);
    }

    /**
     * 获取活跃项目列表（用于任务生成）
     */
    public List<InspectionProject> getActiveProjectsForDate(LocalDate date) {
        return projectRepository.findActiveProjectsForDate(date);
    }

    /**
     * 分页查询项目
     */
    public List<InspectionProject> listProjects(int page, int size, ProjectStatus status, String keyword) {
        return projectRepository.findPagedWithConditions(page, size, status, keyword);
    }

    /**
     * 统计项目数量
     */
    public long countProjects(ProjectStatus status, String keyword) {
        return projectRepository.countWithConditions(status, keyword);
    }

    /**
     * 删除项目（仅草稿状态）
     */
    @Transactional
    public void deleteProject(Long projectId) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的项目才能删除");
        }

        projectRepository.delete(projectId);
    }

    private String generateProjectCode() {
        String prefix = "PRJ";
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + date + random;
    }

    // Command classes
    public static class CreateProjectCommand {
        private String projectName;
        private String description;
        private Long templateId;
        private Long createdBy;

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    }

    public static class UpdateProjectConfigCommand {
        private ScopeType scopeType;
        private String scopeConfig;
        private LocalDate startDate;
        private LocalDate endDate;
        private CycleType cycleType;
        private String cycleConfig;
        private String timeSlots;
        private boolean skipHolidays;
        private SharedSpaceStrategy sharedSpaceStrategy;
        private InspectorAssignmentMode inspectorAssignmentMode;

        public ScopeType getScopeType() { return scopeType; }
        public void setScopeType(ScopeType scopeType) { this.scopeType = scopeType; }
        public String getScopeConfig() { return scopeConfig; }
        public void setScopeConfig(String scopeConfig) { this.scopeConfig = scopeConfig; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public CycleType getCycleType() { return cycleType; }
        public void setCycleType(CycleType cycleType) { this.cycleType = cycleType; }
        public String getCycleConfig() { return cycleConfig; }
        public void setCycleConfig(String cycleConfig) { this.cycleConfig = cycleConfig; }
        public String getTimeSlots() { return timeSlots; }
        public void setTimeSlots(String timeSlots) { this.timeSlots = timeSlots; }
        public boolean isSkipHolidays() { return skipHolidays; }
        public void setSkipHolidays(boolean skipHolidays) { this.skipHolidays = skipHolidays; }
        public SharedSpaceStrategy getSharedSpaceStrategy() { return sharedSpaceStrategy; }
        public void setSharedSpaceStrategy(SharedSpaceStrategy sharedSpaceStrategy) { this.sharedSpaceStrategy = sharedSpaceStrategy; }
        public InspectorAssignmentMode getInspectorAssignmentMode() { return inspectorAssignmentMode; }
        public void setInspectorAssignmentMode(InspectorAssignmentMode inspectorAssignmentMode) { this.inspectorAssignmentMode = inspectorAssignmentMode; }
    }
}
