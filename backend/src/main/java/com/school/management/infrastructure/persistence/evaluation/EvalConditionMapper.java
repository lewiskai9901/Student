package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评选条件 Mapper
 */
@Mapper
public interface EvalConditionMapper extends BaseMapper<EvalConditionPO> {

    @Select("SELECT * FROM eval_conditions WHERE level_id = #{levelId} ORDER BY sort_order, id")
    List<EvalConditionPO> findByLevelId(@Param("levelId") Long levelId);

    @Delete("DELETE FROM eval_conditions WHERE level_id = #{levelId}")
    void deleteByLevelId(@Param("levelId") Long levelId);

    @Delete("DELETE c FROM eval_conditions c INNER JOIN eval_levels l ON c.level_id = l.id WHERE l.campaign_id = #{campaignId}")
    void deleteByCampaignId(@Param("campaignId") Long campaignId);
}
