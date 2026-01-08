package com.school.management.mapper.analysis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.analysis.StatAnalysisConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分析配置Mapper (新版统计分析系统)
 */
@Mapper
public interface StatAnalysisConfigMapper extends BaseMapper<StatAnalysisConfig> {

    /**
     * 查询计划下的配置列表
     */
    @Select("SELECT * FROM stat_analysis_configs WHERE plan_id = #{planId} AND deleted = 0 ORDER BY sort_order, created_at DESC")
    List<StatAnalysisConfig> selectByPlanId(@Param("planId") Long planId);

    /**
     * 查询计划下启用的配置列表
     */
    @Select("SELECT * FROM stat_analysis_configs WHERE plan_id = #{planId} AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order, created_at DESC")
    List<StatAnalysisConfig> selectEnabledByPlanId(@Param("planId") Long planId);

    /**
     * 查询默认配置
     */
    @Select("SELECT * FROM stat_analysis_configs WHERE plan_id = #{planId} AND is_default = 1 AND deleted = 0 LIMIT 1")
    StatAnalysisConfig selectDefaultByPlanId(@Param("planId") Long planId);

    /**
     * 查询公开配置列表
     */
    @Select("SELECT * FROM stat_analysis_configs WHERE is_public = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order, created_at DESC")
    List<StatAnalysisConfig> selectPublicConfigs();

    /**
     * 分页查询
     */
    IPage<StatAnalysisConfig> selectPageByCondition(
            Page<StatAnalysisConfig> page,
            @Param("planId") Long planId,
            @Param("configName") String configName,
            @Param("creatorId") Long creatorId,
            @Param("isEnabled") Boolean isEnabled
    );
}
