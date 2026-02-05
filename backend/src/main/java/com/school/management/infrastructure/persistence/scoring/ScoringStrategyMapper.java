package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 计分策略Mapper
 */
@Mapper
public interface ScoringStrategyMapper extends BaseMapper<ScoringStrategyPO> {

    @Select("SELECT * FROM scoring_strategies WHERE code = #{code} AND deleted = 0")
    ScoringStrategyPO selectByCode(@Param("code") String code);

    @Select("SELECT * FROM scoring_strategies WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<ScoringStrategyPO> selectAllEnabled();

    @Select("SELECT * FROM scoring_strategies WHERE category = #{category} AND deleted = 0 ORDER BY sort_order")
    List<ScoringStrategyPO> selectByCategory(@Param("category") String category);

    @Select("SELECT COUNT(*) FROM scoring_strategies WHERE code = #{code} AND deleted = 0")
    int countByCode(@Param("code") String code);
}
