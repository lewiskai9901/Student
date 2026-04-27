package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.model.template.TemplateVersion;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.ProjectInspectorRepository;
import com.school.management.domain.inspection.repository.ProjectScoreRepository;
import com.school.management.domain.inspection.repository.ScoringProfileRepository;
import com.school.management.domain.inspection.repository.TemplateSectionRepository;
import com.school.management.domain.inspection.repository.TemplateVersionRepository;
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
    private final TemplateSectionRepository templateSectionRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final InspectionAuditLogger auditLogger;

    public InspProjectApplicationService(InspProjectRepository projectRepository,
                                          ProjectInspectorRepository inspectorRepository,
                                          ProjectScoreRepository scoreRepository,
                                          SpringDomainEventPublisher eventPublisher,
                                          ScoringProfileRepository scoringProfileRepository,
                                          TargetPopulationService targetPopulationService,
                                          ObjectMapper objectMapper,
                                          TemplateSectionRepository templateSectionRepository,
                                          TemplateVersionRepository templateVersionRepository,
                                          InspectionAuditLogger auditLogger) {
        this.projectRepository = projectRepository;
        this.inspectorRepository = inspectorRepository;
        this.scoreRepository = scoreRepository;
        this.eventPublisher = eventPublisher;
        this.scoringProfileRepository = scoringProfileRepository;
        this.targetPopulationService = targetPopulationService;
        this.objectMapper = objectMapper;
        this.templateSectionRepository = templateSectionRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.auditLogger = auditLogger;
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
    public InspProject publishProject(Long id, Long templateVersionIdHint) {
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

        // P1#7: 服务端自动派生 templateVersionId — 不信任 caller 提交的值
        // 单模板项目: rootSectionId → section.templateId → 该模板 latest published version
        // 多模板项目 (rootSectionId == null): templateVersionId 留空, 由 InspectionPlan 处理
        Long resolvedVersionId = resolveTemplateVersionId(project, templateVersionIdHint);

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

        project.publish(resolvedVersionId);
        InspProject saved = projectRepository.save(project);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        log.info("项目 {} 已发布, 锁定模板快照 templateVersionId={}",
                saved.getProjectCode(), resolvedVersionId);
        return saved;
    }

    /**
     * P1#7 follow-up: 把已发布项目的模板快照升级到模板的最新已发布版本.
     * 用于解决模板漂移导致的任务创建被拒.
     *
     * <p>仅 PUBLISHED / PAUSED 项目可升级 — 草稿项目自动在 publish 时取最新版本无需手动升级.
     */
    @Transactional
    public InspProject upgradeTemplateVersion(Long projectId) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        if (project.getStatus() != ProjectStatus.PUBLISHED && project.getStatus() != ProjectStatus.PAUSED) {
            throw new IllegalStateException(
                    "只有已发布或已暂停的项目才能升级模板版本, 当前状态: " + project.getStatus());
        }
        Long rootSectionId = project.getRootSectionId();
        if (rootSectionId == null) {
            throw new IllegalStateException("多模板项目不在 project 层锁定 templateVersionId, 请通过计划管理");
        }
        TemplateSection section = templateSectionRepository.findById(rootSectionId)
                .orElseThrow(() -> new IllegalStateException("根分区不存在: " + rootSectionId));
        Long templateId = section.getTemplateId();
        TemplateVersion latest = templateVersionRepository.findLatestByTemplateId(templateId)
                .orElseThrow(() -> new IllegalStateException("模板尚未发布任何版本"));

        Long currentVersionId = project.getTemplateVersionId();
        if (latest.getId().equals(currentVersionId)) {
            log.info("项目 {} 模板版本已是最新 ({}), 无需升级", project.getProjectCode(), currentVersionId);
            return project;
        }

        project.relockTemplateVersion(latest.getId());
        InspProject saved = projectRepository.save(project);
        // C: 审计日志
        auditLogger.log("InspProject", saved.getId(), saved.getProjectCode(),
                "PROJECT_TEMPLATE_UPGRADED", null,
                Map.of("previousVersionId", currentVersionId != null ? currentVersionId : 0L,
                        "newVersionId", latest.getId()));
        log.info("项目 {} 模板版本已从 {} 升级到 {}",
                saved.getProjectCode(), currentVersionId, latest.getId());
        return saved;
    }

    /**
     * P1#7: 服务端可信地派生 templateVersionId — 防止 caller 传入任意 versionId 绕过快照锁定.
     *
     * <p>规则:
     * <ul>
     *   <li>多模板项目 (rootSectionId == null): 不锁定 templateVersionId, 返回 null (由 plan 处理)</li>
     *   <li>单模板项目: 自动取该模板的 latest published version</li>
     *   <li>caller 提供的 hint 仅用于校验一致性 — 必须属于同一个 templateId, 否则拒绝</li>
     *   <li>模板未发布 (无任何 version): 拒绝项目发布</li>
     * </ul>
     */
    private Long resolveTemplateVersionId(InspProject project, Long hint) {
        Long rootSectionId = project.getRootSectionId();
        if (rootSectionId == null) {
            // 多模板项目: 不在 project 层锁定快照, 由 plan 关联的具体模板各自锁定
            if (hint != null) {
                log.warn("多模板项目 {} 不应在 project 层指定 templateVersionId, 已忽略 hint={}",
                        project.getProjectCode(), hint);
            }
            return null;
        }

        TemplateSection section = templateSectionRepository.findById(rootSectionId)
                .orElseThrow(() -> new IllegalStateException(
                        "rootSectionId " + rootSectionId + " 对应的分区不存在"));
        Long templateId = section.getTemplateId();
        if (templateId == null) {
            throw new IllegalStateException("根分区 " + rootSectionId + " 未关联模板, 无法派生版本快照");
        }

        TemplateVersion latest = templateVersionRepository.findLatestByTemplateId(templateId)
                .orElseThrow(() -> new IllegalStateException(
                        "模板 " + templateId + " 尚未发布任何版本, 请先发布模板再发布项目"));

        // 校验 hint 一致性: 若 caller 显式给了一个 versionId, 必须属于该模板
        if (hint != null && !hint.equals(latest.getId())) {
            TemplateVersion hinted = templateVersionRepository.findById(hint).orElse(null);
            if (hinted == null) {
                throw new IllegalArgumentException("指定的 templateVersionId=" + hint + " 不存在");
            }
            if (!templateId.equals(hinted.getTemplateId())) {
                throw new IllegalArgumentException(
                        "指定的 templateVersionId=" + hint + " 属于模板 " + hinted.getTemplateId()
                        + ", 与项目根分区所属模板 " + templateId + " 不一致");
            }
            // hint 属于同模板的旧版本: 允许但记录, 让运维有意识地选择历史快照
            log.warn("项目 {} 锁定旧版本快照 hint={} (模板 {} 最新版本是 {})",
                    project.getProjectCode(), hint, templateId, latest.getId());
            return hint;
        }
        return latest.getId();
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
