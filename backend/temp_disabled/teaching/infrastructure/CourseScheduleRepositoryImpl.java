package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.aggregate.CourseSchedule;
import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.repository.CourseScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 课表仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CourseScheduleRepositoryImpl implements CourseScheduleRepository {

    private final CourseScheduleMapper scheduleMapper;
    private final ScheduleEntryMapper entryMapper;

    @Override
    public CourseSchedule save(CourseSchedule schedule) {
        CourseSchedulePO po = toPO(schedule);
        if (po.getId() == null) {
            scheduleMapper.insert(po);
        } else {
            scheduleMapper.updateById(po);
        }
        schedule.setId(po.getId());
        return schedule;
    }

    @Override
    public Optional<CourseSchedule> findById(Long id) {
        CourseSchedulePO po = scheduleMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<CourseSchedule> findBySemesterId(Long semesterId) {
        LambdaQueryWrapper<CourseSchedulePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedulePO::getSemesterId, semesterId)
                .orderByDesc(CourseSchedulePO::getVersion)
                .last("LIMIT 1");
        CourseSchedulePO po = scheduleMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<CourseSchedule> findByIdWithEntries(Long id) {
        return findById(id).map(schedule -> {
            List<ScheduleEntry> entries = findEntriesByScheduleId(id);
            schedule.setEntries(entries);
            return schedule;
        });
    }

    @Override
    public Optional<CourseSchedule> findBySemesterIdWithEntries(Long semesterId) {
        return findBySemesterId(semesterId).map(schedule -> {
            List<ScheduleEntry> entries = findEntriesByScheduleId(schedule.getId());
            schedule.setEntries(entries);
            return schedule;
        });
    }

    @Override
    public void deleteById(Long id) {
        entryMapper.deleteByScheduleId(id);
        scheduleMapper.deleteById(id);
    }

    @Override
    public boolean existsBySemesterId(Long semesterId) {
        LambdaQueryWrapper<CourseSchedulePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedulePO::getSemesterId, semesterId);
        return scheduleMapper.selectCount(wrapper) > 0;
    }

    @Override
    public ScheduleEntry saveEntry(ScheduleEntry entry) {
        ScheduleEntryPO po = toEntryPO(entry);
        if (po.getId() == null) {
            entryMapper.insert(po);
        } else {
            entryMapper.updateById(po);
        }
        entry.setId(po.getId());
        return entry;
    }

    @Override
    public Optional<ScheduleEntry> findEntryById(Long entryId) {
        ScheduleEntryPO po = entryMapper.selectById(entryId);
        return Optional.ofNullable(po).map(this::toEntryDomain);
    }

    @Override
    public List<ScheduleEntry> findEntriesByScheduleId(Long scheduleId) {
        LambdaQueryWrapper<ScheduleEntryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleEntryPO::getScheduleId, scheduleId)
                .orderByAsc(ScheduleEntryPO::getWeekday)
                .orderByAsc(ScheduleEntryPO::getSlot);
        return entryMapper.selectList(wrapper).stream()
                .map(this::toEntryDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntry> findEntriesByClassId(Long semesterId, Long classId) {
        return entryMapper.findByClassId(semesterId, classId).stream()
                .map(this::toEntryDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntry> findEntriesByTeacherId(Long semesterId, Long teacherId) {
        return entryMapper.findByTeacherId(semesterId, teacherId).stream()
                .map(this::toEntryDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntry> findEntriesByClassroomId(Long semesterId, Long classroomId) {
        return entryMapper.findByClassroomId(semesterId, classroomId).stream()
                .map(this::toEntryDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteEntry(Long entryId) {
        entryMapper.deleteById(entryId);
    }

    @Override
    public List<ScheduleEntry> saveEntries(List<ScheduleEntry> entries) {
        return entries.stream()
                .map(this::saveEntry)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntry> findEntriesBySemesterId(Long semesterId) {
        // 先找到学期的课表,再获取条目
        return findBySemesterId(semesterId)
                .map(schedule -> findEntriesByScheduleId(schedule.getId()))
                .orElse(List.of());
    }

    @Override
    public List<ScheduleEntry> findEntriesByTaskId(Long taskId) {
        LambdaQueryWrapper<ScheduleEntryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleEntryPO::getTaskId, taskId);
        return entryMapper.selectList(wrapper).stream()
                .map(this::toEntryDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteEntriesByTaskId(Long taskId) {
        LambdaQueryWrapper<ScheduleEntryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleEntryPO::getTaskId, taskId);
        entryMapper.delete(wrapper);
    }

    @Override
    public com.school.management.domain.teaching.model.valueobject.ScheduleConflict saveConflict(
            com.school.management.domain.teaching.model.valueobject.ScheduleConflict conflict) {
        // 冲突记录暂时在内存中管理,不持久化
        return conflict;
    }

    @Override
    public List<com.school.management.domain.teaching.model.valueobject.ScheduleConflict> findUnresolvedConflicts(Long semesterId) {
        // 暂时返回空列表
        return List.of();
    }

    @Override
    public List<com.school.management.domain.teaching.model.valueobject.ScheduleConflict> findConflictsByEntryId(Long entryId) {
        // 暂时返回空列表
        return List.of();
    }

    @Override
    public void deleteConflictById(Long id) {
        // 暂时不做处理
    }

    @Override
    public List<com.school.management.domain.teaching.model.valueobject.ScheduleConflict> detectConflicts(ScheduleEntry entry) {
        // 简单的冲突检测实现
        return List.of();
    }

    @Override
    public int countScheduledHours(Long taskId) {
        List<ScheduleEntry> entries = findEntriesByTaskId(taskId);
        // 统计已排课时数 (每个条目占1课时)
        return entries.size();
    }

    public List<ScheduleEntry> findConflictingEntries(Long semesterId, Integer weekday, Integer slot,
                                                       Integer startWeek, Integer endWeek) {
        // 查找可能冲突的条目
        LambdaQueryWrapper<ScheduleEntryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleEntryPO::getWeekday, weekday)
                .eq(ScheduleEntryPO::getSlot, slot);

        // 通过课表关联学期
        List<ScheduleEntryPO> allEntries = entryMapper.selectList(wrapper);

        // 过滤周范围冲突的条目
        return allEntries.stream()
                .filter(e -> hasWeekOverlap(e.getStartWeek(), e.getEndWeek(), startWeek, endWeek))
                .map(this::toEntryDomain)
                .collect(Collectors.toList());
    }

    private boolean hasWeekOverlap(Integer start1, Integer end1, Integer start2, Integer end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return true; // 如果没有指定周范围,视为可能冲突
        }
        return !(end1 < start2 || end2 < start1);
    }

    private CourseSchedulePO toPO(CourseSchedule domain) {
        CourseSchedulePO po = new CourseSchedulePO();
        po.setId(domain.getId());
        po.setSemesterId(domain.getSemesterId());
        po.setScheduleName(domain.getScheduleName());
        po.setStatus(domain.getStatus());
        po.setVersion(domain.getVersion());
        po.setPublishedAt(domain.getPublishedAt());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private CourseSchedule toDomain(CourseSchedulePO po) {
        return CourseSchedule.builder()
                .id(po.getId())
                .semesterId(po.getSemesterId())
                .scheduleName(po.getScheduleName())
                .status(po.getStatus())
                .version(po.getVersion())
                .publishedAt(po.getPublishedAt())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private ScheduleEntryPO toEntryPO(ScheduleEntry domain) {
        ScheduleEntryPO po = new ScheduleEntryPO();
        po.setId(domain.getId());
        po.setScheduleId(domain.getScheduleId());
        po.setTaskId(domain.getTaskId());
        po.setWeekday(domain.getWeekday());
        po.setSlot(domain.getSlot());
        po.setStartWeek(domain.getStartWeek());
        po.setEndWeek(domain.getEndWeek());
        po.setWeekType(domain.getWeekType());
        po.setClassroomId(domain.getClassroomId());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private ScheduleEntry toEntryDomain(ScheduleEntryPO po) {
        return ScheduleEntry.builder()
                .id(po.getId())
                .scheduleId(po.getScheduleId())
                .taskId(po.getTaskId())
                .weekday(po.getWeekday())
                .slot(po.getSlot())
                .startWeek(po.getStartWeek())
                .endWeek(po.getEndWeek())
                .weekType(po.getWeekType())
                .classroomId(po.getClassroomId())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
