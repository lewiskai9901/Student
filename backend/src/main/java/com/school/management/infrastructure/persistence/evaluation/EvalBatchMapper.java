package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评选执行批次 Mapper
 */
@Mapper
public interface EvalBatchMapper extends BaseMapper<EvalBatchPO> {

    @Select("SELECT * FROM eval_batches WHERE campaign_id = #{campaignId} ORDER BY cycle_start DESC")
    List<EvalBatchPO> findByCampaignId(@Param("campaignId") Long campaignId);
}
