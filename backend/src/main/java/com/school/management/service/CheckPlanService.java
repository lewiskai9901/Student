package com.school.management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.request.CheckPlanCreateRequest;
import com.school.management.dto.request.CheckPlanQueryRequest;
import com.school.management.dto.request.CheckPlanUpdateRequest;
import com.school.management.dto.response.CheckPlanListVO;
import com.school.management.dto.response.CheckPlanStatisticsVO;
import com.school.management.dto.response.CheckPlanVO;
import com.school.management.entity.CheckPlan;

/**
 * 检查计划Service接口
 *
 * @author system
 * @since 3.0.0
 */
public interface CheckPlanService extends IService<CheckPlan> {

    /**
     * 创建检查计划
     */
    CheckPlan createPlan(CheckPlanCreateRequest request);

    /**
     * 更新检查计划
     */
    CheckPlan updatePlan(CheckPlanUpdateRequest request);

    /**
     * 删除检查计划(仅草稿状态可删除)
     */
    void deletePlan(Long id);

    /**
     * 获取计划详情
     */
    CheckPlanVO getPlanDetail(Long id);

    /**
     * 分页查询计划列表
     */
    Page<CheckPlanListVO> getPlanPage(CheckPlanQueryRequest request);

    /**
     * 开始计划
     */
    void startPlan(Long id);

    /**
     * 结束计划
     */
    void finishPlan(Long id);

    /**
     * 归档计划
     */
    void archivePlan(Long id);

    /**
     * 获取计划统计数据
     */
    CheckPlanStatisticsVO getPlanStatistics();

    /**
     * 更新计划统计
     */
    void updatePlanStatistics(Long planId);

    /**
     * 生成模板快照
     */
    String buildTemplateSnapshot(Long templateId);

    /**
     * 获取计划的目标班级列表
     * 根据计划的目标范围配置，返回符合条件的班级列表
     */
    java.util.List<?> getTargetClasses(Long planId);
}
