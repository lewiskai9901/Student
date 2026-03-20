package com.school.management.infrastructure.scheduler;

import com.school.management.application.inspection.v7.InspSubmissionApplicationService;
import com.school.management.domain.inspection.model.v7.execution.InspSubmission;
import com.school.management.domain.inspection.model.v7.execution.SubmissionStatus;
import com.school.management.domain.inspection.repository.v7.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.v7.InspTaskRepository;
import com.school.management.domain.inspection.repository.v7.TemplateSectionRepository;
import com.school.management.domain.inspection.model.v7.execution.InspTask;
import com.school.management.domain.inspection.model.v7.execution.TaskStatus;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * CONTINUOUS 模式提交自动关闭调度器
 *
 * 每分钟扫描 IN_PROGRESS 状态的提交，
 * 如果关联的 TemplateSection 是 CONTINUOUS 模式且当前时间已超过 continuousEnd，
 * 则自动调用 autoClose() 标记 closedAt，并触发计分。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContinuousSubmissionClosureJob {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final InspSubmissionRepository submissionRepository;
    private final InspTaskRepository taskRepository;
    private final TemplateSectionRepository sectionRepository;
    private final InspSubmissionApplicationService submissionApplicationService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredContinuousSubmissions() {
        // 查找所有 IN_PROGRESS 的提交（通过所有活跃任务）
        // 简化实现：遍历所有 IN_PROGRESS 提交，按 sectionId join TemplateSection 判断
        // 注意：此处是简化实现，生产环境应优化为带 join 的单条 SQL
        try {
            List<InspTask> activeTasks = taskRepository.findByStatus(TaskStatus.IN_PROGRESS);
            if (activeTasks == null || activeTasks.isEmpty()) return;

            LocalTime now = LocalTime.now();

            for (InspTask task : activeTasks) {
                List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
                for (InspSubmission submission : submissions) {
                    if (submission.getStatus() != SubmissionStatus.IN_PROGRESS) continue;
                    if (submission.getClosedAt() != null) continue; // 已关闭

                    // 通过 sectionId 查找分区的 CONTINUOUS 模式配置
                    Long sectionId = submission.getSectionId();
                    if (sectionId == null) continue;

                    Optional<TemplateSection> sectionOpt = sectionRepository.findById(sectionId);
                    if (sectionOpt.isEmpty()) continue;

                    TemplateSection section = sectionOpt.get();
                    if (!"CONTINUOUS".equals(section.getInspectionMode())) continue;

                    String continuousEnd = section.getContinuousEnd();
                    if (continuousEnd == null || continuousEnd.isBlank()) continue;

                    try {
                        LocalTime endTime = LocalTime.parse(continuousEnd, TIME_FMT);
                        if (now.isAfter(endTime)) {
                            log.info("CONTINUOUS 模式提交 {} 已超过结束时间 {}，自动关闭并计分",
                                    submission.getId(), continuousEnd);
                            submission.autoClose();
                            submissionRepository.save(submission);
                            // 自动触发计分
                            try {
                                submissionApplicationService.completeSubmission(submission.getId());
                            } catch (Exception scoreEx) {
                                log.warn("自动计分失败，submissionId={}: {}",
                                        submission.getId(), scoreEx.getMessage());
                            }
                        }
                    } catch (Exception parseEx) {
                        log.warn("解析 continuousEnd 时间失败，sectionId={}, value={}: {}",
                                sectionId, continuousEnd, parseEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("ContinuousSubmissionClosureJob 执行失败: {}", e.getMessage(), e);
        }
    }
}
