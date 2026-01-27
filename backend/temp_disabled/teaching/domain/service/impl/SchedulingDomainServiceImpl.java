package com.school.management.domain.teaching.service.impl;

import com.school.management.domain.teaching.model.aggregate.TeachingTask;
import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.model.valueobject.ScheduleConflict;
import com.school.management.domain.teaching.service.ConflictDetectionService;
import com.school.management.domain.teaching.service.SchedulingDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 排课领域服务实现
 * 使用简化的排课算法，可后续替换为遗传算法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingDomainServiceImpl implements SchedulingDomainService {

    private final ConflictDetectionService conflictDetectionService;

    @Override
    public SchedulingResult autoGenerate(SchedulingRequest request) {
        log.info("开始自动排课: semesterId={}, taskCount={}",
                request.getSemesterId(),
                request.getTaskIds() != null ? request.getTaskIds().size() : 0);

        // TODO: 实现完整的遗传算法排课
        // 目前返回空结果
        return SchedulingResult.failure("自动排课功能开发中");
    }

    @Override
    public List<ScheduleEntry> generateSchedule(List<TeachingTask> tasks, Long scheduleId,
                                                 int maxIterations, int populationSize,
                                                 double mutationRate) {
        log.info("开始遗传算法排课: scheduleId={}, taskCount={}, maxIter={}, popSize={}, mutRate={}",
                scheduleId, tasks != null ? tasks.size() : 0, maxIterations, populationSize, mutationRate);

        List<ScheduleEntry> entries = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return entries;
        }

        // 简化的排课逻辑：为每个任务分配一个时间段
        int[] weekdays = {1, 2, 3, 4, 5}; // 周一到周五
        int[] slots = {1, 3, 5, 7}; // 第1、3、5、7节开始的两节连堂
        int dayIndex = 0;
        int slotIndex = 0;

        for (TeachingTask task : tasks) {
            if (task.getWeeklyHours() == null || task.getWeeklyHours() <= 0) {
                continue;
            }

            // 计算需要多少个时间段（假设每次2课时）
            int sessionsNeeded = (task.getWeeklyHours() + 1) / 2;

            for (int i = 0; i < sessionsNeeded; i++) {
                ScheduleEntry entry = ScheduleEntry.builder()
                        .scheduleId(scheduleId)
                        .semesterId(task.getSemesterId())
                        .taskId(task.getId())
                        .courseId(task.getCourseId())
                        .classId(task.getClassId())
                        .teacherId(task.getMainTeacherId())
                        .weekday(weekdays[dayIndex])
                        .slot(slots[slotIndex])
                        .startSlot(slots[slotIndex])
                        .endSlot(slots[slotIndex] + 1)
                        .startWeek(task.getStartWeek())
                        .endWeek(task.getEndWeek())
                        .weekType(0)
                        .createdAt(LocalDateTime.now())
                        .build();

                entries.add(entry);

                // 移动到下一个时间段
                slotIndex++;
                if (slotIndex >= slots.length) {
                    slotIndex = 0;
                    dayIndex++;
                    if (dayIndex >= weekdays.length) {
                        dayIndex = 0;
                    }
                }
            }
        }

        log.info("排课完成，生成{}个条目", entries.size());
        return entries;
    }

    @Override
    public List<ScheduleConflict> detectConflicts(ScheduleEntry entry) {
        // 目前返回空列表
        return new ArrayList<>();
    }

    @Override
    public ValidationResult validate(ScheduleEntry entry) {
        List<String> errors = new ArrayList<>();

        if (entry == null) {
            errors.add("排课条目不能为空");
            return ValidationResult.failure(errors);
        }

        if (entry.getTaskId() == null) {
            errors.add("教学任务ID不能为空");
        }

        if (entry.getWeekday() == null || entry.getWeekday() < 1 || entry.getWeekday() > 7) {
            errors.add("星期必须在1-7之间");
        }

        Integer slot = entry.getSlot() != null ? entry.getSlot() : entry.getStartSlot();
        if (slot == null || slot < 1 || slot > 12) {
            errors.add("节次必须在1-12之间");
        }

        if (entry.getStartWeek() != null && entry.getEndWeek() != null) {
            if (entry.getStartWeek() > entry.getEndWeek()) {
                errors.add("起始周不能大于结束周");
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }
}
