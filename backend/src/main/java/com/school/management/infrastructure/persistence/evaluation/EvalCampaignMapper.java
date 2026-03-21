package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评选活动 Mapper
 */
@Mapper
public interface EvalCampaignMapper extends BaseMapper<EvalCampaignPO> {

    @Select("SELECT * FROM eval_campaigns WHERE status = #{status} AND deleted = 0 ORDER BY sort_order, id")
    List<EvalCampaignPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM eval_campaigns WHERE deleted = 0 ORDER BY sort_order, id")
    List<EvalCampaignPO> findAll();
}
