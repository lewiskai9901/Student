package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.aggregate.TeachingTask;
import com.school.management.domain.teaching.model.entity.TaskTeacher;
import com.school.management.domain.teaching.repository.TeachingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 教学任务仓储实现
 */
@Repository
@RequiredArgsConstructor
public class TeachingTaskRepositoryImpl implements TeachingTaskRepository {

    private final TeachingTaskMapper taskMapper;
    private final TaskTeacherMapper teacherMapper;

    @Override
    public TeachingTask save(TeachingTask task) {
        TeachingTaskPO po = toPO(task);
        if (po.getId() == null) {
            taskMapper.insert(po);
        } else {
            taskMapper.updateById(po);
        }
        task.setId(po.getId());
        return task;
    }

    @Override
    public Optional<TeachingTask> findById(Long id) {
        TeachingTaskPO po = taskMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<TeachingTask> findByIdWithTeachers(Long id) {
        return findById(id).map(task -> {
            List<TaskTeacher> teachers = findTaskTeachers(id);
            task.setTeachers(teachers);
            return task;
        });
    }

    @Override
    public List<TeachingTask> findBySemesterId(Long semesterId) {
        LambdaQueryWrapper<TeachingTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingTaskPO::getSemesterId, semesterId)
                .orderByAsc(TeachingTaskPO::getCourseId);
        return taskMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TeachingTask> findByCourseId(Long courseId) {
        LambdaQueryWrapper<TeachingTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingTaskPO::getCourseId, courseId)
                .orderByDesc(TeachingTaskPO::getCreatedAt);
        return taskMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TeachingTask> findByClassId(Long classId) {
        LambdaQueryWrapper<TeachingTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingTaskPO::getClassId, classId)
                .orderByDesc(TeachingTaskPO::getCreatedAt);
        return taskMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TeachingTask> findByTeacherId(Long teacherId) {
        return taskMapper.findByTeacherId(teacherId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        teacherMapper.deleteByTaskId(id);
        taskMapper.deleteById(id);
    }

    @Override
    public boolean existsBySemesterAndCourseAndClass(Long semesterId, Long courseId, Long classId) {
        LambdaQueryWrapper<TeachingTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingTaskPO::getSemesterId, semesterId)
                .eq(TeachingTaskPO::getCourseId, courseId)
                .eq(TeachingTaskPO::getClassId, classId);
        return taskMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<TaskTeacher> findTaskTeachers(Long taskId) {
        LambdaQueryWrapper<TaskTeacherPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskTeacherPO::getTaskId, taskId);
        return teacherMapper.selectList(wrapper).stream()
                .map(this::toTaskTeacherDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void saveTaskTeachers(Long taskId, List<TaskTeacher> teachers) {
        // 先删除原有教师
        teacherMapper.deleteByTaskId(taskId);

        // 保存新的教师
        for (TaskTeacher teacher : teachers) {
            TaskTeacherPO po = toTaskTeacherPO(teacher);
            po.setTaskId(taskId);
            po.setCreatedAt(LocalDateTime.now());
            teacherMapper.insert(po);
            teacher.setId(po.getId());
        }
    }

    @Override
    public List<TeachingTask> findPage(int page, int size, Long semesterId, Long courseId, Long classId, Integer status) {
        LambdaQueryWrapper<TeachingTaskPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) {
            wrapper.eq(TeachingTaskPO::getSemesterId, semesterId);
        }
        if (courseId != null) {
            wrapper.eq(TeachingTaskPO::getCourseId, courseId);
        }
        if (classId != null) {
            wrapper.eq(TeachingTaskPO::getClassId, classId);
        }
        if (status != null) {
            wrapper.eq(TeachingTaskPO::getStatus, status);
        }
        wrapper.orderByDesc(TeachingTaskPO::getCreatedAt);

        Page<TeachingTaskPO> pageResult = taskMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(Long semesterId, Long courseId, Long classId, Integer status) {
        LambdaQueryWrapper<TeachingTaskPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) {
            wrapper.eq(TeachingTaskPO::getSemesterId, semesterId);
        }
        if (courseId != null) {
            wrapper.eq(TeachingTaskPO::getCourseId, courseId);
        }
        if (classId != null) {
            wrapper.eq(TeachingTaskPO::getClassId, classId);
        }
        if (status != null) {
            wrapper.eq(TeachingTaskPO::getStatus, status);
        }
        return taskMapper.selectCount(wrapper);
    }

    private TeachingTaskPO toPO(TeachingTask domain) {
        TeachingTaskPO po = new TeachingTaskPO();
        po.setId(domain.getId());
        po.setSemesterId(domain.getSemesterId());
        po.setCourseId(domain.getCourseId());
        po.setClassId(domain.getClassId());
        po.setClassroomId(domain.getClassroomId());
        po.setWeeklyHours(domain.getWeeklyHours());
        po.setStartWeek(domain.getStartWeek());
        po.setEndWeek(domain.getEndWeek());
        po.setExamType(domain.getExamType());
        po.setRemark(domain.getRemark());
        po.setStatus(domain.getStatus());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private TeachingTask toDomain(TeachingTaskPO po) {
        return TeachingTask.builder()
                .id(po.getId())
                .semesterId(po.getSemesterId())
                .courseId(po.getCourseId())
                .classId(po.getClassId())
                .classroomId(po.getClassroomId())
                .weeklyHours(po.getWeeklyHours())
                .startWeek(po.getStartWeek())
                .endWeek(po.getEndWeek())
                .examType(po.getExamType())
                .remark(po.getRemark())
                .status(po.getStatus())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private TaskTeacherPO toTaskTeacherPO(TaskTeacher domain) {
        TaskTeacherPO po = new TaskTeacherPO();
        po.setId(domain.getId());
        po.setTaskId(domain.getTaskId());
        po.setTeacherId(domain.getTeacherId());
        po.setIsMain(domain.getIsMain());
        po.setTeachingContent(domain.getTeachingContent());
        return po;
    }

    private TaskTeacher toTaskTeacherDomain(TaskTeacherPO po) {
        return TaskTeacher.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .teacherId(po.getTeacherId())
                .isMain(po.getIsMain())
                .teachingContent(po.getTeachingContent())
                .build();
    }
}
