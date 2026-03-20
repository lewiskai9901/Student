package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.repository.v7.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InspTaskApplicationService {

    private final InspTaskRepository taskRepository;
    private final InspProjectRepository projectRepository;
    private final InspSubmissionRepository submissionRepository;
    private final SubmissionDetailRepository detailRepository;
    private final TemplateSectionRepository sectionRepository;
    private final TemplateItemRepository itemRepository;
    private final ProjectScoreRepository scoreRepository;
    private final TargetPopulationService targetPopulationService;
    private final SpringDomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // ========== Task CRUD ==========

    @Transactional
    public InspTask createTask(Long projectId, LocalDate taskDate,
                               String timeSlotCode, java.time.LocalTime timeSlotStart,
                               java.time.LocalTime timeSlotEnd) {
        String taskCode = generateTaskCode();
        InspTask task = InspTask.create(taskCode, projectId, taskDate);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        // 自动填充 submissions
        populateSubmissions(saved);

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<InspTask> getTask(Long id) {
        return taskRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listMyTasks(Long inspectorId) {
        return taskRepository.findByInspectorId(inspectorId);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<InspTask> listAvailableTasks() {
        return taskRepository.findAvailableTasks();
    }

    // ========== Task Lifecycle ==========

    @Transactional
    public InspTask claimTask(Long id, Long inspectorId, String inspectorName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.claim(inspectorId, inspectorName);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask startTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.start();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask submitTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.submit();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        // 计算项目 ProjectScore
        tryComputeProjectScore(saved);

        return saved;
    }

    @Transactional
    public InspTask startReview(Long id, Long reviewerId, String reviewerName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.startReview(reviewerId, reviewerName);
        return taskRepository.save(task);
    }

    @Transactional
    public InspTask reviewTask(Long id, String comment) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.review(comment);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask publishTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.publish();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask cancelTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.cancel();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask assignTask(Long id, Long inspectorId, String inspectorName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.assign(inspectorId, inspectorName);
        return taskRepository.save(task);
    }

    // ========== Internal: Submission Population ==========

    /**
     * Design B: 自动为任务填充 submissions
     * 按分区树递归，每个有目标配置的分区根据 targetSourceMode 派生目标列表。
     * - INDEPENDENT / 根分区: 从项目 scopeConfig 获取根目标
     * - PARENT_ASSOCIATED: 从父分区的目标列表派生关联实体
     */
    private void populateSubmissions(InspTask task) {
        InspProject project = projectRepository.findById(task.getProjectId()).orElse(null);
        if (project == null) {
            log.warn("任务 {} 关联的项目不存在: {}", task.getTaskCode(), task.getProjectId());
            return;
        }

        Long rootSectionId = project.getRootSectionId();
        List<TemplateSection> firstLevelSections = sectionRepository.findByParentSectionId(rootSectionId);

        if (firstLevelSections.isEmpty()) {
            log.info("项目 {} 的根分区 {} 无子分区", project.getProjectCode(), rootSectionId);
            return;
        }

        // 解析根目标（从项目范围获取）
        List<TargetPopulationService.TargetInfo> rootTargets = resolveRootTargets(project);

        int totalTargets = 0;

        for (TemplateSection section : firstLevelSections) {
            totalTargets += populateSectionSubmissions(
                    task, project, section, rootTargets, "ORG", null, null);
        }

        task.updateTargetCounts(totalTargets, 0, 0);
        taskRepository.save(task);
        log.info("任务 {} 填充了 {} 个目标", task.getTaskCode(), totalTargets);
    }

    /**
     * 递归为分区生成 submissions
     * @param parentTargets 父分区的目标列表
     * @param parentTargetType 父目标的实体类型
     * @param rootTarget 根目标（用于 rootTargetId 字段，按部门分组）
     */
    private int populateSectionSubmissions(InspTask task, InspProject project,
                                            TemplateSection section,
                                            List<TargetPopulationService.TargetInfo> parentTargets,
                                            String parentTargetType,
                                            Long rootTargetId, String rootTargetName) {
        TargetType sectionTargetType = section.getTargetType();
        String sourceMode = section.getTargetSourceMode();

        // 无目标配置的分区（如"检查概况"）：创建一条无目标的 submission
        if (sectionTargetType == null) {
            createSubmissionWithDetails(task, section, null, null, null, rootTargetId, rootTargetName);
            return 1;
        }

        // 解析目标列表
        List<TargetPopulationService.TargetInfo> targets;
        if ("PARENT_ASSOCIATED".equals(sourceMode) && parentTargets != null && !parentTargets.isEmpty()) {
            // 从父目标派生
            targets = targetPopulationService.deriveFromParentTargets(
                    parentTargets, parentTargetType,
                    sectionTargetType.name(), section.getTargetTypeFilter());
        } else {
            // 根分区 / INDEPENDENT：从项目范围获取
            targets = targetPopulationService.resolveTargets(
                    project.getScopeType(), project.getScopeConfig(), sectionTargetType);
        }

        if (targets.isEmpty()) {
            log.debug("分区 {} 无目标", section.getSectionName());
            return 0;
        }

        int count = 0;

        // 检查是否有子分区也有目标配置（多层派生）
        List<TemplateSection> childSections = sectionRepository.findByParentSectionId(section.getId());
        boolean hasTargetChildren = childSections.stream()
                .anyMatch(c -> c.getTargetType() != null);

        for (TargetPopulationService.TargetInfo target : targets) {
            // 确定 rootTargetId（第一层设置，后续层继承）
            Long effectiveRootId = rootTargetId != null ? rootTargetId : target.getId();
            String effectiveRootName = rootTargetName != null ? rootTargetName : target.getName();

            // 为当前分区创建 submission（含当前分区和无目标子分区的字段）
            createSubmissionWithDetails(task, section, sectionTargetType,
                    target.getId(), target.getName(), effectiveRootId, effectiveRootName);
            count++;

            // 递归处理有目标配置的子分区
            if (hasTargetChildren) {
                for (TemplateSection child : childSections) {
                    if (child.getTargetType() != null) {
                        count += populateSectionSubmissions(task, project, child,
                                List.of(target), sectionTargetType.name(),
                                effectiveRootId, effectiveRootName);
                    }
                }
            }
        }

        return count;
    }

    /**
     * 创建 submission + details
     */
    private void createSubmissionWithDetails(InspTask task, TemplateSection section,
                                              TargetType targetType, Long targetId, String targetName,
                                              Long rootTargetId, String rootTargetName) {
        InspSubmission submission = InspSubmission.reconstruct(InspSubmission.builder()
                .taskId(task.getId())
                .sectionId(section.getId())
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .rootTargetId(rootTargetId)
                .rootTargetName(rootTargetName)
                .status(SubmissionStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now()));
        submission = submissionRepository.save(submission);

        // 收集当前分区及其无目标子分区的所有 items
        List<TemplateItem> items = collectItemsForSubmission(section);
        for (TemplateItem item : items) {
            ScoringMode scoringMode = parseScoringMode(item.getScoringConfig());
            TemplateSection itemSection = sectionRepository.findById(item.getSectionId()).orElse(section);
            SubmissionDetail detail = SubmissionDetail.create(
                    submission.getId(), item.getId(),
                    item.getItemCode(), item.getItemName(),
                    item.getItemType() != null ? item.getItemType().name() : null,
                    item.getSectionId(), itemSection.getSectionName(), scoringMode,
                    item.getScoringConfig(), item.getValidationRules(), item.getConditionLogic());
            detailRepository.save(detail);
        }
    }

    /**
     * 收集分区及其无目标子分区的所有字段（铺平）
     */
    private List<TemplateItem> collectItemsForSubmission(TemplateSection section) {
        List<TemplateItem> items = new java.util.ArrayList<>(itemRepository.findBySectionId(section.getId()));
        // 递归收集无目标子分区的字段
        List<TemplateSection> children = sectionRepository.findByParentSectionId(section.getId());
        for (TemplateSection child : children) {
            if (child.getTargetType() == null) {
                items.addAll(collectItemsForSubmission(child));
            }
        }
        return items;
    }

    /**
     * 从项目范围解析根目标
     */
    private List<TargetPopulationService.TargetInfo> resolveRootTargets(InspProject project) {
        // 根目标默认为 ORG 类型（项目范围选择的组织）
        return targetPopulationService.resolveTargets(
                project.getScopeType(), project.getScopeConfig(),
                TargetType.ORG);
    }

    /**
     * 从 scoringConfig JSON 解析评分模式
     */
    private ScoringMode parseScoringMode(String scoringConfig) {
        if (scoringConfig == null || scoringConfig.isBlank()) {
            return ScoringMode.DEDUCTION;
        }
        try {
            JsonNode node = objectMapper.readTree(scoringConfig);
            if (node.has("mode")) {
                return ScoringMode.valueOf(node.get("mode").asText());
            }
            if (node.has("scoringMode")) {
                return ScoringMode.valueOf(node.get("scoringMode").asText());
            }
        } catch (Exception e) {
            log.debug("解析 scoringConfig 的 mode 失败, 使用默认值 DEDUCTION: {}", e.getMessage());
        }
        return ScoringMode.DEDUCTION;
    }

    // ========== Internal: Score Computation ==========

    /**
     * 任务提交后，计算项目分数
     */
    private void tryComputeProjectScore(InspTask task) {
        Long projectId = task.getProjectId();
        InspProject project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;

        // 检查该项目的所有任务是否都已提交（SUBMITTED 或更高状态）
        List<InspTask> projectTasks = taskRepository.findByProjectId(projectId);
        boolean allSubmitted = projectTasks.stream()
                .allMatch(t -> t.getStatus() == TaskStatus.SUBMITTED
                        || t.getStatus() == TaskStatus.UNDER_REVIEW
                        || t.getStatus() == TaskStatus.REVIEWED
                        || t.getStatus() == TaskStatus.PUBLISHED);

        if (!allSubmitted) {
            log.debug("项目 {} 尚有未提交的任务，暂不汇总", project.getProjectCode());
            return;
        }

        // 计算项目当日分数
        computeProjectScore(project, task.getTaskDate());
    }

    /**
     * 计算项目在指定日期的分数（所有任务 submissions 的平均分）
     */
    private void computeProjectScore(InspProject project, LocalDate cycleDate) {
        List<InspTask> tasks = taskRepository.findByProjectId(project.getId());
        List<InspTask> dateTasks = tasks.stream()
                .filter(t -> cycleDate.equals(t.getTaskDate()))
                .collect(Collectors.toList());

        if (dateTasks.isEmpty()) return;

        BigDecimal totalScore = BigDecimal.ZERO;
        int count = 0;

        for (InspTask t : dateTasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(t.getId());
            for (InspSubmission sub : submissions) {
                if (sub.getStatus() == SubmissionStatus.COMPLETED && sub.getFinalScore() != null) {
                    totalScore = totalScore.add(sub.getFinalScore());
                    count++;
                }
            }
        }

        if (count == 0) return;

        BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);

        ProjectScore ps = scoreRepository.findByProjectIdAndCycleDate(project.getId(), cycleDate)
                .orElse(ProjectScore.create(project.getId(), cycleDate));
        ps.updateScore(avgScore, null, count, null);
        scoreRepository.save(ps);

        log.info("项目 {} 日期 {} 汇总分数: {}", project.getProjectCode(), cycleDate, avgScore);
    }

    private String generateTaskCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "TSK-" + dateStr + "-" + random;
    }
}
