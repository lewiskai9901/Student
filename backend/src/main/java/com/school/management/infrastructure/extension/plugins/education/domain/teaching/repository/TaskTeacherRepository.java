package com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository;

import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.task.TaskTeacher;

import java.util.List;

public interface TaskTeacherRepository {
    void save(TaskTeacher teacher);
    List<TaskTeacher> findByTaskId(Long taskId);
    void deleteByTaskId(Long taskId);
    void deleteByTaskIdAndTeacherId(Long taskId, Long teacherId);
}
