package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.RatingLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评级等级Mapper接口 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Mapper
public interface RatingLevelMapper extends BaseMapper<RatingLevel> {

    /**
     * 根据评级规则ID查询等级列表
     *
     * @param ratingRuleId 评级规则ID
     * @return 等级列表
     */
    List<RatingLevel> selectByRatingRuleId(@Param("ratingRuleId") Long ratingRuleId);

    /**
     * 批量插入评级等级
     *
     * @param levels 等级列表
     * @return 插入数量
     */
    int batchInsert(@Param("levels") List<RatingLevel> levels);

    /**
     * 根据规则ID删除所有等级
     *
     * @param ratingRuleId 规则ID
     * @return 删除数量
     */
    int deleteByRuleId(@Param("ratingRuleId") Long ratingRuleId);

    /**
     * 根据规则ID统计等级数量
     *
     * @param ratingRuleId 规则ID
     * @return 等级数量
     */
    int countByRuleId(@Param("ratingRuleId") Long ratingRuleId);
}
