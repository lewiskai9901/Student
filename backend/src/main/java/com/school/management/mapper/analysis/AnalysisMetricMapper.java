package com.school.management.mapper.analysis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.analysis.AnalysisMetric;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分析指标Mapper
 */
@Mapper
public interface AnalysisMetricMapper extends BaseMapper<AnalysisMetric> {

    /**
     * 查询配置下的指标列表
     */
    @Select("SELECT * FROM stat_analysis_metrics WHERE config_id = #{configId} ORDER BY display_order, id")
    List<AnalysisMetric> selectByConfigId(@Param("configId") Long configId);

    /**
     * 查询配置下启用的指标列表
     */
    @Select("SELECT * FROM stat_analysis_metrics WHERE config_id = #{configId} AND is_enabled = 1 ORDER BY display_order, id")
    List<AnalysisMetric> selectEnabledByConfigId(@Param("configId") Long configId);

    /**
     * 按编码查询指标
     */
    @Select("SELECT * FROM stat_analysis_metrics WHERE config_id = #{configId} AND metric_code = #{metricCode} LIMIT 1")
    AnalysisMetric selectByConfigIdAndCode(@Param("configId") Long configId, @Param("metricCode") String metricCode);

    /**
     * 统计配置下的指标数量
     */
    @Select("SELECT COUNT(*) FROM stat_analysis_metrics WHERE config_id = #{configId}")
    int countByConfigId(@Param("configId") Long configId);

    /**
     * 删除配置下的所有指标
     */
    @Delete("DELETE FROM stat_analysis_metrics WHERE config_id = #{configId}")
    void deleteByConfigId(@Param("configId") Long configId);
}
