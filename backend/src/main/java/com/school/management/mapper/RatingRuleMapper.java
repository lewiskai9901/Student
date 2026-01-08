package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.RatingRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 评级规则Mapper接口 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Mapper
public interface RatingRuleMapper extends BaseMapper<RatingRule> {

    /**
     * 根据评级模板ID查询规则列表（含等级）
     *
     * @param ratingTemplateId 评级模板ID
     * @return 规则列表
     */
    List<Map<String, Object>> selectRulesByTemplateId(@Param("ratingTemplateId") Long ratingTemplateId);

    /**
     * 获取规则详细信息（含等级列表）
     *
     * @param id 规则ID
     * @return 规则详情
     */
    Map<String, Object> selectRuleDetail(@Param("id") Long id);

    /**
     * 根据规则编码查询
     *
     * @param ratingCode 规则编码
     * @return 评级规则
     */
    RatingRule selectByRatingCode(@Param("ratingCode") String ratingCode);

    /**
     * 批量插入评级规则
     *
     * @param rules 规则列表
     * @return 插入数量
     */
    int batchInsert(@Param("rules") List<RatingRule> rules);

    /**
     * 根据模板ID删除所有规则
     *
     * @param ratingTemplateId 模板ID
     * @return 删除数量
     */
    int deleteByTemplateId(@Param("ratingTemplateId") Long ratingTemplateId);

    /**
     * 根据模板ID统计规则数量
     *
     * @param ratingTemplateId 模板ID
     * @return 规则数量
     */
    int countByTemplateId(@Param("ratingTemplateId") Long ratingTemplateId);
}
