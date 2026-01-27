package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.aggregate.TeachingTask;

import java.util.List;
import java.util.Optional;

/**
 * 教学任务仓储接口
 */
public interface TeachingTaskRepository {

    /**
     * 保存教学任务
     */
    TeachingTask save(TeachingTask task);

    /**
     * 根据ID查询
     */
    Optional<TeachingTask> findById(Long id);

    /**
     * 根据任务编号查询
     */
    Optional<TeachingTask> findByTaskCode(String taskCode);

    /**
     * 根据ID查询（包含教师）
     */
    Optional<TeachingTask> findByIdWithTeachers(Long id);

    /**
     * 查询学期的所有任务
     */
    List<TeachingTask> findBySemesterId(Long semesterId);

    /**
     * 查询班级的任务
     */
    List<TeachingTask> findByClassId(Long classId);

    /**
     * 查询课程的任务
     */
    List<TeachingTask> findByCourseId(Long courseId);

    /**
     * 查询教师的任务
     */
    List<TeachingTask> findByTeacherId(Long teacherId);

    /**
     * 分页查询
     */
    List<TeachingTask> findPage(int page, int size, Long semesterId, Long classId, Long courseId, Integer status);

    /**
     * 统计总数
     */
    long count(Long semesterId, Long classId, Long courseId, Integer status);

    /**
     * 删除任务
     */
    void deleteById(Long id);

    /**
     * 检查任务编号是否存在
     */
    boolean existsByTaskCode(String taskCode);

    /**
     * 保存任务教师
     */
    void saveTaskTeachers(Long taskId, List<com.school.management.domain.teaching.model.entity.TaskTeacher> teachers);

    /**
     * 删除任务教师
     */
    void deleteTaskTeacher(Long taskId, Long teacherId);

    /**
     * 批量保存
     */
    List<TeachingTask> saveAll(List<TeachingTask> tasks);
}
