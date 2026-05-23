package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.inspection.dto.CloneProjectCommand;
import com.school.management.application.inspection.dto.ProjectStatsSummary;
import com.school.management.domain.inspection.repository.projection.ProjectTaskStats;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.model.template.TemplateVersion;
import com.school.management.domain.inspection.repository.CalculationRuleRepository;
import com.school.management.domain.inspection.repository.GradeBandRepository;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.InspectionPlanRepository;
import com.school.management.domain.inspection.repository.ProjectInspectorRepository;
import com.school.management.domain.inspection.repository.ProjectScoreRepository;
import com.school.management.domain.inspection.repository.ScoreDimensionRepository;
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

@Slf4j
@Service
public class InspProjectApplicationService {

    private final InspProjectRepository projectRepository;
    private final ProjectInspectorRepository inspectorRepository;
    private final ProjectScoreRepository scoreRepository;
    private final SpringDomainEventPublisher eventPublisher;
    private final ScoringProfileRepository scoringProfileRepository;
    private final ScoreDimensionRepository scoreDimensionRepository;
    private final GradeBandRepository gradeBandRepository;
    private final CalculationRuleRepository calculationRuleRepository;
    private final TargetPopulationService targetPopulationService;
    private final ObjectMapper objectMapper;
    private final TemplateSectionRepository templateSectionRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final InspectionAuditLogger auditLogger;
    private final InspTaskRepository taskRepoForStats;
    private final InspectionPlanRepository inspectionPlanRepository;
    private final IndicatorRepository indicatorRepository;
    private final ScoringProfileApplicationService scoringProfileService;

    public InspProjectApplicationService(InspProjectRepository projectRepository,
                                          ProjectInspectorRepository inspectorRepository,
                                          ProjectScoreRepository scoreRepository,
                                          SpringDomainEventPublisher eventPublisher,
                                          ScoringProfileRepository scoringProfileRepository,
                                          ScoreDimensionRepository scoreDimensionRepository,
                                          GradeBandRepository gradeBandRepository,
                                          CalculationRuleRepository calculationRuleRepository,
                                          TargetPopulationService targetPopulationService,
                                          ObjectMapper objectMapper,
                                          TemplateSectionRepository templateSectionRepository,
                                          TemplateVersionRepository templateVersionRepository,
                                          InspectionAuditLogger auditLogger,
                                          InspTaskRepository taskRepoForStats,
                                          InspectionPlanRepository inspectionPlanRepository,
                                          IndicatorRepository indicatorRepository,
                                          @Lazy ScoringProfileApplicationService scoringProfileService) {
        this.projectRepository = projectRepository;
        this.inspectorRepository = inspectorRepository;
        this.scoreRepository = scoreRepository;
        this.eventPublisher = eventPublisher;
        this.scoringProfileRepository = scoringProfileRepository;
        this.scoreDimensionRepository = scoreDimensionRepository;
        this.gradeBandRepository = gradeBandRepository;
        this.calculationRuleRepository = calculationRuleRepository;
        this.targetPopulationService = targetPopulationService;
        this.objectMapper = objectMapper;
        this.templateSectionRepository = templateSectionRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.auditLogger = auditLogger;
        this.taskRepoForStats = taskRepoForStats;
        this.inspectionPlanRepository = inspectionPlanRepository;
        this.indicatorRepository = indicatorRepository;
        this.scoringProfileService = scoringProfileService;
    }

    // ========== Project CRUD ==========

