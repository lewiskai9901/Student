package com.school.management.infrastructure.persistence.teaching.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.task.TaskTeacher;
import com.school.management.domain.teaching.repository.TaskTeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TaskTeacherRepositoryImpl implements TaskTeacherRepository {
    private final TaskTeacherMapper mapper;

    @Override
    public void save(TaskTeacher teacher) {
        TaskTeacherPO po = toPO(teacher);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
    }

    @Override
    public List<TaskTeacher> findByTaskId(Long taskId) {
        return mapper.selectList(new LambdaQueryWrapper<TaskTeacherPO>()
                .eq(TaskTeacherPO::getTaskId, taskId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByTaskId(Long taskId) {
        mapper.delete(new LambdaQueryWrapper<TaskTeacherPO>()
                .eq(TaskTeacherPO::getTaskId, taskId));
    }

    @Override
    public void deleteByTaskIdAndTeacherId(Long taskId, Long teacherId) {
        mapper.delete(new LambdaQueryWrapper<TaskTeacherPO>()
                .eq(TaskTeacherPO::getTaskId, taskId)
                .eq(TaskTeacherPO::getTeacherId, teacherId));
    }

    private TaskTeacherPO toPO(TaskTeacher t) {
        TaskTeacherPO po = new TaskTeacherPO();
        po.setId(t.getId());
        po.setTaskId(t.getTaskId());
        po.setTeacherId(t.getTeacherId());
        po.setTeacherRole(t.getTeacherRole());
        po.setWeeklyHours(t.getWeeklyHours());
        po.setWorkloadRatio(t.getWorkloadRatio());
        po.setRemark(t.getRemark());
        return po;
    }

    private TaskTeacher toDomain(TaskTeacherPO po) {
        return TaskTeacher.reconstruct(
                po.getId(), po.getTaskId(), po.getTeacherId(),
                po.getTeacherRole(), po.getWeeklyHours(), po.getWorkloadRatio(), po.getRemark()
        );
    }
}
