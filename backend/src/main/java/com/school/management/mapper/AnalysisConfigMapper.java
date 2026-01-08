package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.analysis.AnalysisConfigDTO;
import com.school.management.dto.analysis.AnalysisConfigQueryDTO;
import com.school.management.entity.AnalysisConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计分析配置Mapper接口
 *
 * @author Claude
 * @since 2025-12-05
 */
@Mapper
public interface AnalysisConfigMapper extends BaseMapper<AnalysisConfig> {

    /**
     * 分页查询配置（带关联信息）
     */
    IPage<AnalysisConfigDTO> selectConfigPage(Page<AnalysisConfigDTO> page, @Param("query") AnalysisConfigQueryDTO query);

    /**
     * 查询配置详情（带关联信息）
     */
    AnalysisConfigDTO selectConfigById(@Param("id") Long id);

    /**
     * 统计按检查类别的扣分频次
     */
    List<Map<String, Object>> statDeductionByCategory(
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds);

    /**
     * 统计按扣分项的频次
     */
    List<Map<String, Object>> statDeductionByItem(
            @Param("deductionItemId") Long deductionItemId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds);

    /**
     * 统计按检查模板的数据
     */
    List<Map<String, Object>> statByTemplate(
            @Param("templateId") Long templateId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds);

    /**
     * 获取班级排名统计
     */
    List<Map<String, Object>> statClassRanking(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds,
            @Param("limit") Integer limit);

    /**
     * 获取学生排名统计
     */
    List<Map<String, Object>> statStudentRanking(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds,
            @Param("limit") Integer limit);

    /**
     * 获取趋势数据（按日期聚合）
     */
    List<Map<String, Object>> statTrendByDate(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds);

    /**
     * 获取热力图数据
     */
    List<Map<String, Object>> statHeatmapData(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds);

    /**
     * 获取总体统计数据
     */
    Map<String, Object> statOverview(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds);

    /**
     * 统计各检查类别数据（用于雷达图）
     */
    List<Map<String, Object>> statByAllCategories(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orgScopeType") String orgScopeType,
            @Param("orgScopeIds") List<Long> orgScopeIds);

    /**
     * 根据检查计划ID查询统计配置列表
     */
    List<AnalysisConfigDTO> selectConfigsByPlanId(@Param("planId") Long planId);
}
