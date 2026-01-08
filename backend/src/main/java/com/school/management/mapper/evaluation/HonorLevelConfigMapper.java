package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.evaluation.HonorLevelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 荣誉等级配置Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface HonorLevelConfigMapper extends BaseMapper<HonorLevelConfig> {

    /**
     * 根据荣誉类型ID查询等级配置
     */
    List<HonorLevelConfig> selectByHonorTypeId(@Param("honorTypeId") Long honorTypeId);

    /**
     * 根据荣誉类型ID查询启用的等级配置
     */
    List<HonorLevelConfig> selectEnabledByHonorTypeId(@Param("honorTypeId") Long honorTypeId);

    /**
     * 根据级别编码和名次编码查询
     */
    HonorLevelConfig selectByLevelAndRank(
            @Param("honorTypeId") Long honorTypeId,
            @Param("levelCode") String levelCode,
            @Param("rankCode") String rankCode);

    /**
     * 删除荣誉类型的所有等级配置
     */
    int deleteByHonorTypeId(@Param("honorTypeId") Long honorTypeId);
}
