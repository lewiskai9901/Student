package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评选批次结果 Mapper
 */
@Mapper
public interface EvalResultMapper extends BaseMapper<EvalResultPO> {

    @Select("SELECT * FROM eval_results WHERE batch_id = #{batchId} ORDER BY rank_no")
    List<EvalResultPO> findByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT * FROM eval_results WHERE campaign_id = #{campaignId} " +
            "AND target_type = #{targetType} AND target_id = #{targetId} ORDER BY id DESC")
    List<EvalResultPO> findByCampaignAndTarget(
            @Param("campaignId") Long campaignId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    @Select("SELECT r.* FROM eval_results r " +
            "INNER JOIN eval_batches b ON r.batch_id = b.id " +
            "WHERE r.campaign_id = #{campaignId} AND r.target_id = #{targetId} " +
            "ORDER BY b.cycle_start DESC LIMIT #{limit}")
    List<EvalResultPO> findRecentByTarget(
            @Param("campaignId") Long campaignId,
            @Param("targetId") Long targetId,
            @Param("limit") int limit);

    @Delete("DELETE FROM eval_results WHERE batch_id = #{batchId}")
    void deleteByBatchId(@Param("batchId") Long batchId);
}
