package com.school.management.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.entity.task.Task;
import com.school.management.entity.task.TaskAssignee;
import com.school.management.enums.TaskStatus;
import com.school.management.mapper.task.TaskAssigneeMapper;
import com.school.management.mapper.task.TaskMapper;
import com.school.management.service.task.SystemMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务提醒定时任务
 *
 * @author System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskReminderScheduledTask {

    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final SystemMessageService systemMessageService;

    /**
     * 每小时检查即将到期的任务（每小时的第30分钟执行）
     */
    @Scheduled(cron = "0 30 * * * ?")
    public void checkDueSoonTasks() {
        log.info("开始检查即将到期的任务...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dueSoonStart = now;
            LocalDateTime dueSoonEnd = now.plusHours(24);

            // 查询24小时内即将到期且未完成的任务
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Task::getStatus,
                       TaskStatus.PENDING.getCode(),
                       TaskStatus.IN_PROGRESS.getCode(),
                       TaskStatus.SUBMITTED.getCode(),
                       TaskStatus.APPROVING.getCode())
                   .between(Task::getDueDate, dueSoonStart, dueSoonEnd)
                   .isNotNull(Task::getDueDate);

            List<Task> dueSoonTasks = taskMapper.selectList(wrapper);

            log.info("发现{}个即将到期的任务", dueSoonTasks.size());

            for (Task task : dueSoonTasks) {
                // 查询所有未完成的执行人（统一从TaskAssignee表获取）
                LambdaQueryWrapper<TaskAssignee> assigneeWrapper = new LambdaQueryWrapper<>();
                assigneeWrapper.eq(TaskAssignee::getTaskId, task.getId())
                               .in(TaskAssignee::getStatus, 0, 1, 4); // 待接收、进行中、已打回

                List<TaskAssignee> pendingAssignees = taskAssigneeMapper.selectList(assigneeWrapper);

                for (TaskAssignee assignee : pendingAssignees) {
                    long hoursLeft = java.time.Duration.between(now, task.getDueDate()).toHours();
                    String message = String.format("任务「%s」将在%d小时后截止，请尽快处理！", task.getTitle(), hoursLeft);
                    systemMessageService.sendTaskRemindMessage(task, assignee.getAssigneeId(), message);
                    log.debug("发送提醒给执行人: userId={}, taskId={}", assignee.getAssigneeId(), task.getId());
                }
            }

            log.info("任务到期提醒检查完成，已发送{}个提醒", dueSoonTasks.size());

        } catch (Exception e) {
            log.error("检查即将到期任务失败", e);
        }
    }

    /**
     * 每天早上8点检查已超期的任务
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkOverdueTasks() {
        log.info("开始检查超期任务...");

        try {
            LocalDateTime now = LocalDateTime.now();

            // 查询已超期且未完成的任务
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Task::getStatus,
                       TaskStatus.PENDING.getCode(),
                       TaskStatus.IN_PROGRESS.getCode(),
                       TaskStatus.SUBMITTED.getCode(),
                       TaskStatus.APPROVING.getCode())
                   .lt(Task::getDueDate, now)
                   .isNotNull(Task::getDueDate);

            List<Task> overdueTasks = taskMapper.selectList(wrapper);

            log.info("发现{}个超期任务", overdueTasks.size());

            for (Task task : overdueTasks) {
                // 发送超期提醒给创建人
                if (task.getAssignerId() != null) {
                    long daysOverdue = java.time.Duration.between(task.getDueDate(), now).toDays();
                    String message = String.format("任务「%s」已超期%d天，请及时处理！", task.getTitle(), daysOverdue);
                    systemMessageService.sendTaskRemindMessage(task, task.getAssignerId(), message);
                    log.debug("发送超期提醒给创建人: userId={}, taskId={}", task.getAssignerId(), task.getId());
                }

                // 也提醒执行人（统一从TaskAssignee表获取）
                LambdaQueryWrapper<TaskAssignee> assigneeWrapper = new LambdaQueryWrapper<>();
                assigneeWrapper.eq(TaskAssignee::getTaskId, task.getId())
                               .in(TaskAssignee::getStatus, 0, 1, 4);

                List<TaskAssignee> pendingAssignees = taskAssigneeMapper.selectList(assigneeWrapper);

                for (TaskAssignee assignee : pendingAssignees) {
                    long daysOverdue = java.time.Duration.between(task.getDueDate(), now).toDays();
                    String message = String.format("您的任务「%s」已超期%d天，请尽快完成！", task.getTitle(), daysOverdue);
                    systemMessageService.sendTaskRemindMessage(task, assignee.getAssigneeId(), message);
                }
            }

            log.info("超期任务检查完成，已发送{}个提醒", overdueTasks.size() * 2);

        } catch (Exception e) {
            log.error("检查超期任务失败", e);
        }
    }
}
