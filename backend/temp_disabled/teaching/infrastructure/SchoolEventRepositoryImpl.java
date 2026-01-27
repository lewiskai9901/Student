package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.entity.SchoolEvent;
import com.school.management.domain.teaching.repository.SchoolEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 校历事件仓储实现
 */
@Repository
@RequiredArgsConstructor
public class SchoolEventRepositoryImpl implements SchoolEventRepository {

    private final SchoolEventMapper schoolEventMapper;

    @Override
    public SchoolEvent save(SchoolEvent event) {
        SchoolEventPO po = toPO(event);
        if (po.getId() == null) {
            schoolEventMapper.insert(po);
        } else {
            schoolEventMapper.updateById(po);
        }
        event.setId(po.getId());
        return event;
    }

    @Override
    public Optional<SchoolEvent> findById(Long id) {
        SchoolEventPO po = schoolEventMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<SchoolEvent> findByEventCode(String eventCode) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchoolEventPO::getEventCode, eventCode);
        SchoolEventPO po = schoolEventMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<SchoolEvent> findBySemesterId(Long semesterId) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchoolEventPO::getSemesterId, semesterId)
                .orderByAsc(SchoolEventPO::getStartDate);
        return schoolEventMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchoolEvent> findByDateRange(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(SchoolEventPO::getStartDate, endDate)
                .ge(SchoolEventPO::getEndDate, startDate)
                .orderByAsc(SchoolEventPO::getStartDate);
        return schoolEventMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchoolEvent> findBySemesterIdAndDateRange(Long semesterId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchoolEventPO::getSemesterId, semesterId)
                .le(SchoolEventPO::getStartDate, endDate)
                .ge(SchoolEventPO::getEndDate, startDate)
                .orderByAsc(SchoolEventPO::getStartDate);
        return schoolEventMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchoolEvent> findByEventType(Integer eventType) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchoolEventPO::getEventType, eventType)
                .orderByAsc(SchoolEventPO::getStartDate);
        return schoolEventMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchoolEvent> findScheduleAffectingEvents(Long semesterId) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchoolEventPO::getSemesterId, semesterId)
                .eq(SchoolEventPO::getAffectSchedule, true)
                .eq(SchoolEventPO::getStatus, 1)
                .orderByAsc(SchoolEventPO::getStartDate);
        return schoolEventMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchoolEvent> findByDate(LocalDate date) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(SchoolEventPO::getStartDate, date)
                .ge(SchoolEventPO::getEndDate, date)
                .eq(SchoolEventPO::getStatus, 1);
        return schoolEventMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchoolEvent> findPage(int page, int size, Long semesterId, Integer eventType) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) {
            wrapper.eq(SchoolEventPO::getSemesterId, semesterId);
        }
        if (eventType != null) {
            wrapper.eq(SchoolEventPO::getEventType, eventType);
        }
        wrapper.orderByDesc(SchoolEventPO::getStartDate);

        Page<SchoolEventPO> pageResult = schoolEventMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(Long semesterId, Integer eventType) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) {
            wrapper.eq(SchoolEventPO::getSemesterId, semesterId);
        }
        if (eventType != null) {
            wrapper.eq(SchoolEventPO::getEventType, eventType);
        }
        return schoolEventMapper.selectCount(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        schoolEventMapper.deleteById(id);
    }

    @Override
    public boolean hasScheduleAffectingEvent(LocalDate date) {
        LambdaQueryWrapper<SchoolEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(SchoolEventPO::getStartDate, date)
                .ge(SchoolEventPO::getEndDate, date)
                .eq(SchoolEventPO::getAffectSchedule, true)
                .eq(SchoolEventPO::getStatus, 1);
        return schoolEventMapper.selectCount(wrapper) > 0;
    }

    private SchoolEventPO toPO(SchoolEvent domain) {
        SchoolEventPO po = new SchoolEventPO();
        po.setId(domain.getId());
        po.setSemesterId(domain.getSemesterId());
        po.setEventCode(domain.getEventCode());
        po.setEventName(domain.getEventName());
        po.setEventType(domain.getEventType());
        po.setStartDate(domain.getStartDate());
        po.setEndDate(domain.getEndDate());
        po.setStartTime(domain.getStartTime());
        po.setEndTime(domain.getEndTime());
        po.setAllDay(domain.getAllDay());
        po.setAffectSchedule(domain.getAffectSchedule());
        po.setAffectedOrgUnits(domain.getAffectedOrgUnits());
        po.setSwapToDate(domain.getSwapToDate());
        po.setSwapWeekday(domain.getSwapWeekday());
        po.setColor(domain.getColor());
        po.setPriority(domain.getPriority());
        po.setDescription(domain.getDescription());
        po.setAttachmentUrls(domain.getAttachmentUrls());
        po.setStatus(domain.getStatus());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private SchoolEvent toDomain(SchoolEventPO po) {
        return SchoolEvent.builder()
                .id(po.getId())
                .semesterId(po.getSemesterId())
                .eventCode(po.getEventCode())
                .eventName(po.getEventName())
                .eventType(po.getEventType())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .allDay(po.getAllDay())
                .affectSchedule(po.getAffectSchedule())
                .affectedOrgUnits(po.getAffectedOrgUnits())
                .swapToDate(po.getSwapToDate())
                .swapWeekday(po.getSwapWeekday())
                .color(po.getColor())
                .priority(po.getPriority())
                .description(po.getDescription())
                .attachmentUrls(po.getAttachmentUrls())
                .status(po.getStatus())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
