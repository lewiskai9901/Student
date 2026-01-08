package com.school.management.mapper.analysis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.analysis.AnalysisSnapshot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分析快照Mapper
 */
@Mapper
public interface AnalysisSnapshotMapper extends BaseMapper<AnalysisSnapshot> {

    /**
     * 查询配置下的快照列表
     */
    @Select("SELECT * FROM stat_analysis_snapshots WHERE config_id = #{configId} ORDER BY generated_at DESC")
    List<AnalysisSnapshot> selectByConfigId(@Param("configId") Long configId);

    /**
     * 查询最新快照
     */
    @Select("SELECT * FROM stat_analysis_snapshots WHERE config_id = #{configId} ORDER BY generated_at DESC LIMIT 1")
    AnalysisSnapshot selectLatestByConfigId(@Param("configId") Long configId);

    /**
     * 统计配置下的快照数量
     */
    @Select("SELECT COUNT(*) FROM stat_analysis_snapshots WHERE config_id = #{configId}")
    int countByConfigId(@Param("configId") Long configId);

    /**
     * 分页查询快照
     */
    IPage<AnalysisSnapshot> selectPageByConfigId(
            Page<AnalysisSnapshot> page,
            @Param("configId") Long configId
    );

    /**
     * 删除配置下的所有快照
     */
    @Delete("DELETE FROM stat_analysis_snapshots WHERE config_id = #{configId}")
    void deleteByConfigId(@Param("configId") Long configId);
}
