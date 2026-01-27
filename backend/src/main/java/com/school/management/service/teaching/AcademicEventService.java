package com.school.management.service.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.entity.teaching.AcademicEvent;
import com.school.management.mapper.teaching.AcademicEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 校历事件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicEventService {

    private final AcademicEventMapper eventMapper;

    /**
     * 查询事件列表
     */
    public List<AcademicEvent> listEvents(Long yearId, Long semesterId, Integer eventType) {
        LambdaQueryWrapper<AcademicEvent> wrapper = new LambdaQueryWrapper<>();

        if (yearId != null) {
            wrapper.eq(AcademicEvent::getYearId, yearId);
        }
        if (semesterId != null) {
            wrapper.eq(AcademicEvent::getSemesterId, semesterId);
        }
        if (eventType != null) {
            wrapper.eq(AcademicEvent::getEventType, eventType);
        }

        wrapper.orderByAsc(AcademicEvent::getStartDate);
        return eventMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询事件
     */
    public AcademicEvent getById(Long id) {
        return eventMapper.selectById(id);
    }

    /**
     * 创建事件
     */
    @Transactional
    public AcademicEvent createEvent(AcademicEvent event) {
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        event.setDeleted(0);
        if (event.getAllDay() == null) {
            event.setAllDay(true);
        }
        eventMapper.insert(event);
        log.info("Created academic event: id={}, name={}", event.getId(), event.getEventName());
        return event;
    }

    /**
     * 更新事件
     */
    @Transactional
    public AcademicEvent updateEvent(Long id, AcademicEvent event) {
        AcademicEvent existing = eventMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("事件不存在: " + id);
        }

        event.setId(id);
        event.setUpdatedAt(LocalDateTime.now());
        eventMapper.updateById(event);
        log.info("Updated academic event: id={}", id);
        return eventMapper.selectById(id);
    }

    /**
     * 删除事件
     */
    @Transactional
    public void deleteEvent(Long id) {
        eventMapper.deleteById(id);
        log.info("Deleted academic event: id={}", id);
    }
}