    /**
     * 创建检查项目。rootSectionId 可为 null（多模板项目通过 InspectionPlan.rootSectionId 关联模板）。
     * orgUnitId 强制非空 — 数据权限边界, 标记项目覆盖到哪个组织单元.
     */
    @Transactional
    public InspProject createProject(String projectName, Long rootSectionId,
                                     LocalDate startDate, Long orgUnitId, Long createdBy) {
        String projectCode = generateProjectCode();
        // rootSectionId 可空：null 表示项目使用多模板，模板通过计划关联
        InspProject project = InspProject.create(projectCode, projectName, rootSectionId, startDate, orgUnitId, createdBy);
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

    /**
     * 列表页 N+1 消除: 一次返回项目 + 任务统计 + 检查员人数.
     */
    @Transactional(readOnly = true)
    public List<ProjectStatsSummary> listProjectsWithStats(ProjectStatus status) {
        List<InspProject> projects = (status != null)
                ? projectRepository.findByStatus(status)
                : projectRepository.findAll();
        if (projects.isEmpty()) return java.util.Collections.emptyList();
        List<Long> ids = projects.stream().map(InspProject::getId).toList();
        Map<Long, ProjectTaskStats> statsMap = new HashMap<>();
        // taskRepository 需要 lazy 注入, 否则会循环依赖. 这里直接用 lookup.
        for (ProjectTaskStats s : taskRepoForStats.findStatsByProjectIds(ids)) {
            statsMap.put(s.getProjectId(), s);
        }
        Map<Long, Integer> inspectorCountMap = inspectorRepository.countByProjectIds(ids);
        return projects.stream().map(p -> {
            ProjectTaskStats s = statsMap.get(p.getId());
            return new ProjectStatsSummary(
                    p,
                    s == null ? 0 : s.getTotal(),
                    s == null ? 0 : s.getDone(),
                    s == null ? 0 : s.getOverdue(),
                    s == null ? 0 : s.getPendingReview(),
                    inspectorCountMap.getOrDefault(p.getId(), 0));
        }).toList();
    }

    @Transactional
    public InspProject updateProject(Long id, String projectName, Long rootSectionId,
                                     Long defaultScoringProfileId, ScopeType scopeType,
                                     String scopeConfig,
                                     LocalDate startDate, LocalDate endDate,
                                     AssignmentMode assignmentMode, Boolean reviewRequired,
                                     Boolean autoPublish, Long updatedBy) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.updateInfo(projectName, rootSectionId, defaultScoringProfileId,
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

    /**
     * P1#7: 删除项目.
     *
     * <p>禁止删除已产生任务的项目 — 硬删会留下 insp_tasks / insp_submissions /
     * insp_corrective_cases / inspection_appeals / inspection_plans 等一批孤儿,
     * 这些表通过 project_id 关联但无 FK 级联. 报表 / 审计追溯会拿到悬挂引用.
     *
     * <p>选择"禁止"而非"级联清理": 已检查产生的数据是考核证据, 静默级联删除
     * 会抹掉审计痕迹, 风险远高于让管理员先归档 (archiveProject) 项目.
     */
    @Transactional
    public void deleteProject(Long id) {
        projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        long taskCount = taskRepoForStats.findByProjectId(id).size();
        if (taskCount > 0) {
            throw new com.school.management.exception.BusinessException(
                    "项目已产生 " + taskCount + " 个检查任务, 不能删除. " +
                    "如需停用请改用归档 (archiveProject), 以保留检查记录与审计痕迹.");
        }
        scoreRepository.deleteByProjectId(id);
        inspectorRepository.deleteByProjectId(id);
        // 评分配置项目-owned 重构 (2026-05-23): 删除项目同时清理 owned ScoringProfile
        // 与关联子实体 (dimensions/rules/bands). 顺序: 子 → profile → project,
        // 避免删 profile 后留下孤儿 dimensions/rules/bands.
        cascadeDeleteScoringProfiles(id);
        projectRepository.deleteById(id);
    }

    /**
     * 级联清理项目 owned 的 ScoringProfile 及其关联实体.
     * 顺序: dimensions → rules → grade_bands → profile (反依赖顺序).
     */
    private void cascadeDeleteScoringProfiles(Long projectId) {
        List<com.school.management.domain.inspection.model.scoring.ScoringProfile> profiles =
                scoringProfileRepository.findByProjectId(projectId);
        for (var p : profiles) {
            Long pid = p.getId();
            scoreDimensionRepository.deleteByScoringProfileId(pid);
            calculationRuleRepository.deleteByScoringProfileId(pid);
            gradeBandRepository.deleteByScoringProfileId(pid);
        }
        int n = scoringProfileRepository.deleteByProjectId(projectId);
        if (n > 0) {
            log.info("项目 {} 删除级联清理 owned ScoringProfile: {} 套", projectId, n);
        }
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
                    // P2#13: 快照锁定是项目发布的核心契约 — 序列化失败若仅 log 继续,
                    // 项目会以"无快照"状态发布, 后续任务评分将与发布时承诺脱节.
                    // 失败即抛, 让 @Transactional 回滚整个 publish.
                    log.error("序列化评分配置快照失败，projectId={}: {}", project.getId(), e.getMessage());
                    throw new com.school.management.exception.BusinessException(
                            "评分配置快照序列化失败, 项目发布已中止: " + e.getMessage());
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
     * review #7: 更新项目级策略 (max_reject_count / max_escalation_level / appeal_window_days).
     */
    @Transactional
    public InspProject updatePolicyConfig(Long id, Integer maxRejectCount,
                                           Integer maxEscalationLevel, Integer appealWindowDays,
                                           Long updatedBy) {
        InspProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        project.updatePolicyConfig(maxRejectCount, maxEscalationLevel, appealWindowDays, updatedBy);
        InspProject saved = projectRepository.save(project);
        auditLogger.log("InspProject", saved.getId(), saved.getProjectCode(),
                "PROJECT_POLICY_UPDATED", null,
                java.util.Map.of(
                        "maxRejectCount", maxRejectCount != null ? maxRejectCount : "default",
                        "maxEscalationLevel", maxEscalationLevel != null ? maxEscalationLevel : "default",
                        "appealWindowDays", appealWindowDays != null ? appealWindowDays : "default"));
        return saved;
    }

    /**
     * review #12: 查询项目模板版本状态 (当前锁定 vs 模板最新).
     * 返回字段: drifted (是否漂移), currentVersionId, currentVersionNumber,
     *         latestVersionId, latestVersionNumber, templateId.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTemplateVersionStatus(Long projectId) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        Map<String, Object> result = new HashMap<>();
        result.put("drifted", false);
        result.put("currentVersionId", project.getTemplateVersionId());
        result.put("rootSectionId", project.getRootSectionId());

        if (project.getRootSectionId() == null) {
            result.put("multiTemplate", true);
            return result; // 多模板项目无须在 project 层比对
        }
        TemplateSection section = templateSectionRepository.findById(project.getRootSectionId()).orElse(null);
        if (section == null || section.getTemplateId() == null) return result;
        result.put("templateId", section.getTemplateId());

        // 当前锁定版本号
        if (project.getTemplateVersionId() != null) {
            templateVersionRepository.findById(project.getTemplateVersionId())
                    .ifPresent(cur -> result.put("currentVersionNumber", cur.getVersion()));
        }
        // 最新版本
        templateVersionRepository.findLatestByTemplateId(section.getTemplateId()).ifPresent(latest -> {
            result.put("latestVersionId", latest.getId());
            result.put("latestVersionNumber", latest.getVersion());
            result.put("drifted", !latest.getId().equals(project.getTemplateVersionId()));
        });
        return result;
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

    // ========== Project Clone ==========

    /**
     * 深拷贝项目: 复制项目设置 + 全部 owned ScoringProfile (+ dimensions/bands/rules)
     * + InspectionPlans + Indicators 到一个全新的 DRAFT 项目.
     *
     * <p>不拷贝执行数据 — tasks / submissions / evidences / scores / audit / corrective / appeals
     * 全部不复制, 新项目从零开始. inspectors 默认不复制 (检查员是项目内独立配置),
     * 通过 cloneInspectors=true 显式启用.
     *
     * <p>所有动作在同一个事务里完成, 任一步骤异常 → 全部回滚.
     */
    @Transactional
    public InspProject cloneProject(Long sourceId, CloneProjectCommand command, Long userId) {
        InspProject source = projectRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("源项目不存在: " + sourceId));
        if (source.getStatus() == ProjectStatus.ARCHIVED) {
            throw new com.school.management.exception.BusinessException(
                    "已归档的项目不能克隆 (请先恢复或基于其他版本克隆)");
        }

        // 1. 新建 project (DRAFT). orgUnitId / projectName / startDate 来自请求.
        InspProject newProject = InspProject.create(
                generateProjectCode(),
                command.getProjectName(),
                source.getRootSectionId(),
                command.getStartDate(),
                command.getOrgUnitId(),
                userId);
        InspProject savedProject = projectRepository.save(newProject);
        Long newProjectId = savedProject.getId();

        // 2. 复制运行参数 — 通过既有 updateInfo + updatePolicyConfig 走聚合根校验路径.
        //    defaultScoringProfileId 在第 4 步映射后再设, 此处先填 null.
        savedProject.updateInfo(
                /* projectName */ null, // 已通过 create 设置
                /* rootSectionId */ null,
                /* defaultScoringProfileId */ null,
                source.getScopeType(),
                source.getScopeConfig(),
                /* startDate */ null,
                command.getEndDate(),
                source.getAssignmentMode(),
                source.getReviewRequired(),
                source.getAutoPublish(),
                userId);
        savedProject.updatePolicyConfig(
                source.getMaxRejectCount(),
                source.getMaxEscalationLevel(),
                source.getAppealWindowDays(),
                userId);
        savedProject = projectRepository.save(savedProject);

        // 3. 深拷贝 owned ScoringProfile (含 dimensions/bands/rules), 维护 旧 id → 新 id 映射
        Map<Long, Long> profileIdMap = new HashMap<>();
        List<ScoringProfile> sourceProfiles =
                scoringProfileRepository.findByProjectId(sourceId);
        for (ScoringProfile sp : sourceProfiles) {
            ScoringProfile cloned = scoringProfileService.cloneForProject(
                    sp.getId(), newProjectId, userId);
            profileIdMap.put(sp.getId(), cloned.getId());
        }

        // 4. 设置 defaultScoringProfileId — 通过映射查到新 profile id;
        //    源项目无默认 profile 时, 新项目也 null.
        Long mappedDefault = source.getDefaultScoringProfileId() != null
                ? profileIdMap.get(source.getDefaultScoringProfileId())
                : null;
        if (mappedDefault != null) {
            savedProject.updateInfo(null, null, mappedDefault, null, null, null, null, null, null, null, userId);
            savedProject = projectRepository.save(savedProject);
        }

        // 5. 深拷贝 InspectionPlans (排期组). scoringProfileId 经映射重写;
        //    inspectorIds 默认清空 (检查员名单各项目独立配, cloneInspectors=true 时原样复制).
        boolean cloneInspectors = command.isCloneInspectors();
        List<InspectionPlan> sourcePlans = inspectionPlanRepository.findByProjectId(sourceId);
        for (InspectionPlan oldPlan : sourcePlans) {
            Long newPlanScoringProfileId = null;
            if (oldPlan.getScoringProfileId() != null) {
                newPlanScoringProfileId = profileIdMap.get(oldPlan.getScoringProfileId());
                if (newPlanScoringProfileId == null) {
                    log.warn("克隆项目 {}: 调度组 {} 引用的 scoringProfileId={} 不在源项目 owned profile 列表中, 已置 null",
                            sourceId, oldPlan.getPlanName(), oldPlan.getScoringProfileId());
                }
            }
            InspectionPlan newPlan = InspectionPlan.reconstruct(InspectionPlan.builder()
                    .tenantId(oldPlan.getTenantId())
                    .projectId(newProjectId)
                    .planName(oldPlan.getPlanName())
                    .rootSectionId(oldPlan.getRootSectionId())
                    .sectionIds(oldPlan.getSectionIds())
                    .scheduleMode(oldPlan.getScheduleMode())
                    .cycleType(oldPlan.getCycleType())
                    .frequency(oldPlan.getFrequency())
                    .scheduleDays(oldPlan.getScheduleDays())
                    .timeSlots(oldPlan.getTimeSlots())
                    .skipHolidays(oldPlan.getSkipHolidays())
                    .inspectorIds(cloneInspectors ? oldPlan.getInspectorIds() : null)
                    .scoringProfileId(newPlanScoringProfileId)
                    .ratersPerTarget(oldPlan.getRatersPerTarget())
                    .isEnabled(oldPlan.getIsEnabled())
                    .sortOrder(oldPlan.getSortOrder())
                    .createdBy(userId));
            inspectionPlanRepository.save(newPlan);
        }

        // 6. 深拷贝 Indicators (项目-owned 指标树). sectionId 保持不变 — 分区共享模板侧.
        //    第一遍建副本并维护 旧→新 id 映射; 第二遍设 parentIndicatorId.
        List<Indicator> sourceIndicators = indicatorRepository.findByProjectId(sourceId);
        Map<Long, Long> indicatorIdMap = new HashMap<>();
        Map<Long, Long> sourceParentMap = new HashMap<>();
        for (Indicator oldInd : sourceIndicators) {
            sourceParentMap.put(oldInd.getId(), oldInd.getParentIndicatorId());
            Indicator copy = Indicator.reconstruct(Indicator.builder()
                    .tenantId(oldInd.getTenantId())
                    .projectId(newProjectId)
                    // parentIndicatorId 第二遍再设
                    .name(oldInd.getName())
                    .indicatorType(oldInd.getIndicatorType())
                    .sourceSectionId(oldInd.getSourceSectionId())
                    .sourceAggregation(oldInd.getSourceAggregation())
                    .compositeAggregation(oldInd.getCompositeAggregation())
                    .missingPolicy(oldInd.getMissingPolicy())
                    .normalization(oldInd.getNormalization())
                    .normalizationConfig(oldInd.getNormalizationConfig())
                    .evaluationPeriod(oldInd.getEvaluationPeriod())
                    .gradeSchemeId(oldInd.getGradeSchemeId())
                    .evaluationMethod(oldInd.getEvaluationMethod())
                    .gradeThresholds(oldInd.getGradeThresholds())
                    .sortOrder(oldInd.getSortOrder()));
            Indicator saved = indicatorRepository.save(copy);
            indicatorIdMap.put(oldInd.getId(), saved.getId());
        }
        // 第二遍: 重设 parentIndicatorId (按映射)
        for (Indicator oldInd : sourceIndicators) {
            Long oldParent = sourceParentMap.get(oldInd.getId());
            if (oldParent == null) continue;
            Long newParent = indicatorIdMap.get(oldParent);
            Long newId = indicatorIdMap.get(oldInd.getId());
            if (newParent == null || newId == null) continue;
            Indicator newInd = indicatorRepository.findById(newId).orElse(null);
            if (newInd != null) {
                newInd.setParentIndicatorId(newParent);
                indicatorRepository.save(newInd);
            }
        }

        // 7. 可选: 克隆 ProjectInspectors (默认 false — 检查员各项目独立配置)
        if (cloneInspectors) {
            List<ProjectInspector> sourceInspectors = inspectorRepository.findByProjectId(sourceId);
            for (ProjectInspector si : sourceInspectors) {
                ProjectInspector copy = ProjectInspector.create(
                        newProjectId, si.getUserId(), si.getUserName(), si.getRole());
                inspectorRepository.save(copy);
            }
        }

        // 8. 审计日志
        auditLogger.log("InspProject", newProjectId, savedProject.getProjectCode(),
                "PROJECT_CLONED", null,
                java.util.Map.of(
                        "sourceProjectId", sourceId,
                        "sourceProjectCode", source.getProjectCode(),
                        "profilesCloned", sourceProfiles.size(),
                        "plansCloned", sourcePlans.size(),
                        "indicatorsCloned", sourceIndicators.size(),
                        "inspectorsCloned", cloneInspectors ? inspectorRepository.findByProjectId(newProjectId).size() : 0));

        log.info("项目克隆完成: {} → {} (profiles={}, plans={}, indicators={}, cloneInspectors={})",
                source.getProjectCode(), savedProject.getProjectCode(),
                sourceProfiles.size(), sourcePlans.size(), sourceIndicators.size(), cloneInspectors);
        return savedProject;
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

    /**
     * P1#4: 业务编号生成. 旧实现用 4 位随机数后缀, 同日并发碰撞概率不可忽略,
     * 而 insp_projects 有 uk_project_code 唯一索引, 碰撞会 DuplicateKeyException
     * 回滚事务且无法在同一事务内重试. 改为完整雪花 ID 后缀, 全局唯一无碰撞.
     */
    private String generateProjectCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "PRJ-" + dateStr + "-" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
    }
}
