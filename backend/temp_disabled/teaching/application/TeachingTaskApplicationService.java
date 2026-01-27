package com.school.management.application.teaching;

import com.school.management.application.teaching.command.CreateTeachingTaskCommand;
import com.school.management.application.teaching.command.UpdateTeachingTaskCommand;
import com.school.management.application.teaching.query.TaskTeacherDTO;
import com.school.management.application.teaching.query.TeachingTaskDTO;
import com.school.management.domain.teaching.model.aggregate.TeachingTask;
import com.school.management.domain.teaching.model.entity.TaskTeacher;
import com.school.management.domain.teaching.repository.TeachingTaskRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教学任务应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingTaskApplicationService {

    private final TeachingTaskRepository taskRepository;

    /**
     * 创建教学任务
     */
    @Transactional
    public Long createTask(CreateTeachingTaskCommand command) {
        // 检查是否已存在相同的教学任务
        if (taskRepository.existsBySemesterAndCourseAndClass(
                command.getSemesterId(), command.getCourseId(), command.getClassId())) {
            throw new BusinessException("该学期该班级该课程的教学任务已存在");
        }

        TeachingTask task = TeachingTask.builder()
                .semesterId(command.getSemesterId())
                .courseId(command.getCourseId())
                .classId(command.getClassId())
                .classroomId(command.getClassroomId())
                .weeklyHours(command.getWeeklyHours())
                .startWeek(command.getStartWeek())
                .endWeek(command.getEndWeek())
                .examType(command.getExamType())
                .remark(command.getRemark())
                .status(0) // 待分配
                .createdBy(command.getOperatorId())
                .createdAt(LocalDateTime.now())
                .build();

        task = taskRepository.save(task);

        // 保存教师分配
        if (command.getTeachers() != null && !command.getTeachers().isEmpty()) {
            List<TaskTeacher> teachers = new ArrayList<>();
            for (CreateTeachingTaskCommand.TaskTeacherItem item : command.getTeachers()) {
                TaskTeacher teacher = TaskTeacher.builder()
                        .taskId(task.getId())
                        .teacherId(item.getTeacherId())
                        .isMain(item.getIsMain() != null ? item.getIsMain() : false)
                        .teachingContent(item.getTeachingContent())
                        .build();
                teachers.add(teacher);
            }
            taskRepository.saveTaskTeachers(task.getId(), teachers);

            // 更新状态为已分配
            task.setStatus(1);
            taskRepository.save(task);
        }

        log.info("创建教学任务成功: id={}", task.getId());
        return task.getId();
    }

    /**
     * 更新教学任务
     */
    @Transactional
    public void updateTask(UpdateTeachingTaskCommand command) {
        TeachingTask task = taskRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException("教学任务不存在: " + command.getId()));

        if (command.getClassroomId() != null) {
            task.setClassroomId(command.getClassroomId());
        }
        if (command.getWeeklyHours() != null) {
            task.setWeeklyHours(command.getWeeklyHours());
        }
        if (command.getStartWeek() != null) {
            task.setStartWeek(command.getStartWeek());
        }
        if (command.getEndWeek() != null) {
            task.setEndWeek(command.getEndWeek());
        }
        if (command.getExamType() != null) {
            task.setExamType(command.getExamType());
        }
        if (command.getRemark() != null) {
            task.setRemark(command.getRemark());
        }
        if (command.getStatus() != null) {
            task.setStatus(command.getStatus());
        }

        task.setUpdatedBy(command.getOperatorId());
        task.setUpdatedAt(LocalDateTime.now());

        taskRepository.save(task);

        // 更新教师分配
        if (command.getTeachers() != null) {
            List<TaskTeacher> teachers = new ArrayList<>();
            for (UpdateTeachingTaskCommand.TaskTeacherItem item : command.getTeachers()) {
                TaskTeacher teacher = TaskTeacher.builder()
                        .taskId(task.getId())
                        .teacherId(item.getTeacherId())
                        .isMain(item.getIsMain() != null ? item.getIsMain() : false)
                        .teachingContent(item.getTeachingContent())
                        .build();
                teachers.add(teacher);
            }
            taskRepository.saveTaskTeachers(task.getId(), teachers);

            // 如果有教师且状态为待分配,则更新为已分配
            if (!teachers.isEmpty() && task.getStatus() == 0) {
                task.setStatus(1);
                taskRepository.save(task);
            }
        }

        log.info("更新教学任务成功: id={}", task.getId());
    }

    /**
     * 分配教师
     */
    @Transactional
    public void assignTeachers(Long taskId, List<CreateTeachingTaskCommand.TaskTeacherItem> teacherItems, Long operatorId) {
        TeachingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("教学任务不存在: " + taskId));

        List<TaskTeacher> teachers = new ArrayList<>();
        for (CreateTeachingTaskCommand.TaskTeacherItem item : teacherItems) {
            TaskTeacher teacher = TaskTeacher.builder()
                    .taskId(taskId)
                    .teacherId(item.getTeacherId())
                    .isMain(item.getIsMain() != null ? item.getIsMain() : false)
                    .teachingContent(item.getTeachingContent())
                    .build();
            teachers.add(teacher);
        }
        taskRepository.saveTaskTeachers(taskId, teachers);

        // 更新状态为已分配
        if (task.getStatus() == 0) {
            task.setStatus(1);
            task.setUpdatedBy(operatorId);
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);
        }

        log.info("分配教师成功: taskId={}, teacherCount={}", taskId, teachers.size());
    }

    /**
     * 开始教学任务
     */
    @Transactional
    public void startTask(Long id, Long operatorId) {
        TeachingTask task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("教学任务不存在: " + id));

        if (task.getStatus() == 0) {
            throw new BusinessException("请先分配教师");
        }
        if (task.getStatus() == 2) {
            throw new BusinessException("任务已在进行中");
        }
        if (task.getStatus() == 3) {
            throw new BusinessException("任务已完成");
        }

        task.setStatus(2);
        task.setUpdatedBy(operatorId);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        log.info("开始教学任务: id={}", id);
    }

    /**
     * 完成教学任务
     */
    @Transactional
    public void completeTask(Long id, Long operatorId) {
        TeachingTask task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("教学任务不存在: " + id));

        if (task.getStatus() != 2) {
            throw new BusinessException("只有进行中的任务才能标记为完成");
        }

        task.setStatus(3);
        task.setUpdatedBy(operatorId);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        log.info("完成教学任务: id={}", id);
    }

    /**
     * 删除教学任务
     */
    @Transactional
    public void deleteTask(Long id) {
        TeachingTask task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("教学任务不存在: " + id));

        if (task.getStatus() == 2) {
            throw new BusinessException("进行中的任务不能删除");
        }

        taskRepository.deleteById(id);
        log.info("删除教学任务成功: id={}", id);
    }

    /**
     * 获取教学任务详情
     */
    public TeachingTaskDTO getTask(Long id) {
        return taskRepository.findByIdWithTeachers(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("教学任务不存在: " + id));
    }

    /**
     * 根据学期获取教学任务列表
     */
    public List<TeachingTaskDTO> getTasksBySemester(Long semesterId) {
        return taskRepository.findBySemesterId(semesterId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据课程获取教学任务列表
     */
    public List<TeachingTaskDTO> getTasksByCourse(Long courseId) {
        return taskRepository.findByCourseId(courseId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据班级获取教学任务列表
     */
    public List<TeachingTaskDTO> getTasksByClass(Long classId) {
        return taskRepository.findByClassId(classId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据教师获取教学任务列表
     */
    public List<TeachingTaskDTO> getTasksByTeacher(Long teacherId) {
        return taskRepository.findByTeacherId(teacherId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询教学任务
     */
    public List<TeachingTaskDTO> getTasksPage(int page, int size, Long semesterId, Long courseId, Long classId, Integer status) {
        return taskRepository.findPage(page, size, semesterId, courseId, classId, status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计数量
     */
    public long countTasks(Long semesterId, Long courseId, Long classId, Integer status) {
        return taskRepository.count(semesterId, courseId, classId, status);
    }

    /**
     * 批量导入教学任务
     */
    @Transactional
    public int batchImport(Long semesterId, Long planId, Long operatorId) {
        // TODO: 根据培养方案批量生成教学任务
        log.info("批量导入教学任务: semesterId={}, planId={}", semesterId, planId);
        return 0;
    }

    private TeachingTaskDTO toDTO(TeachingTask task) {
        TeachingTaskDTO dto = TeachingTaskDTO.builder()
                .id(task.getId())
                .semesterId(task.getSemesterId())
                .courseId(task.getCourseId())
                .classId(task.getClassId())
                .classroomId(task.getClassroomId())
                .weeklyHours(task.getWeeklyHours())
                .startWeek(task.getStartWeek())
                .endWeek(task.getEndWeek())
                .examType(task.getExamType())
                .remark(task.getRemark())
                .status(task.getStatus())
                .statusName(TeachingTaskDTO.getStatusName(task.getStatus()))
                .createdBy(task.getCreatedBy())
                .createdAt(task.getCreatedAt())
                .updatedBy(task.getUpdatedBy())
                .updatedAt(task.getUpdatedAt())
                .build();

        // 转换教师列表
        if (task.getTeachers() != null && !task.getTeachers().isEmpty()) {
            List<TaskTeacherDTO> teacherDTOs = task.getTeachers().stream()
                    .map(this::toTaskTeacherDTO)
                    .collect(Collectors.toList());
            dto.setTeachers(teacherDTOs);
        }

        return dto;
    }

    private TaskTeacherDTO toTaskTeacherDTO(TaskTeacher teacher) {
        return TaskTeacherDTO.builder()
                .id(teacher.getId())
                .taskId(teacher.getTaskId())
                .teacherId(teacher.getTeacherId())
                .isMain(teacher.getIsMain())
                .teachingContent(teacher.getTeachingContent())
                .build();
    }
}
