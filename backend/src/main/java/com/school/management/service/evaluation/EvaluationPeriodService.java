package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.EvaluationPeriod;

import java.util.List;
import java.util.Map;

/**
 * 综测评定周期服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface EvaluationPeriodService extends IService<EvaluationPeriod> {

    /**
     * 分页查询综测周期
     */
    Page<Map<String, Object>> pagePeriods(Page<?> page, Map<String, Object> query);

    /**
     * 创建综测周期
     */
    Long createPeriod(EvaluationPeriod period);

    /**
     * 更新综测周期
     */
    void updatePeriod(EvaluationPeriod period);

    /**
     * 删除综测周期
     */
    void deletePeriod(Long id);

    /**
     * 获取周期详情
     */
    Map<String, Object> getPeriodDetail(Long id);

    /**
     * 获取当前进行中的周期
     */
    EvaluationPeriod getCurrentPeriod();

    /**
     * 根据学期ID获取周期
     */
    EvaluationPeriod getBySemesterId(Long semesterId);

    /**
     * 开始数据采集
     */
    void startDataCollection(Long id);

    /**
     * 开始荣誉申报
     */
    void startApplication(Long id);

    /**
     * 开始审核
     */
    void startReview(Long id);

    /**
     * 开始公示
     */
    void startPublicity(Long id);

    /**
     * 开始申诉处理
     */
    void startAppeal(Long id);

    /**
     * 结束周期
     */
    void finishPeriod(Long id);

    /**
     * 锁定周期
     */
    void lockPeriod(Long id);

    /**
     * 解锁周期
     */
    void unlockPeriod(Long id);

    /**
     * 根据学年获取周期列表
     */
    List<EvaluationPeriod> getByAcademicYear(String academicYear);
}
