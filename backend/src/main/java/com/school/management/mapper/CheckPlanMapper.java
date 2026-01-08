package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.request.CheckPlanQueryRequest;
import com.school.management.dto.response.CheckPlanListVO;
import com.school.management.dto.response.CheckPlanStatisticsVO;
import com.school.management.entity.CheckPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 检查计划Mapper接口
 *
 * @author system
 * @since 3.0.0
 */
@Mapper
public interface CheckPlanMapper extends BaseMapper<CheckPlan> {

    /**
     * 分页查询检查计划列表
     */
    Page<CheckPlanListVO> selectPlanPage(Page<CheckPlanListVO> page, @Param("query") CheckPlanQueryRequest query);

    /**
     * 根据ID查询计划详情(带关联信息)
     */
    CheckPlan selectPlanWithDetails(@Param("id") Long id);

    /**
     * 查询计划统计数据
     */
    CheckPlanStatisticsVO selectPlanStatistics();

    /**
     * 更新计划统计数据
     */
    void updatePlanStatistics(@Param("planId") Long planId);

    /**
     * 生成计划编号
     */
    String generatePlanCode(@Param("datePrefix") String datePrefix);

    /**
     * 查询今日已创建的计划数量(用于生成序号)
     */
    Integer countTodayPlans(@Param("datePrefix") String datePrefix);
}
