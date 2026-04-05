package com.school.management.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.scheduling.ScheduleEntry;
import com.school.management.domain.teaching.model.scheduling.WeekType;
import com.school.management.domain.teaching.repository.ScheduleEntryRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ScheduleEntryRepositoryImpl implements ScheduleEntryRepository {
    private final ScheduleEntryPersistenceMapper mapper;

    public ScheduleEntryRepositoryImpl(ScheduleEntryPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ScheduleEntry save(ScheduleEntry entry) {
        ScheduleEntryPO po = toPO(entry);
        if (entry.getId() == null) {
            mapper.insert(po);
            entry.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return entry;
    }

    @Override
    public Optional<ScheduleEntry> findById(Long id) {
        ScheduleEntryPO po = mapper.selectById(id);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<ScheduleEntry> findBySemesterId(Long semesterId) {
        LambdaQueryWrapper<ScheduleEntryPO> w = new LambdaQueryWrapper<>();
        w.eq(ScheduleEntryPO::getSemesterId, semesterId).eq(ScheduleEntryPO::getEntryStatus, 1);
        return mapper.selectList(w).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntry> findByTaskId(Long taskId) {
        LambdaQueryWrapper<ScheduleEntryPO> w = new LambdaQueryWrapper<>();
        w.eq(ScheduleEntryPO::getTaskId, taskId);
        return mapper.selectList(w).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntry> findByTeacherAndWeekday(Long semesterId, Long teacherId, Integer weekday) {
        return mapper.findByTeacherAndWeekday(semesterId, teacherId, weekday)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntry> findByClassroomAndWeekday(Long semesterId, Long classroomId, Integer weekday) {
        return mapper.findByClassroomAndWeekday(semesterId, classroomId, weekday)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntry> findByClassAndWeekday(Long semesterId, Long classId, Integer weekday) {
        return mapper.findByClassAndWeekday(semesterId, classId, weekday)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) { mapper.deleteById(id); }

    @Override
    public void deleteByTaskId(Long taskId) { mapper.deleteByTaskId(taskId); }

    private ScheduleEntryPO toPO(ScheduleEntry d) {
        ScheduleEntryPO po = new ScheduleEntryPO();
        po.setId(d.getId()); po.setSemesterId(d.getSemesterId()); po.setTaskId(d.getTaskId());
        po.setTeachingClassId(d.getTeachingClassId()); po.setCourseId(d.getCourseId());
        po.setClassId(d.getClassId()); po.setTeacherId(d.getTeacherId());
        po.setClassroomId(d.getClassroomId()); po.setWeekday(d.getWeekday());
        po.setStartSlot(d.getStartSlot()); po.setEndSlot(d.getEndSlot());
        po.setStartWeek(d.getStartWeek()); po.setEndWeek(d.getEndWeek());
        po.setWeekType(d.getWeekType() != null ? d.getWeekType().getCode() : 0);
        po.setConsecutiveGroup(d.getConsecutiveGroup()); po.setScheduleType(d.getScheduleType());
        po.setEntryStatus(d.getEntryStatus()); po.setConflictFlag(d.getConflictFlag());
        po.setCreatedBy(d.getCreatedBy()); po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ScheduleEntry toDomain(ScheduleEntryPO po) {
        return ScheduleEntry.reconstruct(po.getId(), po.getSemesterId(), po.getTaskId(),
                po.getTeachingClassId(), po.getCourseId(), po.getClassId(),
                po.getTeacherId(), po.getClassroomId(), po.getWeekday(),
                po.getStartSlot(), po.getEndSlot(), po.getStartWeek(), po.getEndWeek(),
                WeekType.fromCode(po.getWeekType()), po.getConsecutiveGroup(),
                po.getScheduleType(), po.getEntryStatus(), po.getConflictFlag(),
                po.getCreatedBy(), po.getCreatedAt(), po.getUpdatedAt());
    }
}
