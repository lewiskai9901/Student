package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.entity.SchoolEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 校历事件仓储接口
 */
public interface SchoolEventRepository {

    /**
     * 保存校历事件
     */
    SchoolEvent save(SchoolEvent event);

    /**
     * 根据ID查询
     */
    Optional<SchoolEvent> findById(Long id);

    /**
     * 根据事件代码查询
     */
    Optional<SchoolEvent> findByEventCode(String eventCode);

    /**
     * 查询学期的所有事件
     */
    List<SchoolEvent> findBySemesterId(Long semesterId);

    /**
     * 按日期范围查询事件
     */
    List<SchoolEvent> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 按日期范围和学期查询事件
     */
    List<SchoolEvent> findBySemesterIdAndDateRange(Long semesterId, LocalDate startDate, LocalDate endDate);

    /**
     * 按事件类型查询
     */
    List<SchoolEvent> findByEventType(Integer eventType);

    /**
     * 查询影响排课的事件
     */
    List<SchoolEvent> findScheduleAffectingEvents(Long semesterId);

    /**
     * 查询指定日期的事件
     */
    List<SchoolEvent> findByDate(LocalDate date);

    /**
     * 分页查询
     */
    List<SchoolEvent> findPage(int page, int size, Long semesterId, Integer eventType);

    /**
     * 统计总数
     */
    long count(Long semesterId, Integer eventType);

    /**
     * 删除事件
     */
    void deleteById(Long id);

    /**
     * 检查日期是否有停课事件
     */
    boolean hasScheduleAffectingEvent(LocalDate date);
}
