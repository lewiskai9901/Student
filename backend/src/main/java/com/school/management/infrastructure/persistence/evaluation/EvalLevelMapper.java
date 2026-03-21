package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评选级别 Mapper
 */
@Mapper
public interface EvalLevelMapper extends BaseMapper<EvalLevelPO> {

    @Select("SELECT * FROM eval_levels WHERE campaign_id = #{campaignId} ORDER BY level_num")
    List<EvalLevelPO> findByCampaignId(@Param("campaignId") Long campaignId);

    @Delete("DELETE FROM eval_levels WHERE campaign_id = #{campaignId}")
    void deleteByCampaignId(@Param("campaignId") Long campaignId);
}
