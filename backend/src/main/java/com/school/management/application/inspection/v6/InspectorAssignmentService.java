package com.school.management.application.inspection.v6;

import com.school.management.infrastructure.persistence.inspection.v6.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * V6检查员分配服务
 */
@Service
public class InspectorAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(InspectorAssignmentService.class);

    private final InspectorAssignmentMapper assignmentMapper;
    private final ProjectInspectorConfigMapper configMapper;

    public InspectorAssignmentService(InspectorAssignmentMapper assignmentMapper,
                                       ProjectInspectorConfigMapper configMapper) {
        this.assignmentMapper = assignmentMapper;
        this.configMapper = configMapper;
    }

    // ========== 项目检查员配置 ==========

    /**
     * 获取项目的检查员配置
     */
    public List<ProjectInspectorConfigPO> getProjectInspectors(Long projectId) {
        return configMapper.findByProjectId(projectId);
    }

    /**
     * 获取项目的默认检查员
     */
    public List<ProjectInspectorConfigPO> getDefaultInspectors(Long projectId) {
        return configMapper.findDefaultInspectors(projectId);
    }

    /**
     * 添加项目检查员
     */
    @Transactional
    public ProjectInspectorConfigPO addProjectInspector(Long projectId, Long inspectorId,
                                                         String inspectorName, boolean isDefault,
                                                         String scopeType, String scopeIds,
                                                         Long createdBy) {
        // 检查是否已存在
        ProjectInspectorConfigPO existing = configMapper.findByProjectAndInspector(projectId, inspectorId);
        if (existing != null) {
            throw new IllegalStateException("该检查员已添加到项目");
        }

        ProjectInspectorConfigPO config = new ProjectInspectorConfigPO();
        config.setProjectId(projectId);
        config.setInspectorId(inspectorId);
        config.setInspectorName(inspectorName);
        config.setIsDefault(isDefault);
        config.setScopeType(scopeType);
        config.setScopeIds(scopeIds);
        config.setCreatedBy(createdBy);

        configMapper.insert(config);
        return config;
    }

    /**
     * 批量添加项目检查员
     */
    @Transactional
    public void addProjectInspectors(Long projectId, List<InspectorConfig> inspectors, Long createdBy) {
        if (inspectors == null || inspectors.isEmpty()) {
            return;
        }

        List<ProjectInspectorConfigPO> configs = new ArrayList<>();
        for (InspectorConfig inspector : inspectors) {
            ProjectInspectorConfigPO config = new ProjectInspectorConfigPO();
            config.setProjectId(projectId);
            config.setInspectorId(inspector.getInspectorId());
            config.setInspectorName(inspector.getInspectorName());
            config.setIsDefault(inspector.isDefault());
            config.setScopeType(inspector.getScopeType());
            config.setScopeIds(inspector.getScopeIds());
            config.setCreatedBy(createdBy);
            configs.add(config);
        }

        configMapper.batchInsert(configs);
    }

    /**
     * 移除项目检查员
     */
    @Transactional
    public void removeProjectInspector(Long projectId, Long inspectorId) {
        configMapper.deleteByProjectAndInspector(projectId, inspectorId);
    }

    /**
     * 清空项目检查员
     */
    @Transactional
    public void clearProjectInspectors(Long projectId) {
        configMapper.deleteByProjectId(projectId);
    }

    // ========== 任务检查员分配 ==========

    /**
     * 获取任务的检查员分配
     */
    public List<TaskInspectorAssignmentPO> getTaskAssignments(Long taskId) {
        return assignmentMapper.findByTaskId(taskId);
    }

    /**
     * 分配检查员到任务
     */
    @Transactional
    public TaskInspectorAssignmentPO assignInspector(Long taskId, Long inspectorId,
                                                      String inspectorName, String scopeType,
                                                      String scopeIds, Long createdBy) {
        // 检查是否已分配
        TaskInspectorAssignmentPO existing = assignmentMapper.findByTaskAndInspector(taskId, inspectorId);
        if (existing != null) {
            throw new IllegalStateException("该检查员已分配到此任务");
        }

        TaskInspectorAssignmentPO assignment = new TaskInspectorAssignmentPO();
        assignment.setTaskId(taskId);
        assignment.setInspectorId(inspectorId);
        assignment.setInspectorName(inspectorName);
        assignment.setScopeType(scopeType);
        assignment.setScopeIds(scopeIds);
        assignment.setStatus("ASSIGNED");
        assignment.setCreatedBy(createdBy);

        assignmentMapper.insert(assignment);
        return assignment;
    }

    /**
     * 批量分配检查员
     */
    @Transactional
    public void assignInspectors(Long taskId, List<InspectorAssignment> assignments, Long createdBy) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        // 先清除现有分配
        assignmentMapper.deleteByTaskId(taskId);

        List<TaskInspectorAssignmentPO> pos = new ArrayList<>();
        for (InspectorAssignment assignment : assignments) {
            TaskInspectorAssignmentPO po = new TaskInspectorAssignmentPO();
            po.setTaskId(taskId);
            po.setInspectorId(assignment.getInspectorId());
            po.setInspectorName(assignment.getInspectorName());
            po.setScopeType(assignment.getScopeType());
            po.setScopeIds(assignment.getScopeIds());
            po.setStatus("ASSIGNED");
            po.setCreatedBy(createdBy);
            pos.add(po);
        }

        assignmentMapper.batchInsert(pos);
    }

    /**
     * 接受任务分配
     */
    @Transactional
    public void acceptAssignment(Long assignmentId) {
        assignmentMapper.updateStatus(assignmentId, "ACCEPTED");
    }

    /**
     * 拒绝任务分配
     */
    @Transactional
    public void declineAssignment(Long assignmentId) {
        assignmentMapper.updateStatus(assignmentId, "DECLINED");
    }

    /**
     * 清除任务分配
     */
    @Transactional
    public void clearTaskAssignments(Long taskId) {
        assignmentMapper.deleteByTaskId(taskId);
    }

    /**
     * 自动分配检查员（根据项目配置）
     */
    @Transactional
    public void autoAssignInspectors(Long taskId, Long projectId, Long createdBy) {
        // 获取项目默认检查员
        List<ProjectInspectorConfigPO> defaultInspectors = configMapper.findDefaultInspectors(projectId);
        if (defaultInspectors.isEmpty()) {
            log.warn("项目 {} 没有配置默认检查员", projectId);
            return;
        }

        List<TaskInspectorAssignmentPO> assignments = new ArrayList<>();
        for (ProjectInspectorConfigPO config : defaultInspectors) {
            TaskInspectorAssignmentPO assignment = new TaskInspectorAssignmentPO();
            assignment.setTaskId(taskId);
            assignment.setInspectorId(config.getInspectorId());
            assignment.setInspectorName(config.getInspectorName());
            assignment.setScopeType(config.getScopeType());
            assignment.setScopeIds(config.getScopeIds());
            assignment.setStatus("ASSIGNED");
            assignment.setCreatedBy(createdBy);
            assignments.add(assignment);
        }

        assignmentMapper.batchInsert(assignments);
    }

    // ========== DTO类 ==========

    public static class InspectorConfig {
        private Long inspectorId;
        private String inspectorName;
        private boolean isDefault;
        private String scopeType;
        private String scopeIds;

        public Long getInspectorId() { return inspectorId; }
        public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
        public String getInspectorName() { return inspectorName; }
        public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean aDefault) { isDefault = aDefault; }
        public String getScopeType() { return scopeType; }
        public void setScopeType(String scopeType) { this.scopeType = scopeType; }
        public String getScopeIds() { return scopeIds; }
        public void setScopeIds(String scopeIds) { this.scopeIds = scopeIds; }
    }

    public static class InspectorAssignment {
        private Long inspectorId;
        private String inspectorName;
        private String scopeType;
        private String scopeIds;

        public Long getInspectorId() { return inspectorId; }
        public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
        public String getInspectorName() { return inspectorName; }
        public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
        public String getScopeType() { return scopeType; }
        public void setScopeType(String scopeType) { this.scopeType = scopeType; }
        public String getScopeIds() { return scopeIds; }
        public void setScopeIds(String scopeIds) { this.scopeIds = scopeIds; }
    }
}
