package com.school.management.infrastructure.persistence.teaching.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.task.SchedulingStatus;
import com.school.management.domain.teaching.model.task.TaskStatus;
import com.school.management.domain.teaching.model.task.TeachingTask;
import com.school.management.domain.teaching.repository.TeachingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TeachingTaskRepositoryImpl implements TeachingTaskRepository {
    private final TeachingTaskMapper mapper;

    @Override
    public TeachingTask save(TeachingTask task) {
        TeachingTaskPO po = toPO(task);
        if (mapper.selectById(po.getId()) == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<TeachingTask> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<TeachingTask> findByFilter(Long semesterId, Integer taskStatus, int offset, int limit) {
        LambdaQueryWrapper<TeachingTaskPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) {
            wrapper.eq(TeachingTaskPO::getSemesterId, semesterId);
        }
        if (taskStatus != null) {
            wrapper.eq(TeachingTaskPO::getTaskStatus, taskStatus);
        }
        wrapper.orderByDesc(TeachingTaskPO::getCreatedAt);
        wrapper.last("LIMIT " + limit + " OFFSET " + offset);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByFilter(Long semesterId, Integer taskStatus) {
        LambdaQueryWrapper<TeachingTaskPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) {
            wrapper.eq(TeachingTaskPO::getSemesterId, semesterId);
        }
        if (taskStatus != null) {
            wrapper.eq(TeachingTaskPO::getTaskStatus, taskStatus);
        }
        return mapper.selectCount(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TeachingTaskPO toPO(TeachingTask t) {
        TeachingTaskPO po = new TeachingTaskPO();
        po.setId(t.getId());
        po.setTaskCode(t.getTaskCode());
        po.setSemesterId(t.getSemesterId());
        po.setCourseId(t.getCourseId());
        po.setOrgUnitId(t.getOrgUnitId());
        po.setStudentCount(t.getStudentCount());
        po.setWeeklyHours(t.getWeeklyHours());
        po.setTotalHours(t.getTotalHours());
        po.setStartWeek(t.getStartWeek());
        po.setEndWeek(t.getEndWeek());
        po.setRoomTypeRequired(t.getRoomTypeRequired());
        po.setConsecutivePeriods(t.getConsecutivePeriods());
        po.setCourseNature(t.getCourseNature());
        po.setSchedulingStatus(t.getSchedulingStatus() != null ? t.getSchedulingStatus().getCode() : 0);
        po.setTaskStatus(t.getTaskStatus() != null ? t.getTaskStatus().getCode() : 0);
        po.setRemark(t.getRemark());
        po.setCreatedBy(t.getCreatedBy());
        return po;
    }

    private TeachingTask toDomain(TeachingTaskPO po) {
        return TeachingTask.reconstruct(
                po.getId(), po.getTaskCode(), po.getSemesterId(),
                po.getCourseId(), po.getOrgUnitId(),
                po.getStudentCount(), po.getWeeklyHours(), po.getTotalHours(),
                po.getStartWeek(), po.getEndWeek(),
                po.getRoomTypeRequired(), po.getConsecutivePeriods(), po.getCourseNature(),
                po.getSchedulingStatus() != null ? SchedulingStatus.fromCode(po.getSchedulingStatus()) : SchedulingStatus.UNSCHEDULED,
                po.getTaskStatus() != null ? TaskStatus.fromCode(po.getTaskStatus()) : TaskStatus.PENDING,
                po.getRemark(), po.getCreatedBy()
        );
    }
}
