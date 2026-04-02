package com.school.management.application.inspection.v7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.v7.InspProjectRepository;
import com.school.management.domain.inspection.repository.v7.ProjectInspectorRepository;
import com.school.management.domain.inspection.repository.v7.ProjectScoreRepository;
import com.school.management.domain.inspection.repository.v7.ScoringProfileRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class InspProjectApplicationService {

    private final InspProjectRepository projectRepository;
    private final ProjectInspectorRepository inspectorRepository;
    private final ProjectScoreRepository scoreRepository;
    private final SpringDomainEventPublisher eventPublisher;
    private final ScoringProfileRepository scoringProfileRepository;
    private final TargetPopulationService targetPopulationService;
    private final ObjectMapper objectMapper;

    public InspProjectApplicationService(InspProjectRepository projectRepository,
                                          ProjectInspectorRepository inspectorRepository,
                                          ProjectScoreRepository scoreRepository,
                                          SpringDomainEventPublisher eventPublisher,
                                          ScoringProfileRepository scoringProfileRepository,
                                          TargetPopulationService targetPopulationService,
                                          ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.inspectorRepository = inspectorRepository;
        this.scoreRepository = scoreRepository;
        this.eventPublisher = eventPublisher;
        this.scoringProfileRepository = scoringProfileRepository;
        this.targetPopulationService = targetPopulationService;
        this.objectMapper = objectMapper;
    }

    // ========== Project CRUD ==========

    /**
     * 创建检查项目。rootSectionId 可为 null（多模板项目通过 InspectionPlan.rootSectionId 关联模板）。
     */
    @Transactional
    public InspProject createProject(String projectName, Long rootSectionId,
                                     LocalDate startDate, Long createdBy) {
        String projectCode = generateProjectCode();
        // rootSectionId 可空：null 表示项目使用多模板，模板通过计划关联
        InspProject project = InspProject.create(projectCode, projectName, rootSectionId, startDate, createdBy);
        return projectRepository.save(project);
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
    public InspProject updateProject(Long id, String projectName, Long rootSectionId,
                                     Long scoringProfileId, ScopeType scopeType,
                                     String scopeConfig,
                                     LocalDate startDate, LocalDate endDate,
                                     AssignmentMode assignmentMode, Boolean reviewRequired,
                                     Boolean autoPublish, Long updatedBy) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.updateInfo(projectName, rootSectionId, scoringProfileId,
                scopeType, scopeConfig, startDate, endDate,
                assignmentMode, reviewRequired, autoPublish, updatedBy);
        return projectRepository.save(project);
    }

    @Transactional
    public InspProject updateOperationalConfig(Long id, AssignmentMode assignmentMode,
                                                Boolean reviewRequired, Boolean autoPublish,
                                                String projectName, Long updatedBy) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.updateOperationalConfig(assignmentMode, reviewRequired, autoPublish, projectName, updatedBy);
        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        scoreRepository.deleteByProjectId(id);
        inspectorRepository.deleteByProjectId(id);
        projectRepository.deleteById(id);
    }

    // ========== Project Lifecycle ==========

    @Transactional
    public InspProject publishProject(Long id, Long templateVersionId) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));

        // 发布前校验
        if (project.getScopeConfig() == null || project.getScopeConfig().isBlank()
                || "[]".equals(project.getScopeConfig().trim())) {
            throw new IllegalStateException("请先配置检查范围，否则无法确定检查目标");
        }
        if (project.getStartDate() == null) {
            throw new IllegalStateException("请先设置开始日期");
        }

        // 校验检查范围内存在有效的检查对象
        List<TargetPopulationService.TargetInfo> targets = targetPopulationService.resolveTargets(
                project.getScopeType(), project.getScopeConfig(), TargetType.ORG);
        if (targets.isEmpty()) {
            throw new IllegalStateException("检查范围内未找到有效的检查对象，请检查范围配置");
        }

        // 锁定评分配置快照
        Long rootSectionId = project.getRootSectionId();
        if (rootSectionId != null) {
            scoringProfileRepository.findBySectionId(rootSectionId).ifPresentOrElse(profile -> {
                try {
                    Map<String, Object> snapshotMap = new HashMap<>();
                    snapshotMap.put("profileId", profile.getId());
                    snapshotMap.put("sectionId", profile.getSectionId());
                    snapshotMap.put("maxScore", profile.getMaxScore());
                    snapshotMap.put("minScore", profile.getMinScore());
                    snapshotMap.put("precisionDigits", profile.getPrecisionDigits());
                    snapshotMap.put("multiRaterMode", profile.getMultiRaterMode());
                    snapshotMap.put("calibrationEnabled", profile.getCalibrationEnabled());
                    snapshotMap.put("calibrationMethod", profile.getCalibrationMethod());
                    snapshotMap.put("trendFactorEnabled", profile.getTrendFactorEnabled());
                    snapshotMap.put("decayEnabled", profile.getDecayEnabled());
                    snapshotMap.put("decayMode", profile.getDecayMode());
                    snapshotMap.put("snapshotAt", LocalDateTime.now().toString());
                    String snapshot = objectMapper.writeValueAsString(snapshotMap);
                    project.lockScoringConfig(snapshot);
                    log.info("评分配置快照已锁定，projectId={}", project.getId());
                } catch (Exception e) {
                    log.error("序列化评分配置快照失败，projectId={}，将使用默认评分配置: {}", project.getId(), e.getMessage());
                }
            }, () -> {
                log.info("项目 {} 未配置评分方案，将使用默认评分逻辑", project.getId());
            });
        }

        project.publish(templateVersionId);
        InspProject saved = projectRepository.save(project);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        log.info("项目 {} 已发布", saved.getProjectCode());
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

    // ========== Scores ==========

    @Transactional(readOnly = true)
    public List<ProjectScore> listProjectScores(Long projectId) {
        return scoreRepository.findByProjectId(projectId);
    }

    private String generateProjectCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PRJ-" + dateStr + "-" + random;
    }
}
