package com.school.management.application.teaching;

import com.school.management.application.teaching.command.AutoScheduleCommand;
import com.school.management.application.teaching.command.CreateScheduleEntryCommand;
import com.school.management.application.teaching.query.ScheduleConflictDTO;
import com.school.management.application.teaching.query.ScheduleEntryDTO;
import com.school.management.domain.teaching.model.aggregate.CourseSchedule;
import com.school.management.domain.teaching.model.aggregate.TeachingTask;
import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.model.valueobject.ScheduleConflict;
import com.school.management.domain.teaching.repository.CourseScheduleRepository;
import com.school.management.domain.teaching.repository.TeachingTaskRepository;
import com.school.management.domain.teaching.service.ConflictDetectionService;
import com.school.management.domain.teaching.service.SchedulingDomainService;
import com.school.management.exception.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排课应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleApplicationService {

    private final CourseScheduleRepository scheduleRepository;
    private final TeachingTaskRepository taskRepository;
    private final SchedulingDomainService schedulingService;
    private final ConflictDetectionService conflictService;

    /**
     * 创建课表
     */
    @Transactional
    public Long createSchedule(Long semesterId, String scheduleName, Long operatorId) {
        // 检查是否已存在
        if (scheduleRepository.existsBySemesterId(semesterId)) {
            throw new BusinessException("该学期已存在课表");
        }

        CourseSchedule schedule = CourseSchedule.builder()
                .semesterId(semesterId)
                .scheduleName(scheduleName)
                .status(0) // 草稿
                .version(1)
                .createdBy(operatorId)
                .createdAt(LocalDateTime.now())
                .build();

        schedule = scheduleRepository.save(schedule);
        log.info("创建课表成功: id={}, semesterId={}", schedule.getId(), semesterId);
        return schedule.getId();
    }

    /**
     * 添加课表条目
     */
    @Transactional
    public Long addScheduleEntry(CreateScheduleEntryCommand command) {
        // 获取或创建课表
        CourseSchedule schedule = scheduleRepository.findBySemesterId(command.getSemesterId())
                .orElseGet(() -> {
                    CourseSchedule newSchedule = CourseSchedule.builder()
                            .semesterId(command.getSemesterId())
                            .scheduleName("课表-" + command.getSemesterId())
                            .status(0)
                            .version(1)
                            .createdBy(command.getOperatorId())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return scheduleRepository.save(newSchedule);
                });

        if (schedule.getStatus() == 1) {
            throw new BusinessException("已发布的课表不能修改");
        }

        // 检查冲突
        List<ScheduleConflict> conflicts = conflictService.detectConflicts(
                command.getTaskId(),
                command.getWeekday(),
                command.getSlot(),
                command.getStartWeek(),
                command.getEndWeek(),
                command.getWeekType(),
                command.getClassroomId()
        );

        if (!conflicts.isEmpty()) {
            String conflictDesc = conflicts.stream()
                    .map(ScheduleConflict::getDescription)
                    .collect(Collectors.joining("; "));
            throw new BusinessException("排课冲突: " + conflictDesc);
        }

        ScheduleEntry entry = ScheduleEntry.builder()
                .scheduleId(schedule.getId())
                .taskId(command.getTaskId())
                .weekday(command.getWeekday())
                .slot(command.getSlot())
                .startWeek(command.getStartWeek())
                .endWeek(command.getEndWeek())
                .weekType(command.getWeekType() != null ? command.getWeekType() : 0)
                .classroomId(command.getClassroomId())
                .createdAt(LocalDateTime.now())
                .build();

        scheduleRepository.saveEntry(entry);
        log.info("添加课表条目成功: entryId={}, taskId={}", entry.getId(), command.getTaskId());
        return entry.getId();
    }

    /**
     * 删除课表条目
     */
    @Transactional
    public void deleteScheduleEntry(Long entryId) {
        ScheduleEntry entry = scheduleRepository.findEntryById(entryId)
                .orElseThrow(() -> new BusinessException("课表条目不存在"));

        CourseSchedule schedule = scheduleRepository.findById(entry.getScheduleId())
                .orElseThrow(() -> new BusinessException("课表不存在"));

        if (schedule.getStatus() == 1) {
            throw new BusinessException("已发布的课表不能修改");
        }

        scheduleRepository.deleteEntry(entryId);
        log.info("删除课表条目成功: entryId={}", entryId);
    }

    /**
     * 自动排课
     */
    @Transactional
    public AutoScheduleResult autoSchedule(AutoScheduleCommand command) {
        log.info("开始自动排课: semesterId={}", command.getSemesterId());

        // 获取待排课的教学任务
        List<TeachingTask> tasks;
        if (command.getTaskIds() != null && !command.getTaskIds().isEmpty()) {
            tasks = command.getTaskIds().stream()
                    .map(id -> taskRepository.findByIdWithTeachers(id)
                            .orElseThrow(() -> new BusinessException("教学任务不存在: " + id)))
                    .collect(Collectors.toList());
        } else {
            tasks = taskRepository.findBySemesterId(command.getSemesterId()).stream()
                    .filter(t -> t.getStatus() >= 1) // 已分配教师
                    .collect(Collectors.toList());
        }

        if (tasks.isEmpty()) {
            throw new BusinessException("没有可排课的教学任务");
        }

        // 获取或创建课表
        CourseSchedule schedule = scheduleRepository.findBySemesterId(command.getSemesterId())
                .orElseGet(() -> {
                    CourseSchedule newSchedule = CourseSchedule.builder()
                            .semesterId(command.getSemesterId())
                            .scheduleName("课表-" + command.getSemesterId())
                            .status(0)
                            .version(1)
                            .createdBy(command.getOperatorId())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return scheduleRepository.save(newSchedule);
                });

        // 执行遗传算法排课
        int maxIterations = command.getMaxIterations() != null ? command.getMaxIterations() : 1000;
        int populationSize = command.getPopulationSize() != null ? command.getPopulationSize() : 100;
        double mutationRate = command.getMutationRate() != null ? command.getMutationRate() : 0.1;

        List<ScheduleEntry> entries = schedulingService.generateSchedule(
                tasks,
                schedule.getId(),
                maxIterations,
                populationSize,
                mutationRate
        );

        // 保存排课结果
        int successCount = 0;
        int failCount = 0;
        List<String> failedTasks = new ArrayList<>();

        for (ScheduleEntry entry : entries) {
            try {
                scheduleRepository.saveEntry(entry);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failedTasks.add("任务" + entry.getTaskId() + ": " + e.getMessage());
            }
        }

        log.info("自动排课完成: 成功={}, 失败={}", successCount, failCount);
        return AutoScheduleResult.builder()
                .successCount(successCount)
                .failCount(failCount)
                .failedTasks(failedTasks)
                .build();
    }

    /**
     * 检测冲突
     */
    public List<ScheduleConflictDTO> detectConflicts(Long taskId, Integer weekday, Integer slot,
                                                      Integer startWeek, Integer endWeek,
                                                      Integer weekType, Long classroomId) {
        List<ScheduleConflict> conflicts = conflictService.detectConflicts(
                taskId, weekday, slot, startWeek, endWeek, weekType, classroomId
        );

        return conflicts.stream()
                .map(this::toConflictDTO)
                .collect(Collectors.toList());
    }

    /**
     * 发布课表
     */
    @Transactional
    public void publishSchedule(Long scheduleId, Long operatorId) {
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException("课表不存在"));

        if (schedule.getStatus() == 1) {
            throw new BusinessException("课表已发布");
        }

        // 检查是否有排课数据
        List<ScheduleEntry> entries = scheduleRepository.findEntriesByScheduleId(scheduleId);
        if (entries.isEmpty()) {
            throw new BusinessException("课表为空,无法发布");
        }

        schedule.setStatus(1);
        schedule.setPublishedAt(LocalDateTime.now());
        schedule.setUpdatedBy(operatorId);
        schedule.setUpdatedAt(LocalDateTime.now());
        scheduleRepository.save(schedule);

        // 发布领域事件
        // eventPublisher.publish(new SchedulePublishedEvent(scheduleId));

        log.info("发布课表成功: id={}", scheduleId);
    }

    /**
     * 撤销发布
     */
    @Transactional
    public void unpublishSchedule(Long scheduleId, Long operatorId) {
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException("课表不存在"));

        if (schedule.getStatus() != 1) {
            throw new BusinessException("课表未发布");
        }

        schedule.setStatus(0);
        schedule.setUpdatedBy(operatorId);
        schedule.setUpdatedAt(LocalDateTime.now());
        scheduleRepository.save(schedule);

        log.info("撤销发布课表: id={}", scheduleId);
    }

    /**
     * 获取课表详情
     */
    public CourseSchedule getSchedule(Long scheduleId) {
        return scheduleRepository.findByIdWithEntries(scheduleId)
                .orElseThrow(() -> new BusinessException("课表不存在"));
    }

    /**
     * 根据学期获取课表
     */
    public CourseSchedule getScheduleBySemester(Long semesterId) {
        return scheduleRepository.findBySemesterIdWithEntries(semesterId)
                .orElse(null);
    }

    /**
     * 获取班级课表
     */
    public List<ScheduleEntryDTO> getClassSchedule(Long semesterId, Long classId) {
        return scheduleRepository.findEntriesByClassId(semesterId, classId).stream()
                .map(this::toEntryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取教师课表
     */
    public List<ScheduleEntryDTO> getTeacherSchedule(Long semesterId, Long teacherId) {
        return scheduleRepository.findEntriesByTeacherId(semesterId, teacherId).stream()
                .map(this::toEntryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取教室课表
     */
    public List<ScheduleEntryDTO> getClassroomSchedule(Long semesterId, Long classroomId) {
        return scheduleRepository.findEntriesByClassroomId(semesterId, classroomId).stream()
                .map(this::toEntryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有课表条目
     */
    public List<ScheduleEntryDTO> getScheduleEntries(Long scheduleId) {
        return scheduleRepository.findEntriesByScheduleId(scheduleId).stream()
                .map(this::toEntryDTO)
                .collect(Collectors.toList());
    }

    private ScheduleEntryDTO toEntryDTO(ScheduleEntry entry) {
        return ScheduleEntryDTO.builder()
                .id(entry.getId())
                .scheduleId(entry.getScheduleId())
                .taskId(entry.getTaskId())
                .classroomId(entry.getClassroomId())
                .weekday(entry.getWeekday())
                .weekdayName(ScheduleEntryDTO.getWeekdayName(entry.getWeekday()))
                .slot(entry.getSlot())
                .startWeek(entry.getStartWeek())
                .endWeek(entry.getEndWeek())
                .weekType(entry.getWeekType())
                .weekTypeName(ScheduleEntryDTO.getWeekTypeName(entry.getWeekType()))
                .weekRangeDesc(ScheduleEntryDTO.getWeekRangeDesc(entry.getStartWeek(), entry.getEndWeek(), entry.getWeekType()))
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private ScheduleConflictDTO toConflictDTO(ScheduleConflict conflict) {
        return ScheduleConflictDTO.builder()
                .conflictType(conflict.getConflictType())
                .conflictTypeName(ScheduleConflictDTO.getConflictTypeName(conflict.getConflictType()))
                .resourceId(conflict.getResourceId())
                .resourceName(conflict.getResourceName())
                .weekday(conflict.getWeekday())
                .weekdayName(ScheduleEntryDTO.getWeekdayName(conflict.getWeekday()))
                .slot(conflict.getSlot())
                .startWeek(conflict.getStartWeek())
                .endWeek(conflict.getEndWeek())
                .conflictingEntryId(conflict.getConflictingEntryId())
                .description(conflict.getDescription())
                .build();
    }

    @Data
    @Builder
    public static class AutoScheduleResult {
        private int successCount;
        private int failCount;
        private List<String> failedTasks;
    }
}
