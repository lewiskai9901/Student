package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.evaluation.BehaviorEvaluationEffect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 行为-综测映射Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface BehaviorEvaluationEffectMapper extends BaseMapper<BehaviorEvaluationEffect> {

    /**
     * 根据行为类型ID查询映射规则
     */
    List<BehaviorEvaluationEffect> selectByBehaviorTypeId(@Param("behaviorTypeId") Long behaviorTypeId);

    /**
     * 根据行为类型编码查询映射规则
     */
    List<BehaviorEvaluationEffect> selectByBehaviorTypeCode(@Param("behaviorTypeCode") String behaviorTypeCode);

    /**
     * 查询映射规则详情(含行为类型信息)
     */
    List<Map<String, Object>> selectEffectWithBehaviorType(@Param("behaviorTypeId") Long behaviorTypeId);

    /**
     * 根据维度查询所有映射规则
     */
    List<BehaviorEvaluationEffect> selectByDimension(@Param("dimension") String dimension);
}
